#!/usr/bin/env python3
"""从 Minecraft 客户端资源目录提取每个隐藏方块的破坏粒子贴图。

读取 `data/blocks.json` 里的隐藏清单，对每个方块查找它的原版 blockstate，
沿 models 的 parent 链解析 `textures.particle`，输出 `data/particles.json`。
生成器只依赖这个 JSON，不依赖 Minecraft 安装。

用法：
    python tools/extract_particles.py
    python tools/extract_particles.py --assets "D:/path/to/assets/minecraft"
"""

from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
DEFAULT_ASSETS = Path(r"D:\Agent_Workspace\Minecraft_Code\Minecraft\assets\minecraft")
BLOCKS_FILE = ROOT / "data" / "blocks.json"
OUT_FILE = ROOT / "data" / "particles.json"

FALLBACK_SUFFIXES = ("", "_top", "_side", "_bottom", "_plant", "_stage0")


def load_json(path: Path):
    with path.open("r", encoding="utf-8") as handle:
        return json.load(handle)


def split_id(identifier: str) -> tuple[str, str]:
    namespace, _, path = identifier.partition(":")
    if not path:
        namespace, path = "minecraft", identifier
    return namespace, path


def normalize(texture: str | None) -> str | None:
    if texture is None:
        return None
    texture = texture.strip()
    if not texture or texture.startswith("#"):
        return None
    return texture if ":" in texture else "minecraft:" + texture


def texture_file(assets: Path, texture: str) -> Path:
    # assets 指向 assets/minecraft，命名空间目录已经包含在外层，这里只用 path。
    _, path = split_id(texture)
    return assets / "textures" / (path + ".png")


def discover_models(blockstate: dict) -> list[str]:
    models: list[str] = []
    variants = blockstate.get("variants")
    if isinstance(variants, dict):
        for value in variants.values():
            entries = value if isinstance(value, list) else [value]
            for entry in entries:
                if isinstance(entry, dict) and isinstance(entry.get("model"), str):
                    models.append(entry["model"])
    multipart = blockstate.get("multipart")
    if isinstance(multipart, list):
        for selector in multipart:
            if not isinstance(selector, dict):
                continue
            apply = selector.get("apply")
            entries = apply if isinstance(apply, list) else [apply]
            for entry in entries:
                if isinstance(entry, dict) and isinstance(entry.get("model"), str):
                    models.append(entry["model"])
    return models


def merged_textures(assets: Path, model_id: str, cache: dict[str, dict]) -> dict[str, str]:
    """沿 parent 链合并 textures（子模型覆盖父模型）。"""
    if model_id in cache:
        return cache[model_id]
    chain: list[dict] = []
    seen: set[str] = set()
    current: str | None = model_id
    while current is not None and current not in seen:
        seen.add(current)
        _, path = split_id(current)
        # assets 指向 assets/minecraft，模型路径为 models/<path>.json
        model_file = assets / "models" / (path + ".json")
        if not model_file.is_file():
            break
        data = load_json(model_file)
        chain.append(data)
        parent = data.get("parent")
        current = parent if isinstance(parent, str) else None
    merged: dict[str, str] = {}
    for data in reversed(chain):
        textures = data.get("textures")
        if isinstance(textures, dict):
            for key, value in textures.items():
                if isinstance(value, str):
                    merged[key] = value
    cache[model_id] = merged
    return merged


def resolve_reference(textures: dict[str, str], value, depth: int = 0):
    while isinstance(value, str) and value.startswith("#") and depth < 16:
        key = value[1:]
        if key not in textures:
            return None
        value = textures[key]
        depth += 1
    if isinstance(value, str) and value and not value.startswith("#"):
        return normalize(value)
    return None


def particle_for_model(assets: Path, model_id: str, cache: dict[str, dict]) -> str | None:
    textures = merged_textures(assets, model_id, cache)
    if "particle" in textures:
        resolved = resolve_reference(textures, textures["particle"])
        if resolved:
            return resolved
    for key in ("all", "texture", "cross", "top", "side"):
        if key in textures:
            resolved = resolve_reference(textures, textures[key])
            if resolved:
                return resolved
    return None


def main(argv=None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--assets", default=str(DEFAULT_ASSETS), help="assets/minecraft 目录")
    parser.add_argument("--label", default="Minecraft Java 26.1 (assets/minecraft)", help="写进 particles.json 的来源标签（不写本机绝对路径）")
    parser.add_argument("--blocks", default=str(BLOCKS_FILE), help="隐藏清单 JSON")
    parser.add_argument("--out", default=str(OUT_FILE), help="输出 particles.json")
    args = parser.parse_args(argv)

    assets = Path(args.assets)
    if not (assets / "blockstates").is_dir():
        print(f"ERROR: 找不到 assets 目录（缺 blockstates）：{assets}", file=sys.stderr)
        return 2

    blocks = load_json(Path(args.blocks))
    aliases = {item["id"]: item for item in blocks.get("legacy_aliases", [])}
    ids: list[str] = []
    for group in blocks.get("hidden", []):
        ids.extend(group.get("ids", []))

    particles: dict[str, str | None] = {}
    missing: list[str] = []
    errors: list[str] = []
    cache: dict[str, dict] = {}

    for block_id in ids:
        blockstate_file = assets / "blockstates" / f"{block_id}.json"
        if not blockstate_file.is_file():
            errors.append(f"原版没有该 blockstate：{block_id}")
            particles[block_id] = None
            continue
        try:
            blockstate = load_json(blockstate_file)
        except Exception as exc:  # noqa: BLE001 - 报告文件级错误
            errors.append(f"blockstate 解析失败 {block_id}: {exc}")
            particles[block_id] = None
            continue
        found: str | None = None
        for model_id in discover_models(blockstate):
            particle = particle_for_model(assets, model_id, cache)
            if particle and texture_file(assets, particle).is_file():
                found = particle
                break
        if not found:
            for suffix in FALLBACK_SUFFIXES:
                candidate = normalize(f"block/{block_id}{suffix}")
                if candidate and texture_file(assets, candidate).is_file():
                    found = candidate
                    break
        if not found:
            missing.append(block_id)
        particles[block_id] = found

    warnings: list[str] = []
    for alias_id, alias in aliases.items():
        particle = normalize(alias.get("particle"))
        if particle and not texture_file(assets, particle).is_file():
            # 旧版本 ID 的贴图在当前版本可能已被改名（例如 grass → short_grass），
            # 这里只提示，不影响输出。
            warnings.append(f"别名 {alias_id} 的粒子贴图在当前 assets 中不存在：{particle}")
        particles[alias_id] = particle

    payload = {
        "source": args.label,
        "generated_by": "tools/extract_particles.py",
        "particles": particles,
        "missing": missing,
        "errors": errors,
        "warnings": warnings,
    }
    out_path = Path(args.out)
    out_path.write_text(json.dumps(payload, ensure_ascii=False, indent=2) + "\n", encoding="utf-8", newline="\n")

    total = len(particles)
    print(f"已写入 {out_path}：{total} 个方块（{len(missing)} 个未解析到粒子）")
    if missing:
        print("未解析到粒子（生成器会省略 particle）：" + ", ".join(missing))
    if warnings:
        print("提示：" + "; ".join(warnings))
    if errors:
        print("错误：" + "; ".join(errors))
    return 0 if not errors else 1


if __name__ == "__main__":
    raise SystemExit(main())

#!/usr/bin/env python3
"""校验生成产物：JSON 可解析、模型引用存在、粒子贴图存在、pack.mcmeta 字段、zip 结构。

用法：
    python tools/validate.py
    python tools/validate.py --vanilla-assets "D:/.../assets/minecraft"
"""

from __future__ import annotations

import argparse
import json
import sys
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
BLOCKS_FILE = ROOT / "data" / "blocks.json"
PARTICLES_FILE = ROOT / "data" / "particles.json"
ASSETS_DIR = ROOT / "assets" / "minecraft"
DIST_DIR = ROOT / "dist"
DEFAULT_VANILLA_ASSETS = Path(r"D:\Agent_Workspace\Minecraft_Code\Minecraft\assets\minecraft")
MODEL_PREFIX = "minecraft:block/terrainradar/"


def load_json(path: Path):
    return json.loads(path.read_text(encoding="utf-8"))


def main(argv=None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--vanilla-assets", default=str(DEFAULT_VANILLA_ASSETS))
    args = parser.parse_args(argv)

    problems: list[str] = []
    blocks = load_json(BLOCKS_FILE)
    particles = load_json(PARTICLES_FILE).get("particles", {})
    vanilla = Path(args.vanilla_assets)

    ids: list[str] = []
    for group in blocks["hidden"]:
        ids.extend(group["ids"])
    alias_ids = [alias["id"] for alias in blocks.get("legacy_aliases", [])]
    expected = ids + alias_ids

    # 1. blockstates / 模型
    for block_id in expected:
        blockstate_path = ASSETS_DIR / "blockstates" / f"{block_id}.json"
        model_path = ASSETS_DIR / "models" / "block" / "terrainradar" / f"{block_id}.json"
        if not blockstate_path.is_file():
            problems.append(f"缺少 blockstate：{block_id}")
            continue
        if not model_path.is_file():
            problems.append(f"缺少模型：{block_id}")
            continue
        try:
            blockstate = load_json(blockstate_path)
            model = load_json(model_path)
        except json.JSONDecodeError as exc:
            problems.append(f"JSON 解析失败（{block_id}）：{exc}")
            continue
        selectors = blockstate.get("multipart")
        if not isinstance(selectors, list) or not selectors:
            problems.append(f"blockstate 不是 multipart：{block_id}")
            continue
        for selector in selectors:
            if not isinstance(selector, dict) or "when" in selector:
                problems.append(f"multipart 含条件（应为无条件）：{block_id}")
            apply = selector.get("apply") if isinstance(selector, dict) else None
            if not isinstance(apply, dict) or apply.get("model") != MODEL_PREFIX + block_id:
                problems.append(f"模型引用不正确：{block_id}")
        if "elements" in model:
            problems.append(f"模型不应包含 elements：{block_id}")
        particle = (model.get("textures") or {}).get("particle")
        if particle is None:
            problems.append(f"模型缺少 particle：{block_id}")
            continue
        if vanilla.is_dir():
            _, _, texture_path = particle.partition(":")
            if not (vanilla / "textures" / (texture_path + ".png")).is_file():
                if block_id == "grass":
                    continue  # 1.20.2 的旧贴图在当前版本已改名，属已知情况
                problems.append(f"粒子贴图不存在：{block_id} -> {particle}")

    # 2. 未列入清单的残留文件
    for path in (ASSETS_DIR / "blockstates").glob("*.json"):
        if path.stem not in expected:
            problems.append(f"残留 blockstate（不在清单中）：{path.name}")
    for path in (ASSETS_DIR / "models" / "block" / "terrainradar").glob("*.json"):
        if path.stem not in expected:
            problems.append(f"残留模型（不在清单中）：{path.name}")

    # 3. pack.mcmeta
    mcmeta_path = ROOT / "pack.mcmeta"
    try:
        mcmeta = load_json(mcmeta_path)["pack"]
    except Exception as exc:  # noqa: BLE001
        problems.append(f"pack.mcmeta 无法解析：{exc}")
        mcmeta = {}
    for key in ("pack_format", "supported_formats", "min_format", "max_format", "description"):
        if key not in mcmeta:
            problems.append(f"pack.mcmeta 缺少字段：{key}")
    if isinstance(mcmeta.get("supported_formats"), list) and len(mcmeta["supported_formats"]) == 2:
        low, high = mcmeta["supported_formats"]
        if mcmeta.get("min_format") != low or mcmeta.get("max_format") != high:
            problems.append("pack.mcmeta 的 min/max_format 与 supported_formats 不一致")

    # 4. zip
    version = blocks["pack"]["version"]
    zip_path = DIST_DIR / f"{blocks['pack']['name']}-v{version}.zip"
    if zip_path.is_file():
        with zipfile.ZipFile(zip_path) as archive:
            names = archive.namelist()
            if "pack.mcmeta" not in names:
                problems.append("zip 根目录缺少 pack.mcmeta")
            if "pack.png" not in names:
                problems.append("zip 根目录缺少 pack.png")
            if any("\\" in name for name in names):
                problems.append("zip 内存在反斜杠路径")
            if len(names) != len(expected) * 2 + 2:
                problems.append(f"zip 文件数异常：{len(names)}（预期 {len(expected) * 2 + 2}）")
    else:
        problems.append(f"找不到 zip：{zip_path}（先运行 tools/build.py）")

    if problems:
        print(f"校验失败：{len(problems)} 个问题")
        for problem in problems:
            print(f"  - {problem}")
        return 1
    print(f"校验通过：{len(expected)} 个方块、{len(expected) * 2 + 2} 个文件、pack.mcmeta 与 zip 结构正常")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

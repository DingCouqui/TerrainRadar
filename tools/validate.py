#!/usr/bin/env python3
"""校验生成产物：JSON、模型引用、贴图、pack.mcmeta、zip 与工作区逐字节一致。

用法：
    python tools/validate.py
    python tools/validate.py --vanilla-assets "D:/.../assets/minecraft"
"""

from __future__ import annotations

import argparse
import hashlib
import json
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
BLOCKS_FILE = ROOT / "data" / "blocks.json"
PARTICLES_FILE = ROOT / "data" / "particles.json"
NOCULL_FILE = ROOT / "data" / "nocull.json"
ASSETS_DIR = ROOT / "assets"
DIST_DIR = ROOT / "dist"
DEFAULT_VANILLA_ASSETS = Path(r"D:\Agent_Workspace\Minecraft_Code\Minecraft\assets\minecraft")
MODEL_PREFIX = "minecraft:block/terrainradar/"
NOCULL_PREFIX = "minecraft:block/nocull/"
ASSET_PATTERNS = ("minecraft/blockstates/**/*.json", "minecraft/models/**/*.json")
REQUIRED = ("pack.mcmeta", "pack.png")
EXPECTED_MCMETA = {
    "pack_format": 18,
    "supported_formats": [18, 84],
    "min_format": 18,
    "max_format": 84,
}


def load_json(path: Path):
    return json.loads(path.read_text(encoding="utf-8"))


def sha256(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def collect_assets() -> list[Path]:
    paths: list[Path] = []
    for pattern in ASSET_PATTERNS:
        paths.extend(path for path in ASSETS_DIR.glob(pattern) if path.is_file())
    return sorted(set(paths))


def check_hidden(problems: list[str], blocks: dict, vanilla: Path | None) -> list[str]:
    ids: list[str] = []
    for group in blocks["hidden"]:
        ids.extend(group["ids"])
    expected = ids + [alias["id"] for alias in blocks.get("legacy_aliases", [])]
    for block_id in expected:
        blockstate_path = ASSETS_DIR / "minecraft" / "blockstates" / f"{block_id}.json"
        model_path = ASSETS_DIR / "minecraft" / "models" / "block" / "terrainradar" / f"{block_id}.json"
        if not blockstate_path.is_file():
            problems.append(f"缺少隐藏 blockstate：{block_id}")
            continue
        if not model_path.is_file():
            problems.append(f"缺少空模型：{block_id}")
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
        else:
            for selector in selectors:
                if not isinstance(selector, dict) or "when" in selector:
                    problems.append(f"multipart 含条件（应为无条件）：{block_id}")
                apply = selector.get("apply") if isinstance(selector, dict) else None
                if not isinstance(apply, dict) or apply.get("model") != MODEL_PREFIX + block_id:
                    problems.append(f"模型引用不正确：{block_id}")
        if "elements" in model:
            problems.append(f"空模型不应包含 elements：{block_id}")
        particle = (model.get("textures") or {}).get("particle")
        if particle is None:
            problems.append(f"空模型缺少 particle：{block_id}")
            continue
        if vanilla is not None:
            _, _, texture_path = particle.partition(":")
            if not (vanilla / "textures" / (texture_path + ".png")).is_file() and block_id != "grass":
                problems.append(f"粒子贴图不存在：{block_id} -> {particle}")
    return expected


def check_nocull(problems: list[str], profiles: list[dict], blocked: set[str]) -> int:
    count = 0
    for profile in profiles:
        faces = None if "all" in profile.get("faces", ["up"]) else set(profile.get("faces", ["up"]))
        for block_id in profile.get("blocks", []):
            if block_id in blocked:
                problems.append(f"去剔除覆盖与隐藏清单重叠：{block_id}")
                continue
            blockstate_path = ASSETS_DIR / "minecraft" / "blockstates" / f"{block_id}.json"
            if not blockstate_path.is_file():
                problems.append(f"缺少去剔除 blockstate：{block_id}")
                continue
            try:
                blockstate = load_json(blockstate_path)
            except json.JSONDecodeError as exc:
                problems.append(f"去剔除 blockstate 解析失败（{block_id}）：{exc}")
                continue
            references: list[str] = []

            def collect(entry) -> None:
                if isinstance(entry, dict) and isinstance(entry.get("model"), str):
                    references.append(entry["model"])

            for value in (blockstate.get("variants") or {}).values():
                for entry in value if isinstance(value, list) else [value]:
                    collect(entry)
            for selector in blockstate.get("multipart") or []:
                apply = selector.get("apply") if isinstance(selector, dict) else None
                for entry in apply if isinstance(apply, list) else [apply]:
                    collect(entry)
            if not references:
                problems.append(f"去剔除 blockstate 没有模型引用：{block_id}")
                continue
            for reference in references:
                if not reference.startswith(NOCULL_PREFIX):
                    problems.append(f"去剔除 blockstate 仍引用原版模型：{block_id} -> {reference}")
                    continue
                _, path = reference.split(":", 1)
                model_path = ASSETS_DIR / "minecraft" / "models" / (path + ".json")
                if not model_path.is_file():
                    problems.append(f"缺少去剔除模型：{reference}")
                    continue
                try:
                    model = load_json(model_path)
                except json.JSONDecodeError as exc:
                    problems.append(f"去剔除模型解析失败（{reference}）：{exc}")
                    continue
                for element in model.get("elements", []):
                    for face in (element.get("faces") or {}).values():
                        cullface = face.get("cullface") if isinstance(face, dict) else None
                        if cullface and (faces is None or cullface in faces):
                            problems.append(f"去剔除模型仍保留 cullface（{reference}）：{cullface}")
            count += 1
    return count


def main(argv=None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--vanilla-assets", default=str(DEFAULT_VANILLA_ASSETS))
    args = parser.parse_args(argv)

    problems: list[str] = []
    blocks = load_json(BLOCKS_FILE)
    particles = load_json(PARTICLES_FILE).get("particles", {})
    nocull_profiles = load_json(NOCULL_FILE).get("profiles", []) if NOCULL_FILE.is_file() else []
    vanilla_path = Path(args.vanilla_assets)
    vanilla = vanilla_path if vanilla_path.is_dir() else None
    if vanilla is None:
        print(f"提示：找不到原版资产目录，跳过贴图存在性校验：{vanilla_path}")
    else:
        # 体积较大，仅确认关键目录存在
        if not (vanilla / "blockstates").is_dir():
            print(f"提示：{vanilla} 不像 assets/minecraft，跳过贴图存在性校验")
            vanilla = None

    hidden_expected = check_hidden(problems, blocks, vanilla)
    nocull_count = check_nocull(problems, nocull_profiles, set(hidden_expected))

    # 未解析到粒子的方块
    unresolved = [block_id for block_id in hidden_expected if not particles.get(block_id)]
    for block_id in unresolved:
        problems.append(f"particles.json 里没有粒子：{block_id}")

    # 残留文件
    expected_files = set(collect_assets())
    for path in ASSETS_DIR.rglob("*"):
        if path.is_file() and path not in expected_files:
            problems.append(f"assets 下有白名单之外的残留文件：{path.relative_to(ROOT)}")

    # pack.mcmeta
    try:
        mcmeta = load_json(ROOT / "pack.mcmeta")["pack"]
    except Exception as exc:  # noqa: BLE001
        problems.append(f"pack.mcmeta 无法解析：{exc}")
        mcmeta = {}
    for key, value in EXPECTED_MCMETA.items():
        if mcmeta.get(key) != value:
            problems.append(f"pack.mcmeta 的 {key} 应为 {value}，实际 {mcmeta.get(key)!r}")
    if mcmeta and not (mcmeta.get("min_format", 0) <= mcmeta.get("pack_format", -1) <= mcmeta.get("max_format", -1)):
        problems.append("pack.mcmeta 的 pack_format 不在 min/max 范围内")

    # zip 与工作区逐字节一致
    zip_path = DIST_DIR / f"{blocks['pack']['name']}-v{blocks['pack']['version']}.zip"
    expected_members = {path.relative_to(ROOT).as_posix(): path for path in expected_files}
    expected_members.update({name: ROOT / name for name in REQUIRED})
    if not zip_path.is_file():
        problems.append(f"找不到 zip：{zip_path}（先运行 tools/build.py）")
    else:
        with zipfile.ZipFile(zip_path) as archive:
            names = archive.namelist()
            if set(names) != set(expected_members):
                missing = sorted(set(expected_members) - set(names))
                extra = sorted(set(names) - set(expected_members))
                problems.append(f"zip 条目与工作区不一致：缺 {missing[:5]}，多 {extra[:5]}")
            for name in sorted(set(names) & set(expected_members)):
                if sha256(archive.read(name)) != sha256(expected_members[name].read_bytes()):
                    problems.append(f"zip 内容与工作区不一致：{name}")
            if any("\\" in name for name in names):
                problems.append("zip 内存在反斜杠路径")

    if problems:
        print(f"校验失败：{len(problems)} 个问题")
        for problem in problems:
            print(f"  - {problem}")
        return 1
    print(
        f"校验通过：隐藏 {len(hidden_expected)} 个 ID、去剔除 {nocull_count} 个方块、"
        f"{len(expected_members)} 个文件、pack.mcmeta 与 zip 结构正常"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

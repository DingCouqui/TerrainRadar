#!/usr/bin/env python3
"""由 data/blocks.json + data/particles.json 生成资源包内容。

生成物：
- assets/minecraft/blockstates/<id>.json   —— 无条件 multipart，覆盖该方块的全部状态
- assets/minecraft/models/block/terrainradar/<id>.json —— 只有 particle 的空模型
- pack.mcmeta
- docs/BLOCK-LIST.md（清单文档）

用法：
    python tools/generate.py
    python tools/generate.py --check-assets   # 额外核对 26.1 原版 blockstate 是否存在
"""

from __future__ import annotations

import argparse
import json
import shutil
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
BLOCKS_FILE = ROOT / "data" / "blocks.json"
PARTICLES_FILE = ROOT / "data" / "particles.json"
ASSETS_DIR = ROOT / "assets" / "minecraft"
BLOCKSTATES_DIR = ASSETS_DIR / "blockstates"
MODELS_DIR = ASSETS_DIR / "models" / "block" / "terrainradar"
PACK_MCMETA = ROOT / "pack.mcmeta"
BLOCK_LIST_DOC = ROOT / "docs" / "BLOCK-LIST.md"

DEFAULT_VANILLA_ASSETS = Path(r"D:\Agent_Workspace\Minecraft_Code\Minecraft\assets\minecraft")
MODEL_PREFIX = "minecraft:block/terrainradar/"


def load_json(path: Path):
    with path.open("r", encoding="utf-8") as handle:
        return json.load(handle)


def write_json(path: Path, payload) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    text = json.dumps(payload, ensure_ascii=False, indent=2) + "\n"
    path.write_text(text, encoding="utf-8")


def hidden_ids(blocks: dict) -> list[str]:
    ids: list[str] = []
    for group in blocks["hidden"]:
        ids.extend(group["ids"])
    return ids


def build_blockstate(model_id: str) -> dict:
    """无 when 的 multipart 会作用到方块的全部状态（Selector 的 when 可选）。"""
    return {"multipart": [{"apply": {"model": model_id}}]}


def build_model(particle: str | None) -> dict:
    if particle:
        return {"textures": {"particle": particle}}
    return {}


def write_pack_meta(pack: dict) -> None:
    payload = {
        "pack": {
            "pack_format": pack["pack_format"],
            "supported_formats": pack["supported_formats"],
            "min_format": pack["min_format"],
            "max_format": pack["max_format"],
            "description": pack["description"],
        }
    }
    write_json(PACK_MCMETA, payload)


def wrap_ids(ids: list[str], indent: str = "  ", per_line: int = 6) -> list[str]:
    lines = []
    for start in range(0, len(ids), per_line):
        chunk = ids[start : start + per_line]
        lines.append(indent + ", ".join(f"`{item}`" for item in chunk))
    return lines


def write_block_list(blocks: dict, particles: dict) -> None:
    pack = blocks["pack"]
    ids = hidden_ids(blocks)
    lines: list[str] = []
    lines.append("# TerrainRadar 方块清单")
    lines.append("")
    lines.append(f"> 由 `tools/generate.py` 从 `data/blocks.json` 生成，请勿手改。版本：v{pack['version']}。")
    lines.append("")
    lines.append(f"- 隐藏方块：**{len(ids)}** 个（另有 {len(blocks.get('legacy_aliases', []))} 个旧版本 ID 别名）")
    lines.append(f"- 支持版本：Java 1.20.2–26.1（单 zip，资源包格式 18–84）")
    lines.append("")
    lines.append("## 隐藏清单")
    lines.append("")
    for group in blocks["hidden"]:
        group_ids = group["ids"]
        lines.append(f"### {group['group']}（{len(group_ids)}）")
        lines.append("")
        lines.extend(wrap_ids(group_ids))
        lines.append("")
    aliases = blocks.get("legacy_aliases", [])
    if aliases:
        lines.append("## 旧版本 ID 别名")
        lines.append("")
        for alias in aliases:
            lines.append(f"- `{alias['id']}`：{alias['note']}")
        lines.append("")
    lines.append("## 不处理（保留可见）")
    lines.append("")
    for group in blocks.get("excluded", []):
        lines.append(f"### {group['group']}")
        lines.append("")
        lines.extend(wrap_ids(group["ids"], per_line=4))
        lines.append("")
    notes = blocks.get("notes", {})
    solid = notes.get("solid_hidden_ids", [])
    if solid:
        lines.append("## 已知代价：洞（Culling Hole）")
        lines.append("")
        lines.append(notes.get("culling_holes", "").strip())
        lines.append("")
        lines.append(f"会产生洞的隐藏方块（{len(solid)}）：")
        lines.append("")
        lines.extend(wrap_ids(solid, per_line=5))
        lines.append("")
    unresolved = [block_id for block_id in ids if not particles.get(block_id)]
    if unresolved:
        lines.append("## 未解析到破坏粒子贴图")
        lines.append("")
        lines.append(", ".join(f"`{item}`" for item in unresolved))
        lines.append("")
    lines.append("## 版本备注")
    lines.append("")
    lines.append("- 1.20.3 起 `grass` 改名为 `short_grass`；本包同时提供 `grass.json` 供 1.20.2 使用（1.20.3+ 自动忽略）。")
    lines.append("- 1.21.4 新增 `pale_oak_leaves`、`closed/open_eyeblossom`、`pale_moss_*`；1.21.5 新增 `leaf_litter`、`wildflowers`、`bush`、`firefly_bush`、`short/tall_dry_grass`、`cactus_flower`；26.1 新增 `golden_dandelion`。")
    lines.append("- 旧版本会忽略不存在的方块 ID 对应的 blockstate（只记 debug 日志，不报错）。")
    lines.append("")
    BLOCK_LIST_DOC.parent.mkdir(parents=True, exist_ok=True)
    BLOCK_LIST_DOC.write_text("\n".join(lines) + "\n", encoding="utf-8")


def check_against_vanilla(blocks: dict, vanilla_assets: Path) -> list[str]:
    problems: list[str] = []
    if not (vanilla_assets / "blockstates").is_dir():
        return [f"找不到原版 assets 目录：{vanilla_assets}"]
    for block_id in hidden_ids(blocks):
        if not (vanilla_assets / "blockstates" / f"{block_id}.json").is_file():
            problems.append(f"原版 26.1 没有 blockstate：{block_id}")
    return problems


def main(argv=None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check-assets", action="store_true", help="核对 26.1 原版 blockstate 是否存在")
    parser.add_argument("--vanilla-assets", default=str(DEFAULT_VANILLA_ASSETS), help="26.1 assets/minecraft 目录")
    args = parser.parse_args(argv)

    blocks = load_json(BLOCKS_FILE)
    particles_payload = load_json(PARTICLES_FILE)
    particles: dict[str, str | None] = particles_payload.get("particles", {})

    ids = hidden_ids(blocks)
    duplicates = sorted({item for item in ids if ids.count(item) > 1})
    if duplicates:
        print(f"ERROR: 隐藏清单有重复 ID：{', '.join(duplicates)}", file=sys.stderr)
        return 1
    alias_ids = {alias["id"] for alias in blocks.get("legacy_aliases", [])}
    overlap = alias_ids.intersection(ids)
    if overlap:
        print(f"ERROR: 别名与隐藏清单重叠：{', '.join(sorted(overlap))}", file=sys.stderr)
        return 1

    if args.check_assets:
        problems = check_against_vanilla(blocks, Path(args.vanilla_assets))
        if problems:
            print("ERROR: 清单与原版资产不一致：", file=sys.stderr)
            for problem in problems:
                print(f"  - {problem}", file=sys.stderr)
            return 1

    # 清理旧的生成结果，避免缩清单后残留
    for path in (BLOCKSTATES_DIR, MODELS_DIR):
        if path.exists():
            shutil.rmtree(path)
    BLOCKSTATES_DIR.mkdir(parents=True, exist_ok=True)
    MODELS_DIR.mkdir(parents=True, exist_ok=True)

    written = 0
    unresolved: list[str] = []
    for block_id in ids + sorted(alias_ids):
        particle = particles.get(block_id)
        if not particle:
            unresolved.append(block_id)
        write_json(BLOCKSTATES_DIR / f"{block_id}.json", build_blockstate(MODEL_PREFIX + block_id))
        write_json(MODELS_DIR / f"{block_id}.json", build_model(particle))
        written += 1

    write_pack_meta(blocks["pack"])
    write_block_list(blocks, particles)

    print(f"生成完成：{written} 个方块（blockstates + 空模型），pack.mcmeta、docs/BLOCK-LIST.md 已更新")
    if unresolved:
        print(f"警告：{len(unresolved)} 个方块没有粒子贴图（模型里会省略 particle）：")
        print("  " + ", ".join(unresolved))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

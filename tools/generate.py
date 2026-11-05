#!/usr/bin/env python3
"""由 data/blocks.json + data/particles.json + data/nocull.json 生成资源包内容。

生成物：
- assets/minecraft/blockstates/<id>.json            —— 无条件 multipart，覆盖该方块的全部状态（隐藏）
- assets/minecraft/models/block/terrainradar/<id>.json —— 只有 particle 的空模型（隐藏）
- assets/minecraft/blockstates/<id>.json            —— 去剔除覆盖（nocull profile）
- assets/minecraft/models/block/nocull/**.json      —— 复制原版模型并去掉指定面的 cullface
- pack.mcmeta
- docs/BLOCK-LIST.md

用法：
    python tools/generate.py
    python tools/generate.py --check-assets
"""

from __future__ import annotations

import argparse
import copy
import json
import shutil
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
BLOCKS_FILE = ROOT / "data" / "blocks.json"
PARTICLES_FILE = ROOT / "data" / "particles.json"
NOCULL_FILE = ROOT / "data" / "nocull.json"
ASSETS_DIR = ROOT / "assets" / "minecraft"
BLOCKSTATES_DIR = ASSETS_DIR / "blockstates"
MODELS_DIR = ASSETS_DIR / "models" / "block" / "terrainradar"
NOCULL_MODELS_DIR = ASSETS_DIR / "models" / "block" / "nocull"
PACK_MCMETA = ROOT / "pack.mcmeta"
BLOCK_LIST_DOC = ROOT / "docs" / "BLOCK-LIST.md"

DEFAULT_VANILLA_ASSETS = Path(r"D:\Agent_Workspace\Minecraft_Code\Minecraft\assets\minecraft")
MODEL_PREFIX = "minecraft:block/terrainradar/"
NOCULL_PREFIX = "minecraft:block/nocull/"


def load_json(path: Path):
    with path.open("r", encoding="utf-8") as handle:
        return json.load(handle)


def write_json(path: Path, payload) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    text = json.dumps(payload, ensure_ascii=False, indent=2) + "\n"
    path.write_text(text, encoding="utf-8", newline="\n")


def split_id(identifier: str) -> tuple[str, str]:
    namespace, _, path = identifier.partition(":")
    if not path:
        namespace, path = "minecraft", identifier
    return namespace, path


def hidden_ids(blocks: dict) -> list[str]:
    ids: list[str] = []
    for group in blocks["hidden"]:
        ids.extend(group["ids"])
    return ids


def build_blockstate(model_id: str) -> dict:
    """无 when 的 multipart 会作用到方块的全部状态（Selector 的 when 可选）。"""
    return {"multipart": [{"apply": {"model": model_id}}]}


def build_model(particle: str | None) -> dict:
    return {"textures": {"particle": particle}} if particle else {}


def write_pack_meta(pack: dict) -> None:
    write_json(
        PACK_MCMETA,
        {
            "pack": {
                "pack_format": pack["pack_format"],
                "supported_formats": pack["supported_formats"],
                "min_format": pack["min_format"],
                "max_format": pack["max_format"],
                "description": pack["description"],
            }
        },
    )


def wrap_ids(ids: list[str], indent: str = "  ", per_line: int = 6) -> list[str]:
    lines = []
    for start in range(0, len(ids), per_line):
        chunk = ids[start : start + per_line]
        lines.append(indent + ", ".join(f"`{item}`" for item in chunk))
    return lines


def write_block_list(blocks: dict, particles: dict, nocull_profiles: list[dict]) -> None:
    pack = blocks["pack"]
    ids = hidden_ids(blocks)
    lines: list[str] = []
    lines.append("# TerrainRadar 方块清单")
    lines.append("")
    lines.append(f"> 由 `tools/generate.py` 从 `data/blocks.json` 生成，请勿手改。版本：v{pack['version']}。")
    lines.append("")
    lines.append(f"- 隐藏方块：**{len(ids)}** 个（另有 {len(blocks.get('legacy_aliases', []))} 个旧版本 ID 别名）")
    lines.append("- 支持版本：Java 1.20.2–26.1（单 zip，资源包格式 18–84）")
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
    if nocull_profiles:
        lines.append("## 去剔除覆盖（保持可见的方块）")
        lines.append("")
        lines.append("隐藏实心方块会剔除相邻方块朝向它的面，留下可透视的洞。下面的方块复制了原版模型，")
        lines.append("并去掉指定面的 `cullface`，让这些面永远渲染（原理同参考包让矿石保持可见的做法）。")
        lines.append("")
        for profile in nocull_profiles:
            faces = "、".join(profile.get("faces", ["up"]))
            lines.append(f"### {profile['name']}（去掉 {faces} 面的 cullface）")
            lines.append("")
            lines.append(profile.get("description", ""))
            lines.append("")
            lines.extend(wrap_ids(profile["blocks"], per_line=4))
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
    BLOCK_LIST_DOC.write_text("\n".join(lines) + "\n", encoding="utf-8", newline="\n")


def check_against_vanilla(blocks: dict, vanilla_assets: Path) -> list[str]:
    problems: list[str] = []
    if not (vanilla_assets / "blockstates").is_dir():
        return [f"找不到原版 assets 目录：{vanilla_assets}"]
    for block_id in hidden_ids(blocks):
        if not (vanilla_assets / "blockstates" / f"{block_id}.json").is_file():
            problems.append(f"原版 26.1 没有 blockstate：{block_id}")
    return problems


# --------------------------------------------------------------------------
# 去剔除（no-cull）覆盖：复制原版模型链，去掉指定面的 cullface
# --------------------------------------------------------------------------


def nocull_model_id(model_id: str) -> str:
    _, path = split_id(model_id)
    if path.startswith("block/"):
        path = path[len("block/") :]
    return NOCULL_PREFIX + path


def copy_model_chain(
    vanilla: Path,
    model_id: str,
    faces: set[str] | None,
    created: set[str],
    problems: list[str],
) -> str | None:
    """复制模型及其 parent 链，返回我们命名空间下的模型 id。faces=None 表示去掉全部 cullface。"""
    target_id = nocull_model_id(model_id)
    if target_id in created:
        return target_id
    _, path = split_id(model_id)
    source = vanilla / "models" / (path + ".json")
    if not source.is_file():
        problems.append(f"去剔除覆盖：原版模型不存在 {model_id}")
        return None

    data = load_json(source)
    out: dict = {}
    parent = data.get("parent")
    if isinstance(parent, str):
        copied_parent = copy_model_chain(vanilla, parent, faces, created, problems)
        if copied_parent:
            out["parent"] = copied_parent
    for key in ("ambientocclusion", "gui_light", "display", "textures"):
        if key in data:
            out[key] = data[key]
    elements = data.get("elements")
    if isinstance(elements, list):
        new_elements = []
        for element in elements:
            new_element = copy.deepcopy(element)
            face_map = new_element.get("faces")
            if isinstance(face_map, dict):
                for face in face_map.values():
                    if isinstance(face, dict) and "cullface" in face:
                        if faces is None or face.get("cullface") in faces:
                            del face["cullface"]
            new_elements.append(new_element)
        out["elements"] = new_elements
    _, target_path = split_id(target_id)
    relative = target_path[len("block/nocull/"):]
    write_json(NOCULL_MODELS_DIR / (relative + ".json"), out)
    created.add(target_id)
    return target_id


def copy_blockstate(vanilla: Path, block_id: str, faces: set[str] | None, created: set[str], problems: list[str]) -> None:
    source = vanilla / "blockstates" / f"{block_id}.json"
    if not source.is_file():
        problems.append(f"去剔除覆盖：原版 blockstate 不存在 {block_id}")
        return
    data = load_json(source)

    def rewrite(entry) -> None:
        if isinstance(entry, dict) and isinstance(entry.get("model"), str):
            copied = copy_model_chain(vanilla, entry["model"], faces, created, problems)
            if copied:
                entry["model"] = copied

    variants = data.get("variants")
    if isinstance(variants, dict):
        for value in variants.values():
            for entry in value if isinstance(value, list) else [value]:
                rewrite(entry)
    multipart = data.get("multipart")
    if isinstance(multipart, list):
        for selector in multipart:
            apply = selector.get("apply") if isinstance(selector, dict) else None
            for entry in apply if isinstance(apply, list) else [apply]:
                rewrite(entry)
    write_json(BLOCKSTATES_DIR / f"{block_id}.json", data)


def generate_nocull(vanilla: Path, profiles: list[dict], blocked: set[str]) -> tuple[int, list[str]]:
    problems: list[str] = []
    created: set[str] = set()
    blockstate_count = 0
    for profile in profiles:
        faces_value = profile.get("faces", ["up"])
        faces = None if "all" in faces_value else set(faces_value)
        for block_id in profile.get("blocks", []):
            if block_id in blocked:
                problems.append(f"去剔除覆盖与隐藏清单重叠（跳过）：{block_id}")
                continue
            copy_blockstate(vanilla, block_id, faces, created, problems)
            blockstate_count += 1
    return blockstate_count, problems


def main(argv=None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check-assets", action="store_true", help="核对 26.1 原版 blockstate 是否存在")
    parser.add_argument("--vanilla-assets", default=str(DEFAULT_VANILLA_ASSETS), help="26.1 assets/minecraft 目录")
    args = parser.parse_args(argv)

    blocks = load_json(BLOCKS_FILE)
    particles = load_json(PARTICLES_FILE).get("particles", {})
    nocull_profiles = load_json(NOCULL_FILE).get("profiles", []) if NOCULL_FILE.is_file() else []
    vanilla = Path(args.vanilla_assets)

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
        problems = check_against_vanilla(blocks, vanilla)
        if problems:
            print("ERROR: 清单与原版资产不一致：", file=sys.stderr)
            for problem in problems:
                print(f"  - {problem}", file=sys.stderr)
            return 1

    if nocull_profiles and not (vanilla / "blockstates").is_dir():
        print(f"ERROR: 去剔除覆盖需要原版 assets，但找不到：{vanilla}", file=sys.stderr)
        print("       用 --vanilla-assets 指定 26.1 的 assets/minecraft 目录。", file=sys.stderr)
        return 1

    # 清理旧的生成结果，避免缩清单后残留
    for path in (BLOCKSTATES_DIR, MODELS_DIR, NOCULL_MODELS_DIR):
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

    nocull_blockstates = 0
    nocull_problems: list[str] = []
    if nocull_profiles:
        nocull_blockstates, nocull_problems = generate_nocull(vanilla, nocull_profiles, set(ids) | alias_ids)

    write_pack_meta(blocks["pack"])
    write_block_list(blocks, particles, nocull_profiles)

    nocull_models = len(list(NOCULL_MODELS_DIR.glob("**/*.json"))) if NOCULL_MODELS_DIR.is_dir() else 0
    print(
        f"生成完成：隐藏 {written} 个方块；去剔除覆盖 {nocull_blockstates} 个方块、{nocull_models} 个模型；"
        "pack.mcmeta、docs/BLOCK-LIST.md 已更新"
    )
    if unresolved:
        print(f"警告：{len(unresolved)} 个方块没有粒子贴图（模型里会省略 particle）：")
        print("  " + ", ".join(unresolved))
    if nocull_problems:
        print("去剔除覆盖的问题：", file=sys.stderr)
        for problem in nocull_problems:
            print(f"  - {problem}", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

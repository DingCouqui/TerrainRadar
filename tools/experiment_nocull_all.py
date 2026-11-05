#!/usr/bin/env python3
"""生成 26.1 专用的「全量去剔除」实验包（仅用于测试视觉与 FPS，不进默认产物）。

做法：读取 26.1 原版 assets，把每个方块（隐藏清单除外）的 blockstate 复制到实验包，
并把引用的模型复制成「去掉全部 cullface」的副本；隐藏方块沿用主包的 blockstates 与空模型。
产物：dist/TerrainRadar-<version>-nocull-all-26.1.zip

注意：模型几何来自 26.1，只在 26.1 上使用；其它版本请用主包。

用法：
    python tools/experiment_nocull_all.py
"""

from __future__ import annotations

import argparse
import json
import shutil
import sys
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(ROOT / "tools"))

import generate as gen  # noqa: E402

STAGING = ROOT / "build" / "nocull-all"


def main(argv=None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--vanilla-assets", default=str(gen.DEFAULT_VANILLA_ASSETS), help="26.1 assets/minecraft 目录")
    parser.add_argument("--out-dir", default=str(ROOT / "dist"), help="输出目录")
    args = parser.parse_args(argv)

    vanilla = Path(args.vanilla_assets)
    if not (vanilla / "blockstates").is_dir():
        print(f"ERROR: 找不到原版 assets：{vanilla}", file=sys.stderr)
        return 1

    blocks = gen.load_json(gen.BLOCKS_FILE)
    ids = gen.hidden_ids(blocks)
    aliases = [alias["id"] for alias in blocks.get("legacy_aliases", [])]
    hidden = set(ids) | set(aliases)

    if STAGING.exists():
        shutil.rmtree(STAGING)
    staging_mc = STAGING / "assets" / "minecraft"
    blockstates_out = staging_mc / "blockstates"
    nocull_models_out = staging_mc / "models" / "block" / "nocull"
    blockstates_out.mkdir(parents=True, exist_ok=True)
    nocull_models_out.mkdir(parents=True, exist_ok=True)

    # 让 generate 的复制函数写到实验包的 staging 目录
    gen.BLOCKSTATES_DIR = blockstates_out
    gen.NOCULL_MODELS_DIR = nocull_models_out

    created: set[str] = set()
    problems: list[str] = []
    copied_blockstates = 0
    for path in sorted((vanilla / "blockstates").glob("*.json")):
        block_id = path.stem
        if block_id in hidden:
            continue
        # 隐藏清单里的别名（1.20.2 的 grass）在 26.1 不存在，跳过
        gen.copy_blockstate(vanilla, block_id, None, created, problems)
        copied_blockstates += 1

    # 隐藏方块沿用主包的 blockstates 与空模型
    repo_mc = ROOT / "assets" / "minecraft"
    for block_id in ids:
        source = repo_mc / "blockstates" / f"{block_id}.json"
        if not source.is_file():
            problems.append(f"缺少主包的隐藏 blockstate：{block_id}")
            continue
        shutil.copy2(source, blockstates_out / source.name)
    terrainradar_out = staging_mc / "models" / "block" / "terrainradar"
    terrainradar_out.mkdir(parents=True, exist_ok=True)
    for source in (repo_mc / "models" / "block" / "terrainradar").glob("*.json"):
        shutil.copy2(source, terrainradar_out / source.name)

    pack = blocks["pack"]
    version = pack["version"]
    mcmeta = {
        "pack": {
            "pack_format": pack["pack_format"],
            "supported_formats": pack["supported_formats"],
            "min_format": pack["min_format"],
            "max_format": pack["max_format"],
            "description": f"TerrainRadar v{version} | 26.1 全量去剔除实验包（仅测试 FPS/视觉）",
        }
    }
    (STAGING / "pack.mcmeta").write_text(
        json.dumps(mcmeta, ensure_ascii=False, indent=2) + "\n", encoding="utf-8", newline="\n"
    )
    shutil.copy2(ROOT / "pack.png", STAGING / "pack.png")

    # 校验：每个 blockstate 的模型引用都在 staging 里存在
    referenced = set()
    for path in blockstates_out.glob("*.json"):
        try:
            data = json.loads(path.read_text(encoding="utf-8"))
        except json.JSONDecodeError as exc:
            problems.append(f"blockstate 解析失败 {path.name}: {exc}")
            continue

        def collect(entry) -> None:
            if isinstance(entry, dict) and isinstance(entry.get("model"), str):
                referenced.add(entry["model"])

        for value in (data.get("variants") or {}).values():
            for entry in value if isinstance(value, list) else [value]:
                collect(entry)
        for selector in data.get("multipart") or []:
            apply = selector.get("apply") if isinstance(selector, dict) else None
            for entry in apply if isinstance(apply, list) else [apply]:
                collect(entry)
    for model_id in sorted(referenced):
        _, model_path = model_id.split(":", 1)
        if not (staging_mc / "models" / (model_path + ".json")).is_file():
            problems.append(f"模型引用缺失：{model_id}")

    files = [STAGING / "pack.mcmeta", STAGING / "pack.png"]
    files.extend(sorted(path for path in (STAGING / "assets").rglob("*") if path.is_file()))

    out_dir = Path(args.out_dir)
    out_dir.mkdir(parents=True, exist_ok=True)
    out_path = out_dir / f"TerrainRadar-v{version}-nocull-all-26.1.zip"
    stamp = (2020, 1, 1, 0, 0, 0)
    with zipfile.ZipFile(out_path, "w", compression=zipfile.ZIP_DEFLATED, compresslevel=9) as archive:
        for path in files:
            info = zipfile.ZipInfo(path.relative_to(STAGING).as_posix(), date_time=stamp)
            info.compress_type = zipfile.ZIP_DEFLATED
            info.external_attr = 0o644 << 16
            archive.writestr(info, path.read_bytes())

    total = sum(path.stat().st_size for path in files)
    print(f"已写入 {out_path}")
    print(
        f"  去剔除方块：{copied_blockstates}（隐藏 {len(ids)} 个沿用主包）；"
        f"文件数：{len(files)}；原始 {total / 1024:.1f} KiB，zip {out_path.stat().st_size / 1024:.1f} KiB"
    )
    if problems:
        print(f"警告：{len(problems)} 个问题", file=sys.stderr)
        for problem in problems[:20]:
            print(f"  - {problem}", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

#!/usr/bin/env python3
"""把生成好的资源包内容打成可直接放进 resourcepacks/ 的 zip。

包内结构（zip 根目录）：
    pack.mcmeta
    pack.png
    assets/minecraft/blockstates/*.json
    assets/minecraft/models/block/terrainradar/*.json

用法：
    python tools/build.py
"""

from __future__ import annotations

import argparse
import json
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
BLOCKS_FILE = ROOT / "data" / "blocks.json"
ASSETS_DIR = ROOT / "assets"
DIST_DIR = ROOT / "dist"
REQUIRED = ("pack.mcmeta", "pack.png")


def main(argv=None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--out-dir", default=str(DIST_DIR), help="输出目录")
    args = parser.parse_args(argv)

    blocks = json.loads(BLOCKS_FILE.read_text(encoding="utf-8"))
    pack = blocks["pack"]
    zip_name = f"{pack['name']}-v{pack['version']}.zip"

    for name in REQUIRED:
        if not (ROOT / name).is_file():
            raise SystemExit(f"缺少必需文件：{name}（先运行 tools/generate.py 和 tools/make_icon.py）")
    if not (ASSETS_DIR / "minecraft" / "blockstates").is_dir():
        raise SystemExit("缺少 assets/minecraft/blockstates（先运行 tools/generate.py）")

    out_dir = Path(args.out_dir)
    out_dir.mkdir(parents=True, exist_ok=True)
    out_path = out_dir / zip_name

    files: list[Path] = [ROOT / name for name in REQUIRED]
    files.extend(sorted(path for path in ASSETS_DIR.rglob("*") if path.is_file()))

    # 固定时间戳，保证同内容产出的 zip 哈希稳定（便于比对与校验）。
    stamp = (2020, 1, 1, 0, 0, 0)
    with zipfile.ZipFile(out_path, "w", compression=zipfile.ZIP_DEFLATED, compresslevel=9) as archive:
        for path in files:
            info = zipfile.ZipInfo(path.relative_to(ROOT).as_posix(), date_time=stamp)
            info.compress_type = zipfile.ZIP_DEFLATED
            info.external_attr = 0o644 << 16
            archive.writestr(info, path.read_bytes())

    total = sum(path.stat().st_size for path in files)
    with zipfile.ZipFile(out_path) as archive:
        members = archive.namelist()
    print(f"已写入 {out_path}")
    print(f"  文件数：{len(members)}（原始 {total / 1024:.1f} KiB，zip {out_path.stat().st_size / 1024:.1f} KiB）")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

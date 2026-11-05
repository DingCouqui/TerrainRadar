#!/usr/bin/env python3
"""生成 pack.png（128×128，Pillow）。"""

from __future__ import annotations

from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / "pack.png"
SIZE = 128


def main() -> int:
    image = Image.new("RGBA", (SIZE, SIZE), (11, 18, 14, 255))
    draw = ImageDraw.Draw(image)

    # 地平线：上暗下亮的渐层，用几条色带近似
    bands = [
        (0.00, (18, 30, 24, 255)),
        (0.30, (26, 44, 33, 255)),
        (0.55, (34, 58, 42, 255)),
        (0.72, (52, 84, 58, 255)),
        (1.00, (86, 128, 82, 255)),
    ]
    previous = 0.0
    for ratio, color in bands:
        top = int(previous * SIZE)
        bottom = int(ratio * SIZE)
        draw.rectangle([0, top, SIZE, bottom], fill=color)
        previous = ratio

    # 扫描网格：细点阵
    for y in range(8, SIZE, 8):
        for x in range(8, SIZE, 8):
            draw.point((x, y), fill=(255, 255, 255, 30))

    # 中央"雷达"圆环与扫描线
    center = (SIZE // 2, SIZE // 2)
    for radius, color in ((46, (120, 230, 140, 90)), (30, (120, 230, 140, 130)), (14, (150, 250, 160, 200))):
        box = [center[0] - radius, center[1] - radius, center[0] + radius, center[1] + radius]
        draw.ellipse(box, outline=color, width=2)
    draw.line([center, (center[0] + 40, center[1] - 24)], fill=(170, 255, 180, 220), width=2)
    draw.ellipse([center[0] - 3, center[1] - 3, center[0] + 3, center[1] + 3], fill=(190, 255, 200, 255))

    image.save(OUT, "PNG")
    print(f"已写入 {OUT}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

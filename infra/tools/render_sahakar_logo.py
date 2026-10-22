#!/usr/bin/env python3
"""Circular Sahakar Bharati seal: sun, wheat sheaf, fish."""

from math import cos, pi, sin
from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter

SIZE = 1024
NAVY = (23, 38, 63, 255)
NAVY_DEEP = (2, 17, 41, 255)
SAFFRON = (254, 152, 58, 255)
SAFFRON_DEEP = (233, 135, 42, 255)
GOLD = (232, 193, 121, 255)
GOLD_DEEP = (210, 164, 82, 255)
TEAL = (61, 184, 176, 255)
TEAL_DEEP = (31, 127, 120, 255)
WHITE = (255, 255, 255, 255)


def xy(x, y):
    return int(round(x)), int(round(y))


def rot(px, py, cx, cy, deg):
    rad = deg * pi / 180.0
    x, y = px - cx, py - cy
    return cx + x * cos(rad) - y * sin(rad), cy + x * sin(rad) + y * cos(rad)


def diamond(draw, cx, cy, length, width, angle, fill):
    pts = [
        (cx, cy + length * 0.12),
        (cx - width, cy - length * 0.35),
        (cx, cy - length),
        (cx + width, cy - length * 0.35),
    ]
    draw.polygon([xy(*rot(x, y, cx, cy, angle)) for x, y in pts], fill=fill)


def wheat_ear(draw, bx, by, h, lean):
    tx, ty = bx + lean, by - h
    mid_x, mid_y = bx + (tx - bx) * 0.42, by + (ty - by) * 0.42
    draw.line([xy(bx, by), xy(mid_x, mid_y)], fill=GOLD_DEEP, width=9)
    steps = 5
    for i in range(steps):
        t = 0.36 + i * (0.58 / (steps - 1))
        x = bx + (tx - bx) * t
        y = by + (ty - by) * t
        length = h * (0.22 - i * 0.012)
        width = h * 0.038
        diamond(draw, x, y, length, width, -38 + lean * 0.35, SAFFRON if i % 2 == 0 else GOLD)
        diamond(draw, x, y, length, width, 38 + lean * 0.35, GOLD if i % 2 == 0 else SAFFRON)
    diamond(draw, tx, ty, h * 0.16, h * 0.032, lean * 0.15, GOLD)


def fish(draw, cx, cy, s):
    body = [
        (cx - 1.00 * s, cy),
        (cx - 0.55 * s, cy - 0.28 * s),
        (cx + 0.05 * s, cy - 0.34 * s),
        (cx + 0.42 * s, cy - 0.08 * s),
        (cx + 0.42 * s, cy + 0.08 * s),
        (cx + 0.05 * s, cy + 0.34 * s),
        (cx - 0.55 * s, cy + 0.28 * s),
    ]
    draw.polygon([xy(*p) for p in body], fill=TEAL)
    tail = [
        (cx + 0.38 * s, cy),
        (cx + 0.92 * s, cy - 0.36 * s),
        (cx + 0.92 * s, cy + 0.36 * s),
    ]
    draw.polygon([xy(*p) for p in tail], fill=TEAL_DEEP)
    fin = [
        (cx - 0.04 * s, cy - 0.30 * s),
        (cx + 0.16 * s, cy - 0.56 * s),
        (cx + 0.20 * s, cy - 0.18 * s),
    ]
    draw.polygon([xy(*p) for p in fin], fill=TEAL_DEEP)
    er = max(5, int(0.08 * s))
    ex, ey = cx - 0.58 * s, cy - 0.04 * s
    draw.ellipse([ex - er, ey - er, ex + er, ey + er], fill=NAVY_DEEP)
    sr = max(2, er // 3)
    draw.ellipse([ex - sr + 2, ey - sr - 2, ex + sr + 2, ey + sr - 2], fill=WHITE)


def sun(draw, cx, cy, r):
    for i in range(8):
        a = i * 45
        x1, y1 = xy(*rot(cx, cy - r * 1.18, cx, cy, a))
        x2, y2 = xy(*rot(cx, cy - r * 1.48, cx, cy, a))
        draw.line([(x1, y1), (x2, y2)], fill=GOLD, width=10)
    draw.ellipse([cx - r, cy - r, cx + r, cy + r], fill=SAFFRON)
    ir = r * 0.55
    draw.ellipse([cx - ir, cy - ir, cx + ir, cy + ir], fill=SAFFRON_DEEP)


def render():
    img = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    glow = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    gd = ImageDraw.Draw(glow)
    c = SIZE / 2
    r = SIZE * 0.445
    gd.ellipse([c - r - 20, c - r - 20, c + r + 20, c + r + 20], fill=(254, 152, 58, 36))
    img = Image.alpha_composite(img, glow.filter(ImageFilter.GaussianBlur(18)))
    draw = ImageDraw.Draw(img)

    draw.ellipse([c - r, c - r, c + r, c + r], fill=NAVY)
    draw.ellipse([c - r, c - r, c + r, c + r], outline=GOLD, width=26)
    inset = 40
    draw.ellipse(
        [c - r + inset, c - r + inset, c + r - inset, c + r - inset],
        outline=SAFFRON,
        width=4,
    )

    sun(draw, c, c - r * 0.34, r * 0.15)
    band_y = c + r * 0.28
    wheat_ear(draw, c - r * 0.28, band_y, r * 0.42, -14)
    wheat_ear(draw, c - r * 0.20, band_y + 4, r * 0.46, 0)
    wheat_ear(draw, c - r * 0.12, band_y, r * 0.40, 14)
    draw.rounded_rectangle(
        [c - r * 0.34, band_y - 7, c - r * 0.06, band_y + 7],
        radius=6,
        fill=SAFFRON_DEEP,
    )
    fish(draw, c + r * 0.24, c + r * 0.18, r * 0.26)
    return img


def main():
    img = render()
    img.save("/tmp/sahakar_logo_full.png")
    root = Path(__file__).resolve().parents[2] / "android" / "app" / "src" / "main" / "res"
    for folder, edge in {
        "drawable-mdpi": 162,
        "drawable-hdpi": 243,
        "drawable-xhdpi": 324,
        "drawable-xxhdpi": 486,
        "drawable-xxxhdpi": 648,
    }.items():
        out = root / folder
        out.mkdir(parents=True, exist_ok=True)
        img.resize((edge, edge), Image.Resampling.LANCZOS).save(out / "ic_sahakar_logo.png")
    print("wrote density logos")


if __name__ == "__main__":
    main()

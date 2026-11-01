#!/usr/bin/env python3
"""Regenerate Android TV launcher banner + icons for Play TV quality (TV-LB / TV-BN).

Brand: blue triangle on violet — never the old yellow/blue circle + yellow square mark.

On demand:
  ./scripts/gen-tv-assets.sh
  python3 scripts/playstore/gen_tv_launcher_assets.py

Outputs (opaque RGB, edge-to-edge — no letterboxing / empty margins):
  - androidApp/.../drawable-xhdpi/ic_banner.png   320×180  (app name in image)
  - androidApp/.../mipmap-*/ic_launcher(.png|_round.png)  TV sizes
  - playstore/icon-512.png
  - playstore/tv-banner.png   1280×720 listing banner (name + logo)

Refs:
  https://developer.android.com/training/tv/get-started/create#banner
  https://developer.android.com/docs/quality-guidelines/tv-app-quality (TV-LB, TV-BN)
  https://developer.android.com/design/ui/tv/guides/system/tv-app-icon-guidelines
"""

from __future__ import annotations

import argparse
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parents[2]
RES = ROOT / "androidApp/src/main/res"
PLAYSTORE = ROOT / "playstore"
WEBSITE_BRAND = ROOT / "website/assets/brand"

# Brand tokens — violet field + blue triangle (feature-graphic / website brand)
VIOLET = (42, 24, 72)  # #2A1848 full-bleed background
VIOLET_ORB_A = (55, 35, 95)
VIOLET_ORB_B = (38, 42, 88)
VIOLET_ORB_C = (62, 40, 98)
BLUE = (72, 118, 168)  # slate-blue triangle body
BLUE_DEEP = (48, 78, 118)  # outer stroke
BLUE_LIGHT = (140, 175, 215)  # left facet highlight
LAVENDER = (196, 181, 253)  # soft rim (feature-graphic accent)
PINK = (220, 170, 210)  # right facet (subtle, not yellow)
WHITE = (255, 255, 255)

APP_NAME = "Arthur"

# TV launcher icon sizes (larger than phone mdpi…xxxhdpi)
ICON_SIZES = {
    "mipmap-mdpi": 80,
    "mipmap-hdpi": 120,
    "mipmap-xhdpi": 160,
    "mipmap-xxhdpi": 240,
    "mipmap-xxxhdpi": 320,
}

# Banner density pack (xhdpi required by Play review tooling)
BANNER_SIZES = {
    "drawable-xhdpi": (320, 180),
    "drawable-xxhdpi": (480, 270),
    "drawable-xxxhdpi": (640, 360),
}


def _font(size: int) -> ImageFont.FreeTypeFont | ImageFont.ImageFont:
    candidates = [
        "/System/Library/Fonts/Supplemental/Arial Bold.ttf",
        "/System/Library/Fonts/Supplemental/Arial.ttf",
        "/Library/Fonts/Arial Bold.ttf",
        "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf",
        "/usr/share/fonts/truetype/liberation/LiberationSans-Bold.ttf",
    ]
    for path in candidates:
        if Path(path).is_file():
            return ImageFont.truetype(path, size=size)
    return ImageFont.load_default()


def _triangle_points(
    cx: float, cy: float, half_w: float, half_h: float
) -> tuple[tuple[float, float], tuple[float, float], tuple[float, float]]:
    return (
        (cx, cy - half_h),
        (cx - half_w, cy + half_h),
        (cx + half_w, cy + half_h),
    )


def _draw_blue_triangle(
    draw: ImageDraw.ImageDraw,
    cx: float,
    cy: float,
    half_w: float,
    half_h: float,
) -> None:
    """Solid blue triangle on violet — fills its bbox (no hollow / no yellow)."""
    top, bl, br = _triangle_points(cx, cy, half_w, half_h)

    # Deep blue outer (slightly larger) so the mark reads full against violet
    glow = 1.03
    draw.polygon(
        list(_triangle_points(cx, cy, half_w * glow, half_h * glow)),
        fill=BLUE_DEEP,
    )
    # Solid blue body — reviewers reject hollow logos as “empty icon space”
    draw.polygon([top, bl, br], fill=BLUE)
    # Left facet highlight
    mid_l = (cx - half_w * 0.18, cy + half_h * 0.15)
    draw.polygon([top, bl, mid_l], fill=BLUE_LIGHT)
    # Right facet (lavender/pink — brand, never yellow)
    mid_r = (cx + half_w * 0.22, cy + half_h * 0.12)
    draw.polygon([top, br, mid_r], fill=PINK)
    # Thin lavender rim (feature-graphic accent)
    inset = 0.92
    rim = list(_triangle_points(cx, cy, half_w * inset, half_h * inset))
    # Draw as thick outline via nested fill of violet then rebuild? Keep simple rim strip:
    # leave solid; soft lavender tip accent only
    tip = (cx, cy - half_h * 0.55)
    draw.polygon([top, (cx - half_w * 0.12, cy - half_h * 0.15), tip], fill=LAVENDER)


def render_icon(size: int) -> Image.Image:
    """Square launcher icon — violet full bleed, blue triangle fills the tile."""
    im = Image.new("RGB", (size, size), VIOLET)
    draw = ImageDraw.Draw(im)
    # Vertices nearly on canvas edges → “fills the entire icon space”
    inset = max(1, size // 64)
    top_y = float(inset)
    base_y = float(size - 1 - inset)
    left_x = float(inset)
    right_x = float(size - 1 - inset)
    cx = size / 2.0
    cy = (top_y + base_y) / 2.0
    half_w = (right_x - left_x) / 2.0
    half_h = (base_y - top_y) / 2.0
    _draw_blue_triangle(draw, cx, cy, half_w, half_h)
    return im


def render_round_icon(size: int) -> Image.Image:
    base = render_icon(size)
    mask = Image.new("L", (size, size), 0)
    ImageDraw.Draw(mask).ellipse((0, 0, size - 1, size - 1), fill=255)
    circle = Image.new("RGB", (size, size), VIOLET)
    circle.paste(base, mask=mask)
    return circle


def render_banner_hires(width: int, height: int, *, title: str = APP_NAME) -> Image.Image:
    """16:9 home-screen banner — violet orbs, blue triangle, app name (TV-BN)."""
    im = Image.new("RGB", (width, height), VIOLET)
    draw = ImageDraw.Draw(im)

    # Soft violet orbs (feature-graphic atmosphere) — opaque, edge-to-edge field
    for cx, cy, r, color in (
        (int(width * 0.20), int(height * 0.48), int(height * 0.58), VIOLET_ORB_A),
        (int(width * 0.72), int(height * 0.38), int(height * 0.52), VIOLET_ORB_B),
        (int(width * 0.52), int(height * 0.88), int(height * 0.42), VIOLET_ORB_C),
    ):
        draw.ellipse((cx - r, cy - r, cx + r, cy + r), fill=color)

    # Logo left — large blue triangle (fills vertical band)
    logo_cx = width * 0.22
    logo_cy = height * 0.50
    half_h = height * 0.40
    half_w = half_h * 1.05
    _draw_blue_triangle(draw, logo_cx, logo_cy, half_w, half_h)

    # App name (TV-BN) — right of logo
    font_size = max(28, int(height * 0.30))
    font = _font(font_size)
    bbox = draw.textbbox((0, 0), title, font=font)
    tw, th = bbox[2] - bbox[0], bbox[3] - bbox[1]
    tx = int(width * 0.42)
    ty = int((height - th) / 2 - bbox[1])
    max_right = width - int(width * 0.06)
    while font_size > 16 and tx + tw > max_right:
        font_size -= 2
        font = _font(font_size)
        bbox = draw.textbbox((0, 0), title, font=font)
        tw, th = bbox[2] - bbox[0], bbox[3] - bbox[1]
        ty = int((height - th) / 2 - bbox[1])
    draw.text((tx, ty), title, font=font, fill=WHITE)
    return im


def render_banner(width: int, height: int, *, title: str = APP_NAME) -> Image.Image:
    """Render banner at 4× then downscale for crisp text at xhdpi 320×180."""
    scale = 4 if width <= 640 else 1
    hi = render_banner_hires(width * scale, height * scale, title=title)
    if scale == 1:
        return hi
    return hi.resize((width, height), Image.Resampling.LANCZOS)


def write_png(path: Path, im: Image.Image) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    rgb = im.convert("RGB")
    rgb.save(path, format="PNG", optimize=True)
    print(f"  wrote {path.relative_to(ROOT)} ({rgb.size[0]}×{rgb.size[1]})")


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--name",
        default=APP_NAME,
        help="App name drawn on the TV banner (TV-BN)",
    )
    parser.add_argument(
        "--skip-playstore",
        action="store_true",
        help="Only write in-APK res/; skip playstore/*.png",
    )
    args = parser.parse_args()

    print("Generating TV launcher assets (blue triangle on violet)…")

    banner_xhdpi = render_banner(320, 180, title=args.name)
    for folder, (w, h) in BANNER_SIZES.items():
        img = banner_xhdpi if (w, h) == (320, 180) else render_banner(w, h, title=args.name)
        write_png(RES / folder / "ic_banner.png", img)
    write_png(RES / "drawable/ic_banner.png", banner_xhdpi)

    for folder, size in ICON_SIZES.items():
        write_png(RES / folder / "ic_launcher.png", render_icon(size))
        write_png(RES / folder / "ic_launcher_round.png", render_round_icon(size))

    if not args.skip_playstore:
        icon512 = render_icon(512)
        write_png(PLAYSTORE / "icon-512.png", icon512)
        write_png(
            PLAYSTORE / "tv-banner.png",
            render_banner(1280, 720, title=args.name),
        )
        if WEBSITE_BRAND.is_dir():
            write_png(WEBSITE_BRAND / "icon-512.png", icon512)

    vector = RES / "drawable/ic_banner.xml"
    if vector.exists():
        vector.unlink()
        print(f"  removed {vector.relative_to(ROOT)} (use drawable-xhdpi PNG)")

    print("Done.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

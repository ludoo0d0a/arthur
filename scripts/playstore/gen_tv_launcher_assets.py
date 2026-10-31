#!/usr/bin/env python3
"""Regenerate Android TV launcher banner + icons for Play TV quality (TV-LB / TV-BN).

On demand:
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

# Brand tokens (match ic_launcher_* / feature-graphic)
BG = (42, 24, 72)  # #2A1848
BG_BANNER = (11, 16, 32)  # #0B1020
VIOLET = (196, 181, 253)  # #C4B5FD
TEAL = (94, 234, 212)  # #5EEAD4
PINK = (249, 168, 212)  # #F9A8D4
INK = (42, 24, 72)
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


def _draw_triangle(
    draw: ImageDraw.ImageDraw,
    cx: float,
    cy: float,
    half_w: float,
    half_h: float,
) -> None:
    """Folded triangle logo centered at (cx, cy), sized by half extents."""
    top = (cx, cy - half_h)
    bl = (cx - half_w, cy + half_h)
    br = (cx + half_w, cy + half_h)

    # Teal under-glow (slightly larger)
    glow = 1.08
    draw.polygon(
        [
            (cx, cy - half_h * glow),
            (cx - half_w * glow, cy + half_h * glow),
            (cx + half_w * glow, cy + half_h * glow),
        ],
        fill=TEAL,
    )
    # Outer violet
    draw.polygon([top, bl, br], fill=VIOLET)
    # Inner cutout (hollow core)
    inset = 0.38
    draw.polygon(
        [
            (cx, cy - half_h * (1 - inset * 0.55)),
            (cx - half_w * (1 - inset), cy + half_h * (1 - inset * 0.15)),
            (cx + half_w * (1 - inset), cy + half_h * (1 - inset * 0.15)),
        ],
        fill=INK,
    )
    # Pink facet (right edge accent)
    mid_r = (cx + half_w * 0.42, cy + half_h * 0.15)
    draw.polygon([top, br, mid_r], fill=PINK)


def render_icon(size: int, *, fill_ratio: float = 1.0) -> Image.Image:
    """Square launcher icon that fills the tile (TV: no empty icon margins).

    Triangle vertices sit on the canvas edges (tiny inset only) so the mark
    occupies the full icon space instead of floating in a padded safe-zone.
    """
    im = Image.new("RGB", (size, size), BG)
    draw = ImageDraw.Draw(im)
    # Inset 1px so anti-alias soft edges stay inside the bitmap.
    inset = max(1, size // 128)
    # Tip touches top; base corners sit near bottom-left / bottom-right.
    top = (size / 2, float(inset))
    bl = (float(inset), float(size - 1 - inset))
    br = (float(size - 1 - inset), float(size - 1 - inset))
    cx = size / 2
    cy = (top[1] + bl[1]) / 2
    half_w = (br[0] - bl[0]) / 2
    half_h = (bl[1] - top[1]) / 2
    # Optional uniform scale-down from full-bleed (tests / store variants)
    if fill_ratio < 1.0:
        half_w *= fill_ratio
        half_h *= fill_ratio
    draw.polygon(
        [
            (cx, cy - half_h * 1.04),
            (cx - half_w * 1.04, cy + half_h * 1.04),
            (cx + half_w * 1.04, cy + half_h * 1.04),
        ],
        fill=(70, 180, 170),
    )
    _draw_triangle(draw, cx, cy, half_w, half_h)
    return im


def render_round_icon(size: int) -> Image.Image:
    base = render_icon(size)
    mask = Image.new("L", (size, size), 0)
    ImageDraw.Draw(mask).ellipse((0, 0, size - 1, size - 1), fill=255)
    out = Image.new("RGB", (size, size), BG)
    out.paste(base, (0, 0))
    # Circular crop onto brand bg (opaque — no alpha for TV validators)
    circle = Image.new("RGB", (size, size), BG)
    circle.paste(out, mask=mask)
    return circle


def render_banner(width: int, height: int, *, title: str = APP_NAME) -> Image.Image:
    """16:9 home-screen banner with localized app name (TV-BN)."""
    im = Image.new("RGB", (width, height), BG_BANNER)
    draw = ImageDraw.Draw(im)

    # Soft orbs (atmosphere, still edge-to-edge fill)
    for cx, cy, r, color in (
        (int(width * 0.18), int(height * 0.45), int(height * 0.55), (35, 28, 70)),
        (int(width * 0.78), int(height * 0.35), int(height * 0.50), (28, 40, 72)),
        (int(width * 0.55), int(height * 0.85), int(height * 0.40), (50, 30, 80)),
    ):
        draw.ellipse((cx - r, cy - r, cx + r, cy + r), fill=color)

    # Logo on the left — large, fills vertical safe area
    logo_cx = width * 0.22
    logo_cy = height * 0.50
    half_h = height * 0.38
    half_w = half_h * 1.05
    draw.polygon(
        [
            (logo_cx, logo_cy - half_h * 1.08),
            (logo_cx - half_w * 1.08, logo_cy + half_h * 1.08),
            (logo_cx + half_w * 1.08, logo_cy + half_h * 1.08),
        ],
        fill=(55, 150, 145),
    )
    _draw_triangle(draw, logo_cx, logo_cy, half_w, half_h)

    # App name (required by TV-BN) — right of logo
    font_size = max(28, int(height * 0.28))
    font = _font(font_size)
    # Vertical center via textbbox
    bbox = draw.textbbox((0, 0), title, font=font)
    tw, th = bbox[2] - bbox[0], bbox[3] - bbox[1]
    tx = int(width * 0.42)
    ty = int((height - th) / 2 - bbox[1])
    # Keep text inside right margin
    max_right = width - int(width * 0.06)
    if tx + tw > max_right:
        # shrink font until it fits
        while font_size > 16:
            font_size -= 2
            font = _font(font_size)
            bbox = draw.textbbox((0, 0), title, font=font)
            tw, th = bbox[2] - bbox[0], bbox[3] - bbox[1]
            ty = int((height - th) / 2 - bbox[1])
            if tx + tw <= max_right:
                break
    draw.text((tx, ty), title, font=font, fill=WHITE)
    return im


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

    print("Generating TV launcher assets…")

    # 1) Banners in drawable-*dpi (xhdpi is what Play reviewers cite)
    banner_xhdpi = render_banner(320, 180, title=args.name)
    for folder, (w, h) in BANNER_SIZES.items():
        img = banner_xhdpi if (w, h) == (320, 180) else render_banner(w, h, title=args.name)
        write_png(RES / folder / "ic_banner.png", img)
    # Default drawable/ fallback (same pixels as xhdpi) — never a vector
    write_png(RES / "drawable/ic_banner.png", banner_xhdpi)

    # 2) Launcher icons — TV pixel sizes, full-bleed logo
    for folder, size in ICON_SIZES.items():
        write_png(RES / folder / "ic_launcher.png", render_icon(size))
        write_png(RES / folder / "ic_launcher_round.png", render_round_icon(size))

    if not args.skip_playstore:
        write_png(PLAYSTORE / "icon-512.png", render_icon(512))
        write_png(
            PLAYSTORE / "tv-banner.png",
            render_banner(1280, 720, title=args.name),
        )

    # Drop vector fallback so aapt cannot pick XML over the PNG
    vector = RES / "drawable/ic_banner.xml"
    if vector.exists():
        vector.unlink()
        print(f"  removed {vector.relative_to(ROOT)} (use drawable-xhdpi PNG)")

    print("Done.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

#!/usr/bin/env python3
"""Regenerate Android TV launcher banner + icons for Play TV quality (TV-LB / TV-BN).

Brand: Penrose triangle (impossible tribar) fully inscribed in the round
mask, with light zen/screensaver ambient accents (orbits + particles) on
violet — never the old yellow/blue circle + yellow square mark, never a
plain solid triangle.

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
import math
import re
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parents[2]
RES = ROOT / "androidApp/src/main/res"
PLAYSTORE = ROOT / "playstore"
WEBSITE_BRAND = ROOT / "website/assets/brand"
WEBSITE_ICON = ROOT / "website/assets/icon"

# Brand tokens — violet field + Penrose triangle (lavender / blue / pink)
VIOLET = (42, 24, 72)  # #2A1848 full-bleed background
VIOLET_ORB_A = (55, 35, 95)
VIOLET_ORB_B = (38, 42, 88)
VIOLET_ORB_C = (62, 40, 98)
PENROSE_LAVENDER = (196, 181, 253)  # #C4B5FD
PENROSE_BLUE = (91, 143, 200)  # #5B8FC8
PENROSE_PINK = (249, 168, 212)  # #F9A8D4
TEAL = (94, 234, 212)  # #5EEAD4 — zen / ambient accent
WHITE = (255, 255, 255)

# Penrose + ambient must sit fully inside the round mask (ratio of circle radius).
# Keep Penrose inside the innermost orbit so rings never cross the mark.
PENROSE_RADIUS_FRAC = 0.64
# Soft orbit rings / particles stay inside the circle, outside the Penrose.
AMBIENT_MAX_RADIUS_FRAC = 0.93
ORBIT_RADII = (0.76, 0.85, 0.93)

APP_NAME = "Arthur"

# Canonical Penrose geometry (Wikimedia Commons Penrose_triangle.svg paths).
# Three beams — draw order matters for the impossible join.
_PENROSE_PATHS = (
    "M 55.0625,182.4375 L 80.6875,182.5625 L 151.21875,59.84375 "
    "L 252.21615,228.15076 L 264.76729,205.25818 L 151.5625,13.25 "
    "L 55.0625,182.4375 z",
    "M 15.625,206.0625 L 27.5,228.4375 L 252.30454,228.28334 "
    "L 151.23246,59.859344 L 138.54873,81.934334 L 211.73429,205.92661 "
    "L 15.625,206.0625 z",
    "M 124.05609,12.990601 L 15.638759,206.0253 L 211.76418,205.96833 "
    "L 197.88064,182.51185 L 54.948939,182.532 L 151.67357,13.253813 "
    "L 124.05609,12.990601 z",
)
_PENROSE_COLORS = (PENROSE_LAVENDER, PENROSE_BLUE, PENROSE_PINK)
_NUM = re.compile(r"[-+]?\d*\.?\d+(?:[eE][-+]?\d+)?")


def _parse_path(d: str) -> list[tuple[float, float]]:
    nums = [float(x) for x in _NUM.findall(d)]
    return list(zip(nums[0::2], nums[1::2]))


_PENROSE_POLYS = [_parse_path(d) for d in _PENROSE_PATHS]
_P_XS = [p[0] for poly in _PENROSE_POLYS for p in poly]
_P_YS = [p[1] for poly in _PENROSE_POLYS for p in poly]
_P_MINX, _P_MAXX = min(_P_XS), max(_P_XS)
_P_MINY, _P_MAXY = min(_P_YS), max(_P_YS)
_P_BW = _P_MAXX - _P_MINX
_P_BH = _P_MAXY - _P_MINY
_P_CX = _P_MINX + _P_BW / 2
_P_CY = _P_MINY + _P_BH / 2
_P_SRC_R = max(math.hypot(x - _P_CX, y - _P_CY) for x, y in zip(_P_XS, _P_YS))

# Zen screensaver particles: (angle_deg, radius_frac, size_frac, color, alpha)
# Kept on/near the outer orbits (outside the Penrose).
_PARTICLES: tuple[tuple[float, float, float, tuple[int, int, int], int], ...] = (
    (18, 0.85, 0.020, TEAL, 150),
    (55, 0.78, 0.014, PENROSE_LAVENDER, 170),
    (110, 0.90, 0.017, PENROSE_PINK, 140),
    (155, 0.80, 0.012, TEAL, 120),
    (200, 0.88, 0.016, PENROSE_LAVENDER, 160),
    (245, 0.76, 0.013, PENROSE_PINK, 130),
    (295, 0.91, 0.018, TEAL, 110),
    (330, 0.79, 0.011, PENROSE_LAVENDER, 150),
    (75, 0.86, 0.010, TEAL, 110),
    (260, 0.84, 0.009, PENROSE_PINK, 100),
)

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

# Website raster sizes (png + webp when supported)
WEBSITE_MARK_SIZES = (48, 64, 96, 128, 180, 192, 256, 512, 1024, 2048)
WEBSITE_ROUND_SIZES = (256, 512, 1024, 2048)
WEBSITE_TRANSPARENT_SIZES = (256, 512, 1024, 2048)


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


def _penrose_transform(
    *,
    cx: float,
    cy: float,
    radius: float,
) -> tuple[float, float, float]:
    """Scale/offset so the Penrose is fully inscribed in a circle of the given radius."""
    scale = radius / _P_SRC_R
    return scale, cx - _P_CX * scale, cy - _P_CY * scale


def _draw_penrose_inscribed(
    draw: ImageDraw.ImageDraw,
    *,
    cx: float,
    cy: float,
    radius: float,
) -> None:
    """Draw Penrose fully inside the circle centered at (cx, cy)."""
    scale, ox, oy = _penrose_transform(cx=cx, cy=cy, radius=radius)
    for pts, color in zip(_PENROSE_POLYS, _PENROSE_COLORS):
        mapped = [(ox + x * scale, oy + y * scale) for x, y in pts]
        draw.polygon(mapped, fill=color)


def _draw_penrose(
    draw: ImageDraw.ImageDraw,
    *,
    left: float,
    top: float,
    width: float,
    height: float,
) -> None:
    """Fit the Penrose into a box by inscribing it in the box's inscribed circle."""
    cx = left + width / 2
    cy = top + height / 2
    radius = min(width, height) / 2 * PENROSE_RADIUS_FRAC
    _draw_penrose_inscribed(draw, cx=cx, cy=cy, radius=radius)


def _draw_zen_ambient(base: Image.Image, *, cx: float, cy: float, circle_r: float) -> Image.Image:
    """Soft orbits, arcs and particles — screensaver / ambient-art cues inside the circle."""
    if base.mode != "RGBA":
        layer_base = base.convert("RGBA")
    else:
        layer_base = base
    size = layer_base.size[0]
    ambient = Image.new("RGBA", layer_base.size, (0, 0, 0, 0))
    d = ImageDraw.Draw(ambient)

    # Soft aura discs (calm field behind the mark)
    for rr, col, a in (
        (0.93, (70, 45, 110), 38),
        (0.80, (55, 70, 120), 26),
        (0.66, (90, 70, 140), 18),
    ):
        r = circle_r * rr
        d.ellipse((cx - r, cy - r, cx + r, cy + r), fill=(*col, a))

    composed = Image.alpha_composite(layer_base, ambient)

    # Thin orbit rings — outside the Penrose, inside the circle
    rings = Image.new("RGBA", layer_base.size, (0, 0, 0, 0))
    rd = ImageDraw.Draw(rings)
    stroke = max(1, round(size / 220))
    ring_cols = (
        (ORBIT_RADII[0], PENROSE_PINK, 60),
        (ORBIT_RADII[1], PENROSE_LAVENDER, 90),
        (ORBIT_RADII[2], TEAL, 75),
    )
    for rr, col, a in ring_cols:
        r = circle_r * min(rr, AMBIENT_MAX_RADIUS_FRAC)
        rd.ellipse((cx - r, cy - r, cx + r, cy + r), outline=(*col, a), width=stroke)
    composed = Image.alpha_composite(composed, rings)

    # Partial arcs (motion / screensaver feel) on outer orbits
    arcs = Image.new("RGBA", layer_base.size, (0, 0, 0, 0))
    ad = ImageDraw.Draw(arcs)
    arc_w = max(1, round(size / 180))
    for start, end, rr, col, a in (
        (200, 320, ORBIT_RADII[2], TEAL, 95),
        (-20, 80, ORBIT_RADII[2], PENROSE_LAVENDER, 85),
        (100, 165, ORBIT_RADII[0], PENROSE_PINK, 70),
    ):
        r = circle_r * min(rr, AMBIENT_MAX_RADIUS_FRAC)
        ad.arc((cx - r, cy - r, cx + r, cy + r), start=start, end=end, fill=(*col, a), width=arc_w)
    composed = Image.alpha_composite(composed, arcs)

    # Floating particles
    parts = Image.new("RGBA", layer_base.size, (0, 0, 0, 0))
    pd = ImageDraw.Draw(parts)
    for ang, rf, sf, col, a in _PARTICLES:
        rad = math.radians(ang)
        x = cx + math.cos(rad) * circle_r * rf
        y = cy + math.sin(rad) * circle_r * rf
        s = size * sf
        pd.ellipse((x - s, y - s, x + s, y + s), fill=(*col, a))
    return Image.alpha_composite(composed, parts)


def render_icon(size: int, *, ambient: bool = True) -> Image.Image:
    """Square launcher icon — Penrose + zen accents fully inside the circular mask."""
    cx = cy = size / 2
    circle_r = size / 2 - 1
    im = Image.new("RGBA", (size, size), (*VIOLET, 255))
    if ambient:
        im = _draw_zen_ambient(im, cx=cx, cy=cy, circle_r=circle_r)
    draw = ImageDraw.Draw(im)
    _draw_penrose_inscribed(
        draw,
        cx=cx,
        cy=cy,
        radius=circle_r * PENROSE_RADIUS_FRAC,
    )
    return im.convert("RGB")


def render_icon_rgba(size: int, *, background: bool = True, ambient: bool = True) -> Image.Image:
    """RGBA variant for website transparent / round exports."""
    cx = cy = size / 2
    circle_r = size / 2 - 1
    if background:
        im = Image.new("RGBA", (size, size), (*VIOLET, 255))
    else:
        im = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    if ambient:
        im = _draw_zen_ambient(im, cx=cx, cy=cy, circle_r=circle_r)
    draw = ImageDraw.Draw(im)
    _draw_penrose_inscribed(
        draw,
        cx=cx,
        cy=cy,
        radius=circle_r * PENROSE_RADIUS_FRAC,
    )
    return im


def render_round_icon(size: int) -> Image.Image:
    base = render_icon(size)
    mask = Image.new("L", (size, size), 0)
    ImageDraw.Draw(mask).ellipse((0, 0, size - 1, size - 1), fill=255)
    circle = Image.new("RGB", (size, size), VIOLET)
    circle.paste(base, mask=mask)
    return circle


def render_banner_hires(width: int, height: int, *, title: str = APP_NAME) -> Image.Image:
    """16:9 home-screen banner — violet orbs, inscribed Penrose mark, app name (TV-BN)."""
    im = Image.new("RGB", (width, height), VIOLET)
    draw = ImageDraw.Draw(im)

    for cx, cy, r, color in (
        (int(width * 0.20), int(height * 0.48), int(height * 0.58), VIOLET_ORB_A),
        (int(width * 0.72), int(height * 0.38), int(height * 0.52), VIOLET_ORB_B),
        (int(width * 0.52), int(height * 0.88), int(height * 0.42), VIOLET_ORB_C),
    ):
        draw.ellipse((cx - r, cy - r, cx + r, cy + r), fill=color)

    logo_size = height * 0.78
    logo_left = width * 0.06
    logo_top = (height - logo_size) / 2
    # Small zen disc behind the mark on banners
    mark_cx = logo_left + logo_size / 2
    mark_cy = logo_top + logo_size / 2
    mark_r = logo_size / 2
    rgba = im.convert("RGBA")
    rgba = _draw_zen_ambient(rgba, cx=mark_cx, cy=mark_cy, circle_r=mark_r)
    im = rgba.convert("RGB")
    draw = ImageDraw.Draw(im)
    _draw_penrose(draw, left=logo_left, top=logo_top, width=logo_size, height=logo_size)

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
    if im.mode == "RGBA":
        im.save(path, format="PNG", optimize=True)
    else:
        rgb = im.convert("RGB")
        rgb.save(path, format="PNG", optimize=True)
        im = rgb
    print(f"  wrote {path.relative_to(ROOT)} ({im.size[0]}×{im.size[1]})")


def write_webp(path: Path, im: Image.Image) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    try:
        im.save(path, format="WEBP", quality=90, method=6)
        print(f"  wrote {path.relative_to(ROOT)} ({im.size[0]}×{im.size[1]})")
    except Exception as exc:  # noqa: BLE001 — optional webp codec
        print(f"  skip webp {path.name}: {exc}")


def write_website_icons() -> None:
    """Refresh website/assets/icon raster exports from the same Penrose mark."""
    if not WEBSITE_ICON.is_dir():
        return
    png_dir = WEBSITE_ICON / "png"
    webp_dir = WEBSITE_ICON / "webp"
    png_dir.mkdir(parents=True, exist_ok=True)
    webp_dir.mkdir(parents=True, exist_ok=True)

    for size in WEBSITE_MARK_SIZES:
        im = render_icon(size)
        write_png(png_dir / f"arthur-icon-{size}.png", im)
        write_webp(webp_dir / f"arthur-icon-{size}.webp", im)
        if size in (256, 512, 1024):
            write_png(png_dir / f"arthur-icon-mark-{size}.png", im)
            write_webp(webp_dir / f"arthur-icon-mark-{size}.webp", im)

    for size in WEBSITE_ROUND_SIZES:
        im = render_round_icon(size)
        write_png(png_dir / f"arthur-icon-round-{size}.png", im)
        write_webp(webp_dir / f"arthur-icon-round-{size}.webp", im)

    for size in WEBSITE_TRANSPARENT_SIZES:
        im = render_icon_rgba(size, background=False)
        write_png(png_dir / f"arthur-icon-transparent-{size}.png", im)
        write_webp(webp_dir / f"arthur-icon-transparent-{size}.webp", im)

    # Apple touch + OG
    write_png(WEBSITE_ICON / "apple-touch-icon.png", render_icon(180))
    og = Image.new("RGBA", (1200, 630), (*VIOLET, 255))
    mark = 420
    mx, my = (1200 - mark) / 2, (630 - mark) / 2
    og = _draw_zen_ambient(og, cx=mx + mark / 2, cy=my + mark / 2, circle_r=mark / 2)
    _draw_penrose(ImageDraw.Draw(og), left=mx, top=my, width=mark, height=mark)
    og_rgb = og.convert("RGB")
    write_png(png_dir / "arthur-icon-og-1200x630.png", og_rgb)
    write_webp(webp_dir / "arthur-icon-og-1200x630.webp", og_rgb)

    # favicon.ico (multi-size)
    try:
        icos = [render_icon(s) for s in (16, 32, 48)]
        icos[-1].save(WEBSITE_ICON / "favicon.ico", format="ICO", append_images=icos[:-1])
        print(f"  wrote {(WEBSITE_ICON / 'favicon.ico').relative_to(ROOT)}")
    except Exception as exc:  # noqa: BLE001
        print(f"  skip favicon.ico: {exc}")


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
    parser.add_argument(
        "--skip-website",
        action="store_true",
        help="Skip website/assets/icon raster refresh",
    )
    args = parser.parse_args()

    print("Generating TV launcher assets (Penrose triangle on violet)…")

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

    if not args.skip_website:
        print("Refreshing website icon rasters…")
        write_website_icons()

    vector = RES / "drawable/ic_banner.xml"
    if vector.exists():
        vector.unlink()
        print(f"  removed {vector.relative_to(ROOT)} (use drawable-xhdpi PNG)")

    print("Done.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

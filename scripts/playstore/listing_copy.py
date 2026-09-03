"""Canonical English Play Store listing copy for Arthur (source for DeepL)."""

from __future__ import annotations

from typing import TypedDict


class ListingCopy(TypedDict):
    title: str
    short_description: str
    full_description: str


APP_TITLE = "Arthur"

SOURCE_LANG = "en"

SOURCE_COPY: ListingCopy = {
    "title": APP_TITLE,
    "short_description": "Ambient art for phone, Auto & TV",
    "full_description": """Arthur (ART'hur) turns your phone into a Control Plane for ambient art on Android Auto and Android TV.

Browse museum works, generative fractals, and — with Premium — your own photos. Prepare a rotation on your phone, then enjoy it on car and living-room canvases.

Free tier includes a curated pack, Fractal Presets, and Auto/TV canvases with a limited pool. Premium unlocks Personal Photos, full Remote Sources, unlimited Genart, and Custom Fractal authoring.

Privacy: https://arthur.geoking.fr/privacy.html
Learn more: https://arthur.geoking.fr
""",
}

LISTING_COPY: dict[str, ListingCopy] = {SOURCE_LANG: SOURCE_COPY}

# Street-art Remote Sources

Backlog for street-art / mural connectors behind the shared `Source` interface. Distinct from museum connectors ([`roadmap-museum-sources.md`](roadmap-museum-sources.md)) and stock photos (Pexels / Unsplash). Wikimedia here is **street-art only** — not the famous-art connector ([ADR 0008](adr/0008-rijksmuseum-then-met.md)).

**Goal:** geotagged or curated street art with **downloadable images**, clear attribution, no HTML scraping. Prefer **no API key**. License bar is harder than museums: many mural photos are All Rights Reserved; prefer CC0 / Public Domain / CC BY / CC BY-SA (no NC/ND) for Play distribution.

## Shipped

| Source | `sourceId` | API | Key | Notes |
|--------|------------|-----|-----|--------|
| Wikimedia Commons Street Art | `wikimedia-streetart` | [MediaWiki API](https://commons.wikimedia.org/w/api.php) `generator=categorymembers` on `Category:Street_art` | none | Open licenses only; thumbnails via `iiurlwidth` |

Also available as a **Photo topic** for stock providers: `StockPhotoCategory.StreetArt` (`streetart`) → Pexels `"street art mural"` / Unsplash `"street art mural graffiti"`.

## Backlog

Status: `idea` until a `Source` ships (then move to **Shipped**).

### Prefer next (images + free access)

- **Street Art Cities (city markers JSON)** — `https://streetartcities.com/data/cities/{city}/markers.json` returns titles, artists, and `images[].sizes` with no key. Large worldwide coverage. **Caveat:** their [Open Data](https://streetartcities.com/open-data) exports deliberately **omit hunter photos**; image reuse needs ToS / consent review before Play. Treat as unofficial until cleared.
- **Dublin Canvas (GeoJSON)** — [data.gov.ie](https://data.gov.ie/dataset/dublin-canvas-public-art) CC-BY traffic-box murals (~900). Metadata + profile links; **no direct image URLs** in the dataset (would need a documented image host or curated subset).
- **Municipal mural registries** — e.g. [Chicago Mural Registry](https://data.cityofchicago.org/Historic-Preservation/Mural-Registry/we8h-apcf), Jersey City mural map: open metadata / coordinates, usually **no images**. Useful later if paired with a licensed photo feed.

### Weak / blocked

- **Street Art Aberdeen** — documented open JSON (`art_api.json`) with images and no key, but the live site is a static archive; API endpoints **404**. Revisit if the project comes back.
- **Street Art Cities monthly CSV/JSON** — official open data (artist, lat/lng, metadata) **without images** — not enough for ambient stills alone.
- **mural.place** — community map; no documented public API yet.
- **PublicArt.io** — historical geotagged graffiti project; API / site unreliable.

### Explicitly out of scope (for now)

- Scraping Street Art Cities / Instagram / closed mural apps for images.
- Shipping All-Rights-Reserved mural photos without a license path.
- Treating Wikimedia as a general museum / famous-art Source (ADR 0008).

## Implementation checklist (when shipping one)

1. `shared` Source: inject `httpGet`, fixture unit tests, license + image-required filter.
2. Register in `ArthurApp` `ContentEngine` sources list.
3. Folder title + order in `ArthurMediaBrowse`.
4. Add `MuseumTopic` (or a dedicated street-art topic) + pack cover so Control Plane lists the Source.
5. Mention in `CONTEXT.md` Remote Source / privacy list.
6. Use a descriptive `User-Agent` when the provider requires it (Wikimedia).

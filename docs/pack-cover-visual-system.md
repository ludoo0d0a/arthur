# Pack cover visual system

Canonical look for Control Plane pack / sub-pack tiles (`R.drawable.pack_*`).
Covers are **museum-exhibit album art**: one centered motif under a soft halo on a dark, monochromatic field — not photoreal photos, not Material icons.

Wiring: [`PackCovers.kt`](../androidApp/src/main/kotlin/fr/geoking/arthur/ui/components/PackCovers.kt).
UI: rounded 8 dp tiles in [`PackGrid.kt`](../androidApp/src/main/kotlin/fr/geoking/arthur/ui/components/PackGrid.kt).

---

## Definition (style contract)

| Rule | Spec |
|------|------|
| Format | Square **1:1**, delivered as **512×512 WebP** (`cwebp -q 82`) |
| Subject | One clear motif, **centered**, readable at ~72–120 dp |
| Perspective | Straight-on / elevation, or simple layered silhouette — no dramatic 3D camera |
| Abstraction | Flat–soft illustration / paper-cut layers; **not** photorealistic |
| Text | **None** (no titles, watermarks, captions). Institution/provider marks may include their lettermark only |
| People | Avoid faces / figures unless the family motif requires it (prefer abstract sculpture) |
| Atmosphere | Dark vignette corners + **circular glow halo** behind the subject |
| Grounding | Soft undulating base shapes (dunes / waves / fabric) under the subject |
| Grain | Light fine grain / matte paper feel across the frame |

**Prompt skeleton** (for regenerating covers):

```text
Square 1:1 album-art style pack cover for a <Family/Topic> category,
same museum-exhibit treatment as Arthur pack covers: soft illustrative shading,
centered subject, dark textured background with a soft circular glow halo behind
the subject, subtle fine grain, soft undulating ground shapes at the bottom,
no text, no logos, no watermark, no people.
Subject: <motif>.
Color palette ONLY: deep background <BG>, mid <MID> for body, soft <HI> for
highlights and glow. Soft subtle gradient atmosphere matching those three colors.
Clean Spotify-playlist-cover aesthetic, slightly abstract, not photorealistic.
```

Reference images when regenerating: `pack_museum.webp` and the family root cover
(`pack_photo.webp`, `pack_video.webp`, …).

---

## Lighting, shadows, effects

| Effect | How it is used |
|--------|----------------|
| **Halo / backlight** | Large soft radial disk behind the motif; separates subject from bg |
| **Vignette** | Corners darker than center; keeps focus on the emblem |
| **Paper-cut depth** | Overlapping flat layers with thin contact shadows / ambient occlusion at joins |
| **Bevel / edge highlight** | Lighter rim on top-left or top edges of main shapes |
| **Specular accent** | Small bright spot on glass/lens/metal (camera, clapper, water) |
| **Drop shadow** | Soft, short shadow under the whole subject onto the ground shapes |
| **Gradient fill** | Bodies slightly lighter toward the light; recesses use the deepest stop |
| **Grain / texture** | Subtle noise so the cover feels matte, not glossy UI chrome |
| **No hard outlines** | Forms defined by value contrast between palette stops, not black strokes |

Institution / provider logo variants (Met, Rijks, Pexels, …) use the **same**
halo + grain + vignette treatment, usually **gold-on-dark** (see Museum row).

---

## Family palettes (3 stops)

Each cover is **strictly monochromatic**: only tint/shade of one hue.
Do not mix hues on one cover. Top-level packs use a **family** hue;
stock keyword topics use a **topic** hue so tiles are easy to tell apart.

### Top-level packs

| Family | Background | Mid | Highlight | Motif examples |
|--------|------------|-----|-----------|----------------|
| **Museum** | `#2C1810` | `#8B5A2B` | `#D4A574` | Classical facade / columns; gold lettermarks for institutions |
| **Sculpture** | `#1C1C1C` | `#5A5A5A` | `#C0C0C0` | Abstract stone form / pedestal |
| **Painting** | `#3D1A1A` | `#8B3A3A` | `#E8A87C` | Ornate frame + easel + canvas |
| **Photo** | `#0D2137` | `#2E86AB` | `#A8DADC` | Camera (Random / root only) |
| **Video** | `#042A2E` | `#1A7A78` | `#A8E6E0` | Clapperboard / play slate |
| **Genart** | `#1A0A2E` | `#6B3FA0` | `#E8B4F8` | Generative curves / fractal-like shapes |

### Stock keyword topics (`StockPhotoCategory`)

Primary color is **semantic to the topic** (not the Photo family blue).

| Topic | Hue idea | Background | Mid | Highlight |
|-------|----------|------------|-----|-----------|
| **Nature** | Forest green | `#0A2F1F` | `#2D6A4F` | `#A8E6C0` |
| **Mountains** | Cool slate | `#1A1F2E` | `#5B6B8A` | `#D7DEEA` |
| **Ocean** | Sea blue | `#021F3D` | `#1B6CA8` | `#9AD7F0` |
| **City** | Urban amber | `#1A1008` | `#C47A2C` | `#F0D08A` |
| **Sky** | Dawn indigo | `#1B1540` | `#6B7FD7` | `#D6E0FF` |
| **Abstract** | Magenta | `#2A0A2E` | `#B03A8C` | `#F2B4E0` |
| **Architecture** | Warm stone | `#2A2218` | `#A89070` | `#EDE4D4` |
| **StreetArt** | Coral graffiti | `#2A0C12` | `#E0455A` | `#FFC2C8` |
| **Random** | Warm chance amber (shared across every family) | `#1A1408` | `#A67C3D` | `#F0D9A0` |

### Genart topics (`GenartTopic`)

Primary color is **semantic to the topic** (not always Genart purple).
Random uses shared `pack_random`.

| Topic | Hue idea | Background | Mid | Highlight |
|-------|----------|------------|-----|-----------|
| **Tapet** | Soft lilac | `#1E0F2E` | `#8B6BB0` | `#E8D4F8` |
| **Nature** | Forest green | `#0A2F1F` | `#2D6A4F` | `#A8E6C0` |
| **Weather** | Storm slate | `#0E1A28` | `#4A6B8A` | `#B8D4F0` |
| **Water** | Deep teal | `#021F2E` | `#1A7A8A` | `#A8E8F0` |
| **Life** | Soft bio green | `#0F1F12` | `#3D7A4A` | `#B8E8C0` |
| **Earth** | Terracotta | `#1A1008` | `#8B5A2B` | `#E8C8A0` |
| **Planets** | Cosmic indigo | `#0A0A20` | `#4A4A9A` | `#C8C8F8` |
| **SciFi** | Cyan | `#0A1A22` | `#2A8A9A` | `#A8F0F8` |
| **Abstract** | Magenta | `#2A0A2E` | `#B03A8C` | `#F2B4E0` |
| **Geometry** | Cool steel | `#12161C` | `#5A6A7A` | `#D0D8E0` |
| **Fractal** | Genart purple | `#1A0A2E` | `#6B3FA0` | `#E8B4F8` |
| **Custom** | Warm violet | `#1A0A1E` | `#7A4A9A` | `#E8C8F0` |

Optional fourth stop for recessed voids: near-black of the **same** hue.

---

## Asset map

### Top-level packs (`PackFamily`)

| Pack | Drawable | Palette |
|------|----------|---------|
| Museum | `pack_museum.webp` | Museum browns |
| Sculpture | `pack_sculpture.webp` | Sculpture grays |
| Painting | `pack_painting.webp` | Painting burgundy |
| Photo | `pack_photo.webp` | Photo blues |
| Video | `pack_video.webp` | Video teal |
| Genart | `pack_genart.webp` | Genart purples |

### Photo / Video keyword topics (`StockPhotoCategory`)

Shared via `PackCovers.photo()` (Photo and Video keyword tiles).
Each topic has its **own primary hue** (see table above).
**Random** is one shared asset (`PackCovers.random` / `pack_random.webp`) for every
family’s Random tile (Museum, Sculpture, Painting, Photo, Video, Genart).

| Topic | Drawable | Motif | Primary hue |
|-------|----------|--------|-------------|
| Random | `pack_random.webp` | Die / chance cube | Warm chance amber |
| Nature | `pack_photo_nature.webp` | Tree / foliage / hills | Forest green |
| Mountains | `pack_photo_mountains.webp` | Layered peaks + snow tip | Cool slate |
| Ocean | `pack_photo_ocean.webp` | Waves + horizon disk | Sea blue |
| City | `pack_photo_city.webp` | Skyline | Urban amber |
| Sky | `pack_photo_sky.webp` | Clouds + sun/moon disk | Dawn indigo |
| Abstract | `pack_photo_abstract.webp` | Geometric arcs / disks | Magenta |
| Architecture | `pack_photo_architecture.webp` | Columns / arch fragment | Warm stone |
| StreetArt | `pack_photo_streetart.webp` | Wall + spray swirls (no lettering) | Coral graffiti |

### Genart topics (`GenartTopic`)

Wired via `PackCovers.genart()`. Each topic has its **own primary hue** (see table above).

| Topic | Drawable | Motif | Primary hue |
|-------|----------|--------|-------------|
| Random | `pack_random.webp` | Die / chance cube | Warm chance amber |
| Tapet | `pack_genart_tapet.webp` | Silk diamond weave / wallpaper panels | Soft lilac |
| Nature | `pack_genart_nature.webp` | Layered tree | Forest green |
| Weather | `pack_genart_weather.webp` | Storm cloud + rain + lightning | Storm slate |
| Water | `pack_genart_water.webp` | Waves + horizon disk | Deep teal |
| Life | `pack_genart_life.webp` | Sprout / seedling | Soft bio green |
| Earth | `pack_genart_earth.webp` | Stylized globe | Terracotta |
| Planets | `pack_genart_planets.webp` | Ringed planet + moons | Cosmic indigo |
| SciFi | `pack_genart_scifi.webp` | Geometric probe / ship | Cyan |
| Abstract | `pack_genart_abstract.webp` | Soft arcs / disks | Magenta |
| Geometry | `pack_genart_geometry.webp` | Platonic solid / crystal | Cool steel |
| Fractal | `pack_genart_fractal.webp` | Recursive spiral | Genart purple |
| Custom | `pack_genart_custom.webp` | Faceted gem + dial ring | Warm violet |

### Other covers

- Museum institutions: `pack_met`, `pack_rijksmuseum`, … (gold-on-dark logo variants)
- Video / photo providers: `pack_pexels`, `pack_unsplash`, `pack_pixabay`, `pack_coverr`, `pack_deviantart`
- Shared Random: `pack_random` (dice motif; wired via `PackCovers.random`)

---

## Delivery pipeline

1. Generate PNG 1:1 (prompt + references above).
2. `sips -z 512 512 source.png --out /tmp/name_512.png`
3. `cwebp -q 82 /tmp/name_512.png -o androidApp/src/main/res/drawable/name.webp`
4. Remove any conflicting `name.xml` vector so the WebP wins.
5. Point `PackCovers` at `R.drawable.name` (resource name = file basename).

Do not commit oversized source PNGs into `res/`; keep masters under the Cursor
assets cache or a design folder outside the APK.

---

## Checklist for a new cover

- [ ] Motif reads at thumbnail size
- [ ] Only the three family hex stops (+ darker recess)
- [ ] Halo + vignette + soft ground shapes present
- [ ] No text / logos (unless institution lettermark)
- [ ] 512² WebP in `drawable/`, wired in `PackCovers`
- [ ] Same style contract as siblings; **topic hue** for stock keywords (not Photo blues)

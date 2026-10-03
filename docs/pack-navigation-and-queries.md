# Pack Navigation Flows, Queries, and Caching Architecture

This document details the navigation flows, screen-by-screen API triggers,
endpoints, query parameters, sampling strategies, and caching for Ambient Art
Packs in Arthur.

Canonical pack tree: [navigation-structure.md](navigation-structure.md).

---

## 1. Pack System Overview & Architecture

Arthur organizes ambient content into Spotify-style **Pack Families** on the Control Plane grid. Each family contains **Sub-Packs** (specific topics, institutions, or keywords).

There is no `NavHost` — screen switching is a plain state machine
(`remember { mutableStateOf(...) }`) in
[MainActivity.kt](../androidApp/src/main/kotlin/fr/geoking/arthur/phone/MainActivity.kt)
and
[ControlPlaneScreen.kt](../androidApp/src/main/kotlin/fr/geoking/arthur/ui/screens/ControlPlaneScreen.kt).
Every network call funnels through one facade, `ContentEngine.catalog(PreparedRotation)`
([ContentEngine.kt](../shared/src/commonMain/kotlin/fr/geoking/arthur/shared/engine/ContentEngine.kt)),
which fans out to `Source.load()` for each provider whose id is in
`PreparedRotation.sourceIds`. **Empty `sourceIds` means every registered source** —
Ambient Start and Dream must never pass an empty list as a “reload everything”
fallback; they always use `PackSelection.sourceIdsForAmbientLoad()`.

All calls are plain HTTP `GET`, JSON, over a shared OkHttp client
([ArthurApp.kt](../androidApp/src/main/kotlin/fr/geoking/arthur/ArthurApp.kt)).

### Top-Level Pack Families (`PackFamily`)

| Pack Family | Identifier | Target Artwork Kind | Primary Sources |
|---|---|---|---|
| **Museum** | `museum` | `Painting`, `Sculpture` | Random across **all** museums (incl. Louvre), or one institution |
| **Painting** | `painting` | `Painting` | Random across museums **except Louvre**, or one institution |
| **Sculpture** | `sculpture` | `Sculpture` | Same as Painting (Random without Louvre) |
| **Photo** | `photo` | `Photo` | Pexels, Unsplash, DeviantArt, Wikimedia (+ keyword topics across those four) |
| **Video** | `video` | `Video` | Pexels Video, Unsplash Video, Pixabay Video, Coverr |
| **Genart** | `genart` | `Genart`, `FractalPreset`, `CustomFractal` | In-memory procedural engines (topic packs via Marketplace except All/Random free subset) |
| **Personal** | `personal` | `PersonalPhoto` | On-device Personal Photos (Marketplace pack) |

Marketplace browse/buy: phone + TV only (not Auto). `PackOwnership` gates Content Engine — Premium does not unlock packs.

`BundledPackSource` remains for previews / Android Auto demos — it is **never**
injected as an Ambient stills fallback when a pack pool is empty.

---

## 2. Navigation Tree

```
MainActivity
 └── ControlPlaneScreen
      ├── Home grid (no family opened): 6 tiles
      │    Museum · Genart · Photo · Video · Sculpture · Painting
      │
      ├── Sub-pack grid (family opened)
      │    Museum    -> Random + 8 institutions (Met … Louvre); default open = Met
      │    Painting  -> Random + museums without Louvre
      │    Sculpture -> Random + museums without Louvre
      │    Photo     -> 4 providers (Pexels, Unsplash, DeviantArt, Wikimedia)
      │                 + keyword topics (Random, Nature, City, …)
      │    Video     -> 4 providers + keyword topics
      │    Genart    -> All + Random + categories + Custom (local only)
      │
      │    Tapping an already-selected tile (or TV OK) -> Start Ambient
      │
      ├── Genart > Custom -> "Create custom fractal" -> CustomFractalEditorScreen
      │
      └── Start Ambient -> AmbientActivity
           └── AmbientScreenContent (fullscreen rotation)
                └── "..." details -> ArtworkDetailScreen
```

Opening a family sets `PackSelection(family, family.defaultSubId())`:
Genart → `all`, Museum → `met`, others → `random`.

---

## 3. Screen-by-Screen API Calls

### 3.1 ControlPlaneScreen — Home / sub-pack

File: [ControlPlaneScreen.kt](../androidApp/src/main/kotlin/fr/geoking/arthur/ui/screens/ControlPlaneScreen.kt)

- Home composition may warm Genart / Fractal sources for tile counts; it does
  **not** prefetch every remote provider.
- **On every sub-pack selection**: `selection.sourceIdsForAmbientLoad()` (in
  [PackModels.kt](../androidApp/src/main/kotlin/fr/geoking/arthur/ui/components/PackModels.kt))
  resolves provider id(s), then prefetches
  `ContentEngine.catalog(PreparedRotation(sourceIds = renewIds))` and stores the
  result in `packCatalogCache`.
- **On Start Ambient**: reuses prefetch / `ArtworkImageCache.loadCachedArtworks(renewIds)`
  for the **same** selection. If still empty:
  - Genart → in-memory catalog
  - other families → empty pool / `chosen = null` (placeholder — **no Bundled**)

| Family / sub-pack | Sources queried |
|---|---|
| Museum → Random | all museum institutions (**including Louvre**) |
| Museum → one institution | that single source |
| Museum → null / default | Met |
| Painting/Sculpture → Random | museum sources **without Louvre** |
| Painting/Sculpture → one institution | that single source |
| Photo → one provider | that single source |
| Photo → Random / keyword topic | `SourceCapabilities.sourceIdsForPhotoProviders()` (Pexels, Unsplash, DeviantArt, Wikimedia) — **not** museums |
| Video → Random / keyword | all video remote-search sources |
| Video → one provider | that single source |
| Genart → any | **no network** (`sourceIds = null`) |

### 3.2 AmbientActivity → AmbientScreenContent

File: [AmbientActivity.kt](../androidApp/src/main/kotlin/fr/geoking/arthur/tv/AmbientActivity.kt),
[AmbientScreen.kt](../androidApp/src/main/kotlin/fr/geoking/arthur/ui/screens/AmbientScreen.kt)

- **Rotating / pinned**: use the Control Plane handoff pool only. Never call
  `ContentEngine.catalog(sourceIds = emptyList())` to invent another pack’s content.
- **Empty pool / null artwork**: `StillArtworkPlaceholder` with user-facing
  `artwork_unavailable` messages. Technical `errorDetail` only when Developer → Verbose.
- **Dream**: `loadDreamAmbient` loads `sourceIdsForAmbientLoad()` for the screensaver
  pack; empty selection pool → placeholder (Genart may still resolve from catalog).
- **On pool renew**: when within `POOL_RENEW_LEAD` (2) of the playlist end
  (~19th/20th of a `DEFAULT_LIMIT` page), `ContentEngine.catalog(sourceIds =
  renewSourceIds)` fetches the next sample and **appends** distinct ids to the
  current playlist (capped at `MAX_PLAYLIST_SIZE` = 60).
- **Per still**: `StillImagePrefetcher.ensureCached()` → disk image cache. Videos stream via ExoPlayer.

### 3.3 ArtworkDetailScreen / Custom fractal editor

No pack network calls. Custom fractal uses `CustomFractalStore` only.

---

## 4. Museum Pack Flows & Query Parameters

### 4.1 Random vs institution

- **Museum → Random**: every institution including Louvre; painting + sculpture kinds.
- **Painting / Sculpture → Random**: all institutions except Louvre; kind-filtered.
- **Louvre**: Museum pack only as a dedicated tile (curated ARK lists, no live search);
  also included in Museum → Random.

### 4.2 Museum source endpoints

#### The Met (`met`)
- `MetSource` — `GET …/v1.1/search` + `GET …/v1/objects/{id}`
- Params: `q`, `medium`, `hasImages=true`, `isPublicDomain=true`, `offset`, `limit`
- Variety: random `offset` window (no native sort) + sample

#### Harvard (`harvard`)
- `HarvardSource` — `GET https://api.harvardartmuseums.org/object`
- Params: `apikey`, `classification`, `hasimage`, `q=imagepermissionlevel:0`, `size`, `page`, `sort=random:SEED`
- Variety: native `sort=random:SEED` each load

#### Art Institute of Chicago (`artic`)
- `ArticSource` — search + IIIF image assembly
- Variety: Elasticsearch `function_score` + `random_score` seed each load

#### Cleveland (`cleveland`)
- `ClevelandSource` — `cc0=1`, `has_image=1`, `type`, `limit`, `skip`
- Variety: random deep `skip` (no native sort) + sample

#### Rijksmuseum (`rijksmuseum`)
- `RijksmuseumSource` — Linked Art search + object hydration
- Variety: explore `next.id` frontier, then random known page + sample

#### Smithsonian (`smithsonian`)
- `SmithsonianSource` — Solr `q` + `rows` / `start` + `sort=random` + `api_key`
- Variety: native `sort=random` each load

#### Europeana (`europeana`)
- `EuropeanaSource` — `query`, `theme`, `reusability=open`, `media`, `qf=TYPE:IMAGE`, `sort=random_SEED+asc`
- Variety: native seeded random sort each load

#### Louvre (`louvre`)
- `LouvreSource` — curated `PAINTING_ARKS` / `SCULPTURE_ARKS` → per-ARK JSON
- No public search API — sample curated list

---

## 5. Photo Pack Flows & Query Parameters

Photo providers only (no museum tiles under Photo).

### 5.1 Topics (`StockPhotoCategory`)

| Topic | Query id | Notes |
|---|---|---|
| Random | `random` | Each load picks a concrete remote topic |
| Nature / City / Ocean / Mountains / Abstract / Architecture / Sky / Street Art | matching `query` | Mapped per provider via `RemoteCategoryMapping.stockQuery` |

Provider tiles: Pexels, Unsplash, DeviantArt, Wikimedia (`wikimedia-streetart`).

### 5.2 Provider endpoints

#### Unsplash / Pexels / DeviantArt
Unchanged search/browse APIs (Client-ID / Authorization / OAuth tags). Blank keys
or failed live calls use **`offlineFallback` = same-provider disk cache**
(`ArtworkImageCache.loadCachedStock`), not Bundled.

#### Wikimedia (`wikimedia-streetart`)
- `WikimediaStreetArtSource` — Commons `categorymembers` on `Category:Street_art`
- Kind: `ArtworkKind.Photo`
- Open licenses only (PD / CC0 / CC BY / CC BY-SA)

---

## 6. Video & Genart Pack Flows

### 6.1 Video
Providers: Pexels Video, Unsplash (video), Pixabay Video, Coverr.
Keyword topics search across all video remotes. No still disk cache for streams.

### 6.2 Genart
`GenartSource`, `FractalSource`, `CustomFractalSource` — local only.
Sub-packs: All, Random, Tapet, Nature, Weather, Water, Life, Earth, Planets,
Sci‑Fi, Abstract, Geometry, Fractal, Custom.

---

## 7. Caching Behavior

1. **HTTP OkHttp cache** (`cacheDir/http_cache`, 50 MB) — `HttpCacheController`
   exposes size / clear (also used from Settings).
2. **Artwork image disk cache** — `ArtworkImageCache` at `cacheDir/artwork/*.img`
   + SharedPreferences catalog index:
   - Same-provider offline stock for blank keys / empty live results
   - Genart bake LRU: `MAX_GENART = 120`
   - Ambient prefetch via `StillImagePrefetcher`
   - **Settings → Image cache**: formatted size, entry count, Clear
     (`clearAll()` + HTTP `evictAll()`)
3. **In-memory** `packCatalogCache` on Control Plane for the composable lifetime
4. **Handoff** `AmbientRotationLaunch` avoids refetch when starting Ambient

---

## 8. Empty pool & no cross-pack fallback

| Situation | Behavior |
|---|---|
| Still pack prefetch + disk empty | Empty pool → Ambient placeholder + user message |
| Genart empty | May resolve from in-memory genart catalog (`allowsGenerativeAmbientFallback`) |
| Blank API key | `offlineFallback` → **cached** items for that provider/category only |
| Cache also empty | Empty list → placeholder (never Bundled, never another family’s content) |
| Developer Verbose | Extra technical `errorDetail` on the placeholder |

`PackSelection.allowsGenerativeAmbientFallback()` is `true` **only** for Genart.

Louvre curated ARKs are not a “fallback pack” — they are the Louvre connector’s
only data path (no search API).

`BundledPackSource` is for tests, screenshots, and Auto browse demos only.

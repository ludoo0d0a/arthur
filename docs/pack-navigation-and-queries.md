# Pack Navigation Flows, Queries, and Fallbacks Architecture

This document details the navigation flows, screen-by-screen API triggers,
endpoints, query parameters, sampling strategies, caching, and fallback
mechanisms for all Ambient Art Packs in Arthur.

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
`PreparedRotation.sourceIds` (empty list = every registered source). All calls
are plain HTTP `GET`, JSON, over a shared Ktor/OkHttp client
([ArthurApp.kt](../androidApp/src/main/kotlin/fr/geoking/arthur/ArthurApp.kt)) — there
is no POST/PUT/DELETE anywhere in the packs feature.

### Top-Level Pack Families (`PackFamily`)

| Pack Family | Identifier | Target Artwork Kind | Primary Sources |
|---|---|---|---|
| **Museum** | `museum` | `Painting`, `Sculpture` | Met, Rijksmuseum, Artic, Cleveland, Europeana, Harvard, Smithsonian, Louvre, Wikimedia Street Art |
| **Painting** | `painting` | `Painting` | Same museum sources filtered to Paintings |
| **Sculpture** | `sculpture` | `Sculpture` | Same museum sources filtered to Sculptures |
| **Photo** | `photo` | `Photo` | Unsplash, Pexels, DeviantArt, Met (Photo search), Bundled Suggestions |
| **Video** | `video` | `Video` | Pexels Video, Pixabay Video, Coverr |
| **Genart** | `genart` | `Genart`, `FractalPreset`, `CustomFractal` | In-memory Multiplatform procedural canvas engines |

---

## 2. Navigation Tree

```
MainActivity
 └── ControlPlaneScreen
      ├── Home grid (no family opened): 6 tiles
      │    Museum · Genart · Photo · Video · Sculpture · Painting
      │
      ├── Sub-pack grid (family opened): "All" + one tile per topic
      │    Museum    -> All + 9 institutions
      │    Painting  -> All + Suggestions + 9 institutions
      │    Sculpture -> All + Suggestions + 9 institutions
      │    Photo     -> All + Suggestions + 8 keyword topics
      │    Video     -> All + 4 providers + keyword topics
      │    Genart    -> All + 7 procedural topics (local only, no network)
      │
      │    Tapping an already-selected tile (or TV OK) -> Start Ambient
      │
      ├── Genart > Custom -> "Create custom fractal" -> CustomFractalEditorScreen
      │    (local-only fractal editor, no network calls)
      │
      └── Start Ambient -> AmbientActivity
           └── AmbientScreenContent (fullscreen rotation)
                └── "..." details button -> ArtworkDetailScreen
                     (no network calls; "Open source page" opens an
                     external browser Intent, not an in-app API call)
```

---

## 3. Screen-by-Screen API Calls

### 3.1 ControlPlaneScreen — Home grid

File: [ControlPlaneScreen.kt:151-179](../androidApp/src/main/kotlin/fr/geoking/arthur/ui/screens/ControlPlaneScreen.kt)

On first composition (`LaunchedEffect`), if no catalog was passed in, three
prefetch calls fire to warm the UI:

1. `ContentEngine.catalog(sourceIds = StockSourceIds)` — `bundled`, `pexels`, `unsplash`, `deviantart`
2. `ContentEngine.catalog(sourceIds = VideoSourceIds)` — `pexels-video`, `pixabay-video`, `coverr`
3. `ContentEngine.catalog(sourceIds = emptyList())` — every registered source (all 9 museum APIs + all stock/video APIs + bundled + genart)

So simply opening the home grid can, in the worst case, hit **every external
provider** listed in section 5. Tapping a family tile only changes local
state — no call.

### 3.2 ControlPlaneScreen — Sub-pack grid

Same file, [ControlPlaneScreen.kt:182-198](../androidApp/src/main/kotlin/fr/geoking/arthur/ui/screens/ControlPlaneScreen.kt):

- **On every sub-pack selection** (tapping a tile, or focusing one on TV):
  `selection.sourceIdsForAmbientLoad()` (in
  [PackModels.kt:285-310](../androidApp/src/main/kotlin/fr/geoking/arthur/ui/components/PackModels.kt))
  resolves the exact provider id(s) for that topic, then prefetches
  `ContentEngine.catalog(PreparedRotation(sourceIds = renewIds))` on
  `Dispatchers.IO` and caches the result in-memory
  (`packCatalogCache`, keyed by family+sub-pack+kind).
- **On Start Ambient** (tapping the already-selected tile / TV OK): reuses the
  cached result if present, otherwise re-issues the same `ContentEngine.catalog()`
  call, then hands the result to `AmbientActivity`.

Which sources fire per sub-pack:

| Family / sub-pack | Sources queried |
|---|---|
| Museum → All | Met, Rijksmuseum, Artic, Cleveland, Europeana, Harvard, Smithsonian, Louvre, Wikimedia Street Art |
| Museum → one institution | that single source |
| Painting/Sculpture → All | `bundled` + the 9 museum sources filtered to that kind |
| Painting/Sculpture → Suggestions | `bundled` only — **no network** |
| Painting/Sculpture → one institution | that single source |
| Photo → Suggestions | `bundled` only — **no network** |
| Photo → All/Random | `bundled` + Pexels + Unsplash + DeviantArt + Met |
| Photo → keyword topic (Nature, City, Ocean, Mountains, Abstract, Architecture, Sky, StreetArt) | Pexels + Unsplash + DeviantArt + Met |
| Video → All/keyword topic | Pexels Video + Pixabay Video + Coverr |
| Video → one provider tile | that single source |
| Genart → All/any topic (incl. Custom) | **no network** — in-memory procedural sources only |

### 3.3 AmbientActivity → AmbientScreenContent

File: [AmbientActivity.kt:62-103](../androidApp/src/main/kotlin/fr/geoking/arthur/tv/AmbientActivity.kt), [AmbientScreen.kt:162-180](../androidApp/src/main/kotlin/fr/geoking/arthur/ui/screens/AmbientScreen.kt)

- **On activity create**:
  - Rotating pool (from Control Plane): reuses the pool handed off in-process
    via `AmbientRotationLaunch`; only calls `ContentEngine.catalog(emptyList())`
    (every source) if that handoff is empty.
  - Pinned single artwork: always calls `ContentEngine.catalog(emptyList())`
    to refresh a live `remoteUrl` for that one artwork.
- **On pool exhaustion** (every artwork in the pool has been shown once):
  re-issues `ContentEngine.catalog(sourceIds = <same sources selected in Control Plane>)`
  to fetch a fresh random sample ("renew").
- **On each artwork display**: `StillImagePrefetcher.ensureCached()` does a
  plain GET of the artwork's `remoteUrl` (current + neighboring pool items),
  writing to a disk image cache. Video artworks skip this — ExoPlayer streams
  `remoteUrl` directly.
- Rotation timer advance and swipe/D-pad navigation are pure local index math
  — no network calls.

### 3.4 ArtworkDetailScreen

File: [ArtworkDetailScreen.kt](../androidApp/src/main/kotlin/fr/geoking/arthur/ui/screens/ArtworkDetailScreen.kt)

No network calls — all fields come from the already-loaded `Artwork` model.
"Open source page" opens the system browser via `Intent.ACTION_VIEW`, which is
not an in-app API call.

### 3.5 Genart → Custom → CustomFractalEditorScreen

`CustomFractalSource.load()` reads/writes purely local storage
(`CustomFractalStore`) — no network calls anywhere in this branch.

---

## 4. Museum Pack Flows & Query Parameters

When a user selects a Museum, Painting, or Sculpture pack, `sourceIdsForAmbientLoad()` determines which sources to query.

### 4.1 "All" & "Random" Flow Logic

- **`museum/all` / `painting/all` / `sculpture/all`**:
  - `sourceIdsForAmbientLoad()` returns all active museum source IDs (`MuseumSourceIds`).
  - `MuseumLoad.acrossTargets(kind, limit = 20, random)` expands `MuseumSearchKind.All` into concrete search targets: `[Painting, Sculpture]`.
  - **Query Distribution**: The limit (20) is split equally across targets: `perKind = 20 / 2 = 10`.
  - Up to 10 items are loaded for `Painting` and 10 for `Sculpture` per provider, merged, and then pseudo-randomly sampled down to the total requested batch limit (20).
- **`museum/random`**:
  - Resolves `MuseumTopic.Random` (all museum sources combined with random offset windowing across endpoints).

---

### 4.2 Museum Source Query Parameter Details

#### 1. The Met (`met`)
- **Source Class**: `MetSource`
- **Search Endpoint**: `GET https://collectionapi.metmuseum.org/public/collection/v1/search`
- **Object Endpoint**: `GET https://collectionapi.metmuseum.org/public/collection/v1/objects/{objectId}`
- **Query Parameters**:
  - `q`: Search keyword (`painting`, `sculpture`, `photograph`) derived from `RemoteCategoryMapping`.
  - `medium`: Medium facet filter (`Paintings`, `Sculpture`, `Photographs`).
  - `hasImages`: `true` (enforces presence of primary image).
  - `isPublicDomain`: `true` (filters open-access public domain items).
- **Example Query**:
  `https://collectionapi.metmuseum.org/public/collection/v1/search?q=painting&medium=Paintings&hasImages=true&isPublicDomain=true`
- **Execution Flow**: Search returns array of object IDs (`MetSearchPage`). Arthur samples `perKind` object IDs and hydrates each via `/objects/{objectId}`.

#### 2. Harvard Art Museums (`harvard`)
- **Source Class**: `HarvardSource`
- **Search Endpoint**: `GET https://api.harvardartmuseums.org/object`
- **Query Parameters**:
  - `apikey`: Secrets-based API key (`HARVARD_API_KEY`).
  - `classification`: Facet filter (`Paintings`, `Sculpture`, `Photographs`).
  - `hasimage`: `1` (requires media).
  - `q`: `imagepermissionlevel:0` (publicly viewable images only).
  - `size`: Page size pool limit (default `20`).
  - `page`: Randomly generated page index (1-based via `RemoteSample.randomPage()`).
  - `sort`: `random` (native Harvard API random order).
  - `fields`: Field projection (`id,title,primaryimageurl,people,classification,url,images`).
- **Example Query**:
  `https://api.harvardartmuseums.org/object?apikey={key}&classification=Paintings&hasimage=1&q=imagepermissionlevel%3A0&size=20&page=3&sort=random&fields=id,title,primaryimageurl,people,classification,url,images`

#### 3. Art Institute of Chicago (`artic`)
- **Source Class**: `ArticSource`
- **Search Endpoint**: `GET https://api.artic.edu/api/v1/artworks/search`
- **Query Parameters**:
  - `q`: Search query (`painting`, `sculpture`, `photograph`).
  - `query[term][is_public_domain]`: `true` (encoded as `query%5Bterm%5D%5Bis_public_domain%5D=true`).
  - `limit`: Results per window (default `20`).
  - `page`: Random page offset.
  - `fields`: `id,title,artist_display,image_id,is_public_domain,description,date_display,medium_display`.
- **IIIF Image URL Assembly**:
  `${iiifBase}/${imageId}/full/843,/0/default.jpg`
- **Example Query**:
  `https://api.artic.edu/api/v1/artworks/search?q=painting&query%5Bterm%5D%5Bis_public_domain%5D=true&limit=20&page=2&fields=id,title,artist_display,image_id,is_public_domain,description,date_display,medium_display`

#### 4. Cleveland Museum of Art (`cleveland`)
- **Source Class**: `ClevelandSource`
- **Search Endpoint**: `GET https://openaccess-api.clevelandart.org/api/artworks/`
- **Query Parameters**:
  - `cc0`: `1` (Creative Commons Zero filter).
  - `has_image`: `1` (image required).
  - `type`: Object classification (`Painting`, `Sculpture`, `Photograph`).
  - `limit`: Window size (`20`).
  - `skip`: Random offset (`RemoteSample.randomStart()`).
- **Example Query**:
  `https://openaccess-api.clevelandart.org/api/artworks/?cc0=1&has_image=1&limit=20&skip=40&type=Painting`

#### 5. Rijksmuseum (`rijksmuseum`)
- **Source Class**: `RijksmuseumSource`
- **Search Endpoint**: `GET https://data.rijksmuseum.nl/search/collection`
- **Query Parameters**:
  - `type`: Object classification (`painting`, `sculpture`, `photograph`).
  - `imageAvailable`: `true`.
- **Example Query**:
  `https://data.rijksmuseum.nl/search/collection?type=painting&imageAvailable=true`
- **Execution Flow**: Linked Art JSON-LD search response yields object URIs. Arthur fetches object JSON-LD files and extracts IIIF/DigitalObject `access_point` URLs (`shows` -> `VisualItem` -> `DigitalObject` -> `access_point`).

#### 6. Smithsonian Institution (`smithsonian`)
- **Source Class**: `SmithsonianSource`
- **Search Endpoint**: `GET https://api.si.edu/openaccess/api/v1.0/search`
- **Query Parameters**:
  - `q`: Solr clause e.g. `online_media_type:Images AND (object_type:Paintings OR object_type:Painting OR painting)`.
  - `rows`: Results count (`20`).
  - `start`: Random item offset.
  - `api_key`: Secrets-based API key (`SMITHSONIAN_API_KEY`).
- **Example Query**:
  `https://api.si.edu/openaccess/api/v1.0/search?q=online_media_type%3AImages%20AND%20(object_type%3APaintings%20OR%20object_type%3APainting%20OR%20painting)&rows=20&start=0&api_key={key}`

#### 7. Europeana (`europeana`)
- **Source Class**: `EuropeanaSource`
- **Search Endpoint**: `GET https://api.europeana.eu/record/v2/search.json`
- **Query Parameters**:
  - `query`: Free-text (`painting`, `sculpture`, `photograph`, or `*` for All).
  - `theme`: Category theme (`art`, `photography`).
  - `reusability`: `open` (openly licensed content).
  - `media`: `true`.
  - `qf`: `TYPE:IMAGE` (encoded as `TYPE%3AIMAGE`).
  - `rows`: Window count (`20`).
  - `start`: Item offset (1-based index).
  - `profile`: `standard`.
  - `wskey`: API key (`EUROPEANA_API_KEY`).
- **Example Query**:
  `https://api.europeana.eu/record/v2/search.json?query=painting&theme=art&reusability=open&media=true&qf=TYPE%3AIMAGE&rows=20&start=1&profile=standard&wskey={key}`
- **Header note**: `X-Api-Key` header is preferred over the `wskey` query param when both are wired up (see `ArthurApp.kt`).

#### 8. Musée du Louvre (`louvre`)
- **Source Class**: `LouvreSource`
- **Object Endpoint**: `GET https://collections.louvre.fr/ark:/53355/{arkId}.json`
- **Flow Details**: Louvre has no live search API. Arthur uses curated ARK lists (`PAINTING_ARKS`, `SCULPTURE_ARKS`) and fetches object JSON directly per ARK ID.

#### 9. Wikimedia Street Art (`wikimedia-streetart`)
- **Source Class**: `WikimediaStreetArtSource`
- **Search Endpoint**: `GET https://commons.wikimedia.org/w/api.php`
- **Query Parameters**:
  - `action`: `query`.
  - `generator`: `categorymembers`.
  - `gcmtitle`: `Category:Street_art`.
  - `gcmtype`: `file`.
  - `gcmlimit`: Window fetch size (`60`).
  - `prop`: `imageinfo`.
  - `iiprop`: `url|extmetadata|mime`.
  - `iiurlwidth`: `1600` (thumbnail width).
  - `format`: `json`.
  - `formatversion`: `2`.
- **Filtering**: Filters out non-open licenses (rejects `NC` and `ND` licenses; keeps `PD`, `CC0`, `CC BY`, `CC BY-SA`).
- **Header note**: sends a custom `User-Agent` (see `ArthurApp.kt`).

---

## 5. Photo Pack Flows & Query Parameters

Selecting the **Photo** pack family routes search across stock photo providers, community platforms, museum photography collections, and bundled local packs.

### 5.1 Photo Topics & Query Mapping (`StockPhotoCategory`)

| Sub-Pack Topic | Test Tag / Query | Unsplash `query` | Pexels `query` | DeviantArt `tag` | Met `q` / `medium` |
|---|---|---|---|---|---|
| **Suggestions** | `suggestions` | *None (Bundled)* | *None (Bundled)* | *None (Bundled)* | *None* |
| **Random** | `random` | Resolves to random stock topic | Resolves to random stock topic | Resolves to random stock topic | `photograph` / `Photographs` |
| **Nature** | `nature` | `nature landscape` | `nature` | `nature` | `photograph` / `Photographs` |
| **City** | `city` | `city urban` | `city` | `cityscape` | `photograph` / `Photographs` |
| **Ocean** | `ocean` | `ocean sea` | `ocean` | `ocean` | `photograph` / `Photographs` |
| **Mountains** | `mountains` | `mountains peak` | `mountains` | `mountains` | `photograph` / `Photographs` |
| **Abstract** | `abstract` | `abstract texture` | `abstract` | `abstract` | `photograph` / `Photographs` |
| **Architecture**| `architecture`| `architecture building` | `architecture` | `architecture` | `photograph` / `Photographs` |
| **Sky** | `sky` | `sky clouds` | `sky` | `sky` | `photograph` / `Photographs` |
| **Street Art** | `streetart` | `street art mural graffiti` | `street art mural` | `streetart` | `photograph` / `Photographs` |

---

### 5.2 Photo Provider Query Parameters

#### 1. Unsplash (`unsplash`)
- **Source Class**: `UnsplashSource`
- **Endpoint**: `GET https://api.unsplash.com/search/photos`
- **HTTP Header**: `Authorization: Client-ID {accessKey}`
- **Query Parameters**:
  - `query`: Mapped keyword (e.g. `nature landscape`).
  - `orientation`: `landscape`.
  - `per_page`: `20`.
  - `page`: Random page index via `RemoteSample.randomPage()`.
- **Example Query**:
  `https://api.unsplash.com/search/photos?query=nature%20landscape&orientation=landscape&per_page=20&page=1`

#### 2. Pexels (`pexels`)
- **Source Class**: `PexelsSource`
- **Endpoint**: `GET https://api.pexels.com/v1/search`
- **HTTP Header**: `Authorization: {apiKey}`
- **Query Parameters**:
  - `query`: Mapped keyword (e.g. `nature`).
  - `orientation`: `landscape`.
  - `per_page`: `20`.
  - `page`: Random page index.
- **Example Query**:
  `https://api.pexels.com/v1/search?query=nature&orientation=landscape&per_page=20&page=1`

#### 3. DeviantArt (`deviantart`)
- **Source Class**: `DeviantArtSource`
- **Token Endpoint**: `GET https://www.deviantart.com/oauth2/token?grant_type=client_credentials&client_id={clientId}&client_secret={clientSecret}`
- **Browse Endpoint**: `GET https://www.deviantart.com/api/v1/oauth2/browse/tags`
- **Query Parameters**:
  - `tag`: Mapped single-word tag (e.g. `nature`, `cityscape`).
  - `access_token`: OAuth2 client-credentials token.
  - `limit`: `20`.
  - `offset`: Random item offset.
  - `mature_content`: `false`.
- **Example Query**:
  `https://www.deviantart.com/api/v1/oauth2/browse/tags?tag=nature&access_token={token}&limit=20&offset=0&mature_content=false`

---

## 6. Video & Genart Pack Flows

### 6.1 Video Packs (`video`)
- **Providers**: Pexels Video (`pexels-video`), Pixabay Video (`pixabay-video`), Coverr (`coverr`).
- **Parameters**:
  - **Pexels Video**: `GET https://api.pexels.com/v1/videos/search?query={q}&orientation=landscape&per_page=20`
  - **Pixabay Video**: `GET https://pixabay.com/api/videos/?key={key}&q={q}&video_type=film&safesearch=true&per_page=20`
  - **Coverr**: `GET https://api.coverr.co/videos?query={q}&urls=true&page_size=20&sort=popular` (Header: `Authorization: Bearer {apiKey}`).
- **Response shape highlights**: Pexels Video (`videos[].video_files[]`, picked by `quality == "hd"` else largest resolution); Pixabay Video (`hits[].videos.medium/large/small/tiny.url`); Coverr (`hits[].urls.mp4`, `is_vertical` filtered out).
- **No disk cache for video streams** — ExoPlayer plays `remoteUrl` directly; only whatever ExoPlayer/OkHttp buffers in memory.

### 6.2 Genart Packs (`genart`)
- **Providers**: `GenartSource` (`genart`), `FractalSource` (`fractal`), `CustomFractalSource` (`custom_fractal`).
- **Flow**: Operates entirely in-memory using Multiplatform Canvas rendering shaders and math routines (Julia/Mandelbrot sets, Voronoi, particle fields, perlin noise). Does not issue HTTP network calls.

---

## 7. Caching Behavior

1. **HTTP-level cache (OkHttp)**: 50 MB disk cache at `cacheDir/http_cache`, configured in `ArthurApp.kt`. A custom `ForceCacheNetworkInterceptor` (`DebugInterceptor.kt`) rewrites successful GET responses lacking `no-store` to `Cache-Control: public, max-age=3600`, i.e. it force-caches API JSON responses for 1 hour even if the origin sent no/short cache headers.
2. **Debug/telemetry interceptor**: `DebugInterceptor` records every request's source id (via `X-Source-Id` header or URL-sniffing), duration, cache-hit flag, and status code into `DebugLogger` — surfaced in the in-app `FloatingDebugBar` when verbose/developer mode is on.
3. **Artwork image disk cache**: `ArtworkImageCache` (`StockPhotoSettings.kt`) stores downloaded still images at `cacheDir/artwork/<sanitized-artwork-id>.img`, plus a flat-file "catalog" index (SharedPreferences) mapping id → title/attribution/sourceId/kind/remoteUrl/category/lastAccess. This backs:
   - **Offline fallback** for Pexels/Unsplash/DeviantArt (`offlineFallback = { cache.loadCachedStock(...) }` in `ArthurApp.kt`): when the API key is blank or the live call returns nothing, previously-cached stock images for that category are served instead.
   - **Genart "bake" cache**: `rememberGenart`/`loadCachedGenart`, LRU-capped at 96 entries (`MAX_GENART`), used to serve pre-rendered generative frames (e.g. for Android Auto) without re-running the Canvas engine.
   - **Ambient prefetch**: `StillImagePrefetcher.ensureCached()` warms this same cache for the current + neighboring pool artworks so swiping/rotating doesn't block on network.
4. **In-memory prefetch cache**: `ControlPlaneScreen`'s local `packCatalogCache: MutableMap<String, List<Artwork>>` holds the last `ContentEngine.catalog()` result per sub-pack for the lifetime of the composable so "Start Ambient" doesn't re-fetch if the user just opened that sub-pack.
5. **Cross-Activity handoff (not a cache, but avoids a refetch)**: `AmbientRotationLaunch` is a static in-process holder passing the already-fetched pool + `renewSourceIds` from `ControlPlaneScreen` into `AmbientActivity` without re-querying.
6. **Free-tier gating acts after caching**: `ContentEngine.applyGates()`/`fairStillSample()` round-robins across sources and caps `FractalPreset`/`Genart`/other-stills counts for non-premium users — this happens in-memory after all sources have already returned data, so it doesn't reduce the number of API calls made, only what's shown/pooled.

---

## 8. Why Some Features / APIs Are Not Yet Implemented & Rely on Fallbacks

Several pack queries and sub-categories rely on fallbacks or curated subsets rather than direct live search APIs.

### 1. Absence of Public Live Search APIs on Museum Platforms
- **Musée du Louvre (`louvre`)**:
  - *Why not live search?* Musée du Louvre provides individual object JSON at `collections.louvre.fr/ark:/53355/{id}.json`, but does **not** host a public `/search` endpoint or open search API.
  - *Fallback mechanism*: Arthur uses a curated list of open-access ARK identifiers (`PAINTING_ARKS`, `SCULPTURE_ARKS`) and hydrates title, artist, and image metadata per object.
- **National Gallery of Art (NGA)**:
  - *Why not live search?* NGA only provides static CSV data dumps and IIIF image manifests without a live API endpoint (see `docs/roadmap-museum-sources.md`).

### 2. Missing API Keys at Build Time or Runtime
- **Sources Affected**: `HarvardSource`, `SmithsonianSource`, `EuropeanaSource`, `UnsplashSource`, `PexelsSource`, `DeviantArtSource`, `PexelsVideoSource`, `PixabayVideoSource`, `CoverrSource`.
- *Why fallback occurs*: When building the open-source APK without secret API keys (or when running offline), these sources evaluate `apiKey.isBlank()` to `true`.
- *Fallback mechanism*: They safely return an empty catalog or call `offlineFallback()`, which gracefully supplies high-quality curated assets from `BundledPackSource`.

### 3. Deliberate Design of the "Suggestions" Sub-Pack
- *Why fallback / bundled?*: The `Suggestions` sub-pack (`StockPhotoCategory.Suggestions`, `MuseumTopic.Suggestions`) is intentionally designed to present curated bundled artworks stored inside `BundledPackSource`.
- `RemoteCategoryMapping.stockQuery()` explicitly returns `null` for `Suggestions`, bypassing remote HTTP requests entirely to guarantee instant, offline-ready ambient display.

### 4. Fair Sampling & Free-Tier Gating in `ContentEngine`
- **Free-tier Limits**: Free-tier users are subject to limits (`maxPhotoArtwork`, `maxFractalPresets`, `maxGenart`).
- **Fair Round-Robin Sampling**: When loading "All" museum sources, `ContentEngine.fairStillSample()` round-robin samples artworks across sources so fast/large endpoints (e.g. Rijksmuseum/Met) do not starve smaller or slower endpoints.
- **Strict Fallback Rules**: `PackSelection.allowsGenerativeAmbientFallback()` returns `false` for still packs (Museum, Photo, Painting, Sculpture). If a remote search returns zero items (e.g., network error or missing key), Arthur will **never** display an unexpected procedural particle animation in a museum pack; instead, it safely falls back to curated still images from `BundledPackSource`.

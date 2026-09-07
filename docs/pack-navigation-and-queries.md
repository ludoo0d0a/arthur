# Pack Navigation Flows, Queries, and Fallbacks Architecture

This document details the navigation flows, API endpoints, query parameters, sampling strategies, and fallback mechanisms for all Ambient Art Packs in Arthur.

---

## 1. Pack System Overview & Architecture

Arthur organizes ambient content into Spotify-style **Pack Families** on the Control Plane grid. Each family contains **Sub-Packs** (specific topics, institutions, or keywords).

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

## 2. Museum Pack Flows & Query Parameters

When a user selects a Museum, Painting, or Sculpture pack, `sourceIdsForAmbientLoad()` determines which sources to query.

### 2.1 "All" & "Random" Flow Logic

- **`museum/all` / `painting/all` / `sculpture/all`**:
  - `sourceIdsForAmbientLoad()` returns all active museum source IDs (`MuseumSourceIds`).
  - `MuseumLoad.acrossTargets(kind, limit = 20, random)` expands `MuseumSearchKind.All` into concrete search targets: `[Painting, Sculpture]`.
  - **Query Distribution**: The limit (20) is split equally across targets: `perKind = 20 / 2 = 10`.
  - Up to 10 items are loaded for `Painting` and 10 for `Sculpture` per provider, merged, and then pseudo-randomly sampled down to the total requested batch limit (20).
- **`museum/random`**:
  - Resolves `MuseumTopic.Random` (all museum sources combined with random offset windowing across endpoints).

---

### 2.2 Museum Source Query Parameter Details

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

---

## 3. Photo Pack Flows & Query Parameters

Selecting the **Photo** pack family routes search across stock photo providers, community platforms, museum photography collections, and bundled local packs.

### 3.1 Photo Topics & Query Mapping (`StockPhotoCategory`)

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

### 3.2 Photo Provider Query Parameters

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

## 4. Video & Genart Pack Flows

### 4.1 Video Packs (`video`)
- **Providers**: Pexels Video (`pexels-video`), Pixabay Video (`pixabay-video`), Coverr (`coverr`).
- **Parameters**:
  - **Pexels Video**: `GET https://api.pexels.com/v1/videos/search?query={q}&orientation=landscape&per_page=20`
  - **Pixabay Video**: `GET https://pixabay.com/api/videos/?key={key}&q={q}&video_type=film&safesearch=true&per_page=20`
  - **Coverr**: `GET https://api.coverr.co/videos?query={q}&urls=true&page_size=20&sort=popular` (Header: `Authorization: Bearer {apiKey}`).

### 4.2 Genart Packs (`genart`)
- **Providers**: `GenartSource` (`genart`), `FractalSource` (`fractal`), `CustomFractalSource` (`custom_fractal`).
- **Flow**: Operates entirely in-memory using Multiplatform Canvas rendering shaders and math routines (Julia/Mandelbrot sets, Voronoi, particle fields, perlin noise). Does not issue HTTP network calls.

---

## 5. Why Some Features / APIs Are Not Yet Implemented & Rely on Fallbacks

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

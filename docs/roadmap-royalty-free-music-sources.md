# Royalty-Free Calm & Zen Music Remote Sources

**API key signup links & `local.properties` wiring:** [royalty-free-music-api-keys.md](./royalty-free-music-api-keys.md)

Backlog and evaluation for open, legal, royalty-free audio and music APIs behind the shared audio streaming subsystem in Arthur.
Complements local procedural music generation (`ProceduralMusicEngine`) and local AI stem dynamic mixing (`MusicDirector`) by providing streamable, high-fidelity studio music tracks, ambient nature soundscapes, and calm/zen recordings.

**Goal:** Provide studio-quality, royalty-free zen, calm, and ambient music via free developer APIs without requiring paid subscriptions or heavy on-device neural weight files.

---

## Executive Summary & Comparison Matrix

| Source API | API Key / Auth | Focus / Catalog Type | Primary Licensing | Audio Format | Rate Limits | Suitability for Zen / Calm |
|------------|----------------|----------------------|-------------------|--------------|-------------|----------------------------|
| **Jamendo API** | Free instant `client_id` | Full music tracks (Ambient, Chillout, Meditation, Zen, Classical) | Creative Commons (CC0, CC-BY, CC-BY-NC) | Direct HTTP MP3 (128kbps / 320kbps) | 20,000 req/day | **Excellent** (huge ambient/chill catalog with tag filters) |
| **Freesound API** | Free instant API key | Nature soundscapes, Tibetan singing bowls, rain, wind, ambient synth drones | CC0, CC-BY, CC-BY-NC | Direct HQ MP3 / OGG preview streams | 2,000 req/60 min | **Outstanding** (best for organic nature & zen soundscapes) |
| **Archive.org (Internet Archive)** | **None** (Zero-Key public access) | Public Domain & CC Netlabels (`opensource_audio` / `netlabels` — other items often 401 on CDN) | Public Domain, CC0, CC-BY | Direct HTTP MP3 streaming | Generous (open access with descriptive User-Agent) | **High** (vast public domain & CC ambient collections) |
| **Pixabay Music API** | Free API key (shared with Pixabay Video/Photo) | Studio royalty-free relaxing, yoga, chill, meditation music | Pixabay Content License | Direct HTTP MP3 streams | 100 req/min | **Paused** (`/api/audio/` returns 403 for standard keys; public API is images/videos only) |
| **Free Music Archive (FMA)** | Free API key | Curated independent ambient, acoustic, and minimalist tracks | CC0, CC-BY, CC-BY-NC | Direct HTTP MP3 links | Standard developer tier | **Paused** (legacy API 404 — removed from app for now) |
| **Incompetech (Kevin MacLeod)** | None / Direct CC catalog | Iconic calm, meditation, and atmospheric background music | Creative Commons BY 4.0 | Direct HTTP MP3 | Open access | **High** (well-known peaceful compositions) |

---

## Detailed API Evaluations

### 1. Jamendo API
* **Documentation**: [developer.jamendo.com](https://developer.jamendo.com/v3.0/tracks)
* **API Endpoint**: `GET https://api.jamendo.com/v3.0/tracks/`
* **Query Parameters**:
  - `client_id`: Developer key (free self-serve signup).
  - `format=json`: JSON response.
  - `tags`: Genre & mood tags (e.g. `tags=ambient+zen`, `tags=meditation`, `tags=chillout`).
  - `license_cc`: License filter (`by`, `cc0`, `by-nc`).
  - `audioformat`: Stream format (`mp32` for 320kbps MP3 or `mp31` for 128kbps MP3).
  - `limit`: Number of tracks per page (e.g. `limit=20`).
* **Authentication**: Free instant `client_id` via self-serve developer portal.
* **Audio Delivery**: Direct HTTP MP3 streaming URLs delivered in the JSON `audio` field.
* **Licensing**: Creative Commons options. For CC-BY tracks, artist attribution must be shown in UI.
* **Rate Limits**: 20,000 requests per day (generous for app usage).
* **Pros**:
  - Direct MP3 streaming URLs without complex token signing or OAuth.
  - Granular mood and genre tagging (`zen`, `ambient`, `relaxation`, `meditation`, `calm`).
  - Rich metadata (track title, artist name, album cover art, duration).
* **Cons**:
  - Requires a free `client_id`.
  - Creative Commons BY tracks require artist attribution display on player chrome.

---

### 2. Freesound API
* **Documentation**: [freesound.org/docs/api/](https://freesound.org/docs/api/)
* **API Endpoint**:
  - Text Search: `GET https://freesound.org/apiv2/search/text/`
  - Sound Detail: `GET https://freesound.org/apiv2/sounds/{sound_id}/`
* **Query Parameters**:
  - `query`: Text search query (e.g. `query=singing+bowl+zen` or `query=ambient+rain`).
  - `token`: Developer API key.
  - `filter`: Advanced property filter (e.g. `filter=license:"Creative Commons 0"` or `filter=type:mp3`).
  - `fields`: Comma-separated field selector (`id,name,previews,license,username,tags,duration`).
* **Authentication**: Free API Key (instant registration on Freesound.org).
* **Audio Delivery**: Direct MP3 previews (`preview-hq-mp3` URL in response).
* **Licensing**: CC0 (Public Domain), CC-BY (Attribution), CC-BY-NC. CC0 can be enforced via search filter `filter=license:"Creative Commons 0"`.
* **Rate Limits**: 2,000 requests per 60 minutes.
* **Pros**:
  - Exceptional quality for organic ambient textures: rain, ocean waves, wind, Tibetan bowls, gong soundscapes, soft synth drones.
  - Ability to filter strictly for CC0 (Public Domain) assets to avoid attribution overhead.
* **Cons**:
  - More focused on soundscapes and atmospheric audio than traditional multi-instrument songs.

---

### 3. Internet Archive (Archive.org) Audio API
* **Documentation**: [archive.org/advancedsearch.php](https://archive.org/advancedsearch.php)
* **API Endpoint**: `GET https://archive.org/advancedsearch.php`
* **Query Parameters**:
  - `q`: Lucene search query (e.g. `mediatype:audio AND subject:(ambient OR zen OR meditation OR calm)`).
  - `fl[]`: Field list (`identifier,title,creator,licenseurl,year`).
  - `output=json`: JSON response.
* **Authentication**: **No API key required** (open public access). Requires a descriptive HTTP `User-Agent`.
* **Audio Delivery**: Direct HTTP audio stream URLs formatted as `https://archive.org/download/{identifier}/{file}.mp3`.
* **Licensing**: Public Domain, CC0, CC-BY.
* **Rate Limits**: Open public access; automatic backoff on HTTP 429.
* **Pros**:
  - Zero-key setup: works out of the box without developer key registration or environment secrets.
  - Massive historical catalog of public domain music, netlabels (e.g. Kahvi Collective, Mindful Meditation series), and relaxation field recordings.
* **Cons**:
  - Uncurated metadata requiring defensive field checks and item filtering.

---

### 4. Pixabay Music / Audio API
* **Documentation**: [pixabay.com/api/docs/](https://pixabay.com/api/docs/)
* **API Endpoint**: `GET https://pixabay.com/api/docs/#api_audio`
* **Query Parameters**:
  - `key`: Developer API key (shared with Pixabay Video & Photo API).
  - `q`: Free-text search (e.g. `q=zen+calm` or `q=meditation`).
  - `category`: Music category (e.g. `ambient`, `relaxing`).
* **Authentication**: Free API Key.
* **Audio Delivery**: Direct MP3 streaming URLs.
* **Licensing**: Pixabay Content License (free for commercial and non-commercial app use, no copyright claims).
* **Rate Limits**: 100 requests per 60 seconds per key.
* **Pros**:
  - High studio production quality for relaxing, yoga, and calm instrumental music.
  - Single key shared with existing Pixabay stock video integration in Arthur.
* **Cons**:
  - Strict 100 req/min rate limit requires local caching of track URL responses.

---

### 5. Free Music Archive (FMA) / Tribe of Noise API — paused
* **Status**: Temporarily **removed from the app** (legacy `get/tracks.json` returns HTTP 404 as of Oct 2026). Revisit when FMA publishes a working API.
* **Documentation**: [freemusicarchive.org/api](https://freemusicarchive.org/api)
* **API Endpoint**: `GET https://freemusicarchive.org/api/get/tracks.json`
* **Query Parameters**:
  - `api_key`: Developer API key.
  - `genre_handle`: Genre filter (e.g. `Ambient`, `Experimental`, `Spoken`).
* **Authentication**: Free API Key.
* **Audio Delivery**: Direct MP3 stream links.
* **Licensing**: Creative Commons (CC0, CC-BY, CC-BY-NC).
* **Pros**: Curated library of high-quality independent acoustic and ambient compositions.
* **Cons**: Search indexing and tagging can be less dynamic than Jamendo or Freesound; current public API unavailable.

---

### 6. Incompetech (Kevin MacLeod / CC Archive)
* **Documentation**: [incompetech.com/music/royalty-free/](https://incompetech.com/music/royalty-free/)
* **API / Feed**: Direct JSON feed / Archive.org mirror index.
* **Authentication**: None required.
* **Licensing**: Creative Commons BY 4.0 (requires attribution: "Music by Kevin MacLeod (incompetech.com)").
* **Audio Format**: Standard direct HTTP MP3.
* **Pros**: Iconic, instantly recognizable peaceful compositions ("Calm", "Peace of Mind", "Meditation").
* **Cons**: Fixed static catalog size compared to dynamic search platforms.

---

## Integration Architecture in Arthur

```
+-----------------------------------------------------------------+
|                       Control Plane UI                          |
|  - SoundPlayerScreen / Settings / Audio Pack Catalog            |
+-----------------------------------------------------------------+
                                |
                                v
+-----------------------------------------------------------------+
|                   Remote Audio Engine (:shared)                 |
|  - RemoteAudioSource (Jamendo / Freesound / Archive.org)        |
|  - AudioTrackMetadata (title, artist, streamUrl, license, art)  |
+-----------------------------------------------------------------+
                                |
                                v
+-----------------------------------------------------------------+
|                   Media3 ExoPlayer Subsystem                    |
|  - HTTP Range Audio Caching (AudioCacheManager)                 |
|  - Focus handling & Ambient Auto/TV playback                    |
+-----------------------------------------------------------------+
```

### Proposed KMP Interface (`RemoteAudioSource`)

```kotlin
interface RemoteAudioSource {
    val id: String
    val displayName: String

    suspend fun fetchZenTracks(
        query: String = "zen calm ambient",
        limit: Int = 20
    ): Result<List<RemoteAudioTrack>>
}

data class RemoteAudioTrack(
    val id: String,
    val title: String,
    val artist: String,
    val streamUrl: String,
    val durationSeconds: Int,
    val license: String,
    val artworkUrl: String? = null,
    val sourceId: String
)
```

---

## Implementation Checklist for Shipping an Audio Source

1. **`shared` Source Definition**:
   - Implement `RemoteAudioSource` in `:shared`.
   - Add unit tests with JSON fixture mocks.
2. **Audio Caching**:
   - Integrate stream URLs with `AudioCacheManager` and ExoPlayer `SimpleCache` for offline/smooth playback.
3. **Audio Pack Mapping**:
   - Map remote zen tracks to relevant `AudioPackCatalog` entries (e.g. `AudioPackTopic.Zen`, `TEMPLE_RESONANCE`, `ATMOSPHERES_NATURE`).
4. **UI & Attribution**:
   - Display track title, artist name, and license attribution on `SoundPlayerScreen` and Android Auto `ArthurMediaService`.
5. **Documentation**:
   - Update `CONTEXT.md` and `README.md` with the new remote audio source.

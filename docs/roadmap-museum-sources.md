# Museum / famous-art Remote Sources

Backlog for museum connectors behind the shared `Source` interface. Stock photos (Pexels / Unsplash) and Wikimedia stay out of this list — see [ADR 0008](adr/0008-rijksmuseum-then-met.md).

**Goal:** open-access (or clearly licensed) works with images, attribution, no scraping. Prefer **no API key** so the APK ships without secrets.

## Shipped

| Source | `sourceId` | API | Key | Notes |
|--------|------------|-----|-----|--------|
| Rijksmuseum | `rijksmuseum` | Linked Art Search | none | Paintings, sculptures, photographs |
| The Met | `met` | Collection API | none | Public-domain + `hasImages` only |
| Art Institute of Chicago | `artic` | [api.artic.edu](https://api.artic.edu/docs/) | none | IIIF images; PD filter |
| Cleveland Museum of Art | `cleveland` | [Open Access API](https://openaccess-api.clevelandart.org/) | none | CC0 + `has_image` |

## Backlog

Status: `idea` until a `Source` ships (then move to **Shipped**).

### Prefer next (no key / open access)

- **National Gallery of Art (NGA)** — open-data CSVs + IIIF images (no live search API; needs a curated subset or offline index).
- **Cooper Hewitt (Smithsonian Design)** — collection API; optional key for higher rate limits.
- **Paris Musées / Louvre collections** — check current open endpoints and image license before wiring.
- **British Museum** — collection search exists; verify image reuse terms and rate limits.

### Needs free API key (defer unless we add BuildConfig secrets)

Put keys in repo-root `local.properties` (gitignored). Suggested property names:

```properties
HARVARD_API_KEY=
SMITHSONIAN_API_KEY=
EUROPEANA_API_KEY=
```

Wire into `BuildConfig` only when a Source ships (same pattern as `PEXELS_API_KEY`).

#### Harvard Art Museums — `HARVARD_API_KEY`

1. Open the key request form: [Google Form](https://docs.google.com/forms/d/1Fe1H4nOhFkrLpaeBpLAnSrIMYvcAxnYWm0IU9a6IkFA/viewform) (also linked from [harvardartmuseums.org/collections/api](https://harvardartmuseums.org/collections/api)).
2. Fill name, email, and a short description (e.g. “Arthur ambient-art Android app — open-access object images for Ambient Rotation”).
3. Wait for email with a UUID-style key.
4. Add `HARVARD_API_KEY=<key>` to `local.properties`.
5. Smoke-test:  
   `curl "https://api.harvardartmuseums.org/object?size=1&apikey=$HARVARD_API_KEY"`

**Caveat:** Harvard API ToS is **non-commercial** and requires free access + a description of apps that use the content — review before shipping in a Play-listed paid app.

Docs: [harvardartmuseums/api-docs](https://github.com/harvardartmuseums/api-docs)

#### Smithsonian Open Access — `SMITHSONIAN_API_KEY`

1. Sign up at [api.data.gov/signup](https://api.data.gov/signup/) (name + email).
2. Key is emailed immediately (api.data.gov gateway; used as `api_key` query param on `api.si.edu`).
3. Add `SMITHSONIAN_API_KEY=<key>` to `local.properties`.
4. Smoke-test:  
   `curl "https://api.si.edu/openaccess/api/v1.0/search?q=online_media_type:Images&rows=1&api_key=$SMITHSONIAN_API_KEY"`

`DEMO_KEY` works for a few calls (strict rate limit) — use a personal key for real use (~1000 req/hr typical api.data.gov tier).

Docs: [api.si.edu/openaccess](https://api.si.edu/openaccess) · CC0 collection content.

#### Europeana — `EUROPEANA_API_KEY`

1. Create a free account: [europeana.eu](https://www.europeana.eu/en/create-and-use-a-europeana-account).
2. Log in → profile menu → **Manage API key** (or [API key help](https://www.europeana.eu/en/how-to-register-for-and-manage-an-api-key)).
3. Accept API key ToS → **Request a personal API key** (instant; for testing/dev). For Play production at scale, request a **project** key (manual review).
4. Add `EUROPEANA_API_KEY=<key>` to `local.properties`.
5. Smoke-test Search API (see [Europeana APIs](https://www.europeana.eu/en/apis) / Knowledge Base for the current `wskey` / header style).

**Note:** Aggregator of many institutions — brand as “Europeana”, not a single museum.

### Explicitly out of scope (for now)

- **Wikimedia Commons** — not the famous-art connector (ADR 0008).
- Scraped museum websites or closed platforms without a public API / ToS path.

## Implementation checklist (when shipping one)

1. `shared` Source: inject `httpGet`, fixture unit tests, PD/CC0 + image-required filter.
2. Register in `ArthurApp` `ContentEngine` sources list.
3. Folder title + order in `ArthurMediaBrowse`.
4. Mention in `CONTEXT.md` Remote Source / privacy museum list.

# Community Art Remote Sources

Backlog for community-driven digital-art / illustration connectors behind the shared `Source`
interface. Distinct from museum connectors ([`roadmap-museum-sources.md`](roadmap-museum-sources.md)),
street-art connectors ([`roadmap-streetart-sources.md`](roadmap-streetart-sources.md)), and stock
photos (Pexels / Unsplash). "Community" here means creator-uploaded illustration / digital art
platforms, not institutional collections.

**Goal:** browsable creator artwork with downloadable images and clear attribution. License bar is
the hardest in this category: most community platforms default to All-Rights-Reserved and only a
minority of works are explicitly Creative Commons / public domain — treat every Source here as
attribution-only (in-app ambient display + link back to the original page) unless the API can
filter to an open license, and call that out per Source rather than assuming.

## Shipped

| Source | `sourceId` | API | Key | Notes |
|--------|------------|-----|-----|--------|
| DeviantArt | `deviantart` | [Browse API](https://www.deviantart.com/developers/) `browse/tags` | `DEVIANTART_CLIENT_ID` + `DEVIANTART_CLIENT_SECRET` (OAuth2 client-credentials) | Attribution-only: defaults to All-Rights-Reserved, Browse exposes no per-item license field; mature-flagged deviations dropped; uses the shared **photo topic** (tag search) like Pexels/Unsplash |

## Backlog

Status: `idea` until a `Source` ships (then move to **Shipped**).

### Prefer next (images + free/registerable access)

| Source | API doc | API key | Limitation | Pagination |
|--------|---------|---------|------------|------------|
| **Openverse** | [api.openverse.org](https://api.openverse.org/) | None for search | CC / public-domain aggregator (includes Flickr, Wikimedia, museum sets); explicit `license` field per result — best license-safety fit in this category, prefer over scattered single-platform CC search | `page` + `page_size` (offset paging) |
| **Pixabay** (images) | [pixabay.com/api/docs](https://pixabay.com/api/docs/) | Free, instant, self-serve | Official/supported; 100 req/60s per key; results must be cached ≥24h per ToS; images under Pixabay's own "Content License" (free for most commercial/personal use, **not** CC0 — redistributing the raw API dataset itself is disallowed) | `page` (default 1) + `per_page` (3–200, default 20), offset paging |
| **Flickr** | [flickr.com/services/api](https://www.flickr.com/services/api/) `flickr.photos.search` | Free (Flickr/Yahoo account); non-commercial is self-serve/instant, commercial use needs a separate Flickr-reviewed request | Official/supported; 3,600 queries/hour per key; filter `license=` to CC0/CC BY/CC BY-SA only — per-photo license varies wildly otherwise, must read the `license` field per result | `page` + `per_page` (max ~500/page; geo/bbox queries capped at 250/page) |
| **Wikimedia Commons** (general browse, distinct from the street-art-scoped connector — [ADR 0008](../adr/0008-rijksmuseum-then-met.md)) | [MediaWiki API](https://www.mediawiki.org/wiki/API:Search) / `commons.wikimedia.org/w/api.php` | None for read/search; send a descriptive `User-Agent` | Official/supported; no hard published anon rate limit but must back off on `ratelimited` + serialize requests; per-file license varies (many CC-BY-SA/PD but not universal) — check each file's license field | Cursor-style `continue`/`srcontinue` token in the response, loop until absent (not page numbers) |
| **Pexafy** | [docs.pexafy.com](https://docs.pexafy.com/) | Free self-serve signup, no card required on the free plan | Small/independent third-party meta-search aggregator layering over ~9 stock sources (Unsplash, Pexels, Pixabay, etc.), not affiliated with any of them; per-photo licensing actually depends on the origin source despite the aggregator's blanket "free to use" claim — low-confidence/unverified longevity, sanity-check with a live call before relying on it | Cursor-based (per their docs) |
| **Behance** (Adobe) | historically `api.behance.net` / behance.net/dev | New key issuance appears closed since a 2023–2024 Adobe "technical migration"; no committed relaunch date on adobe.io | Not officially supported at present; content is all-rights-reserved (portfolio site, not stock/CC); revisit if Adobe reopens it | Undocumented in current (closed) state |
| **Pixiv** — large illustration community; API is unofficial / reverse-engineered (no public developer program), ToS explicitly restricts scraping and third-party redistribution. Treat as blocked, not just backlog, unless that changes. | — | — | — | — |

### Weak / blocked

| Source | API doc | API key | Limitation | Pagination |
|--------|---------|---------|------------|------------|
| **ArtStation** | None — no official public API/developer portal; third-party wrappers scrape undocumented internal JSON (e.g. `/projects/{id}.json`) | N/A, no registration exists | Unofficial/reverse-engineered, can break without notice, likely against ToS for automated scraping/redistribution; content is all-rights-reserved by default | Undocumented, varies by scraped endpoint |
| **Dribbble** | [developer.dribbble.com/v2](https://developer.dribbble.com/v2/) | OAuth app registration is nominally open (`dribbble.com/account/applications/new`) | The only documented listing endpoint is `GET /user/shots` (the *authenticated user's own* shots) — **no general search/browse endpoint** in the public v2 API; broader read access needs Dribbble partner approval (case-by-case, not self-serve); shots are all-rights-reserved | `page` + `per_page` (up to 100, not honored by every endpoint); Link-header paging recommended |
| **500px** | [legacy docs (deprecated)](https://github.com/500px/legacy-api-documentation) | Closed to new developers since June 2018; only path back in is emailing `sales@500px.com` for paid/enterprise access | Unofficial for practical purposes; no confirmed reopening of self-serve access | N/A without an enterprise arrangement (legacy docs describe `page`/`rpp`) |
| **Imgur** | [apidocs.imgur.com](https://apidocs.imgur.com/) | Free, self-serve OAuth app registration (`api.imgur.com/oauth2/addclient`); no approval needed for anonymous/free tier | Officially supported; free tier commonly cited around 12,500 req/day (credit-based, check `X-RateLimit` headers); content is user-uploaded with mixed/mostly-absent licensing, ToS restricts bulk scraping/redistribution | `page` param on most plural endpoints (~50/page typical); `/gallery` endpoints don't support `perPage`; `/album/{id}/images` isn't paged at all |
| **Saatchi Art / Artsy** | Artsy: [developers.artsy.net](https://developers.artsy.net/) (legacy REST v1; register a client app for `client_id`/`client_secret`, exchange for an `xapp_token`). Saatchi Art: no open browse API | Free but approval-style app registration (not fully anonymous/instant) | Commercial marketplaces (sell original art); Artsy's terms scope images to non-commercial/educational display, not general free-image stock; Saatchi Art reuse of listed artwork is explicitly for sales purposes only | Artsy: standard REST `page`/`size` params |
| **Ello** | None — no public API for content browsing | N/A | Blocked | N/A |

### Explicitly out of scope (for now)

- Scraping any of the above when no public API / ToS path exists.
- Shipping All-Rights-Reserved community artwork without attribution + link-back, or presenting it
  as openly licensed.
- Community marketplaces where the platform's purpose is art *sales* (Saatchi Art, Artsy) — reuse
  terms are not compatible with an ambient display use case.

## Keys

```properties
DEVIANTART_CLIENT_ID=
DEVIANTART_CLIENT_SECRET=
```

Signup notes:

- **DeviantArt** — [register an application](https://www.deviantart.com/developers/apps) for a
  Client ID + Client Secret; the Browse API only needs the OAuth2 **client-credentials** grant
  (no user login). Review DeviantArt's API ToS before any reuse beyond in-app ambient display —
  Browse does not expose per-item license data, so default to All-Rights-Reserved.

## Implementation checklist (when shipping one)

1. `shared` Source: inject `httpGet`, fixture unit tests, mature/NSFW filter, license field when
   the API exposes one (prefer Openverse-style explicit `license` over guessing).
2. Register in `ArthurApp` `ContentEngine` sources list.
3. Folder title + order in `ArthurMediaBrowse`.
4. Reuse the shared **photo topic** (`StockPhotoCategory` + `RemoteCategoryMapping`) when the API
   supports free-text / tag search, so Control Plane needs no new topic UI.
5. Mention in `CONTEXT.md` Remote Source / community art glossary entry.
6. Call out the license caveat explicitly in the glossary entry and in this doc's Shipped table —
   do not let a Source imply open licensing it cannot back up.

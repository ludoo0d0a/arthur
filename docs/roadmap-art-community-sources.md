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

- **Openverse** ([api.openverse.org](https://api.openverse.org/)) — Creative Commons / public-domain
  aggregator (includes Flickr, Wikimedia, museum sets, and more) with an explicit `license` field
  per result and no key for search. Best license-safety fit in this category — prefer this over
  scattered single-platform CC search where possible.
- **Flickr** ([REST API](https://www.flickr.com/services/api/)) — `flickr.photos.search` with
  `license=` filter (CC0/CC BY/CC BY-SA only) and free API key; huge community photography corpus,
  well-documented image size URLs.
- **Behance** (Adobe) — `api.behance.net` v2 exists but new API key registration has been closed /
  unreliable for new apps; revisit if Adobe reopens it. No documented per-project license facet.
- **Pixiv** — large illustration community; API is unofficial / reverse-engineered (no public
  developer program), ToS explicitly restricts scraping and third-party redistribution. Treat as
  blocked, not just backlog, unless that changes.

### Weak / blocked

- **ArtStation** — no public read API; a project JSON endpoint (`/projects/{id}.json`) exists but
  is undocumented and has been rate-limited / blocked for bulk/anonymous use. No license facet.
- **Saatchi Art / Artsy** — commercial marketplaces (sell original art); no open browse API, and
  reuse of listed artwork images is explicitly for sales purposes only.
- **500px** — API access has been closed to new third-party developers since 2018.
- **Ello** — no public API for content browsing.

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

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
| Europeana | `europeana` | [Search API](https://www.europeana.eu/en/apis) | `EUROPEANA_API_KEY` | Open reusability + image media; aggregator |
| Harvard Art Museums | `harvard` | [Object API](https://github.com/harvardartmuseums/api-docs) | `HARVARD_API_KEY` | `classification` + `hasimage`; review ToS |
| Smithsonian | `smithsonian` | [Open Access](https://api.si.edu/openaccess) | `SMITHSONIAN_API_KEY` | Prefers CC0 image media |
| Musée du Louvre | `louvre` | [Collections JSON](https://collections.louvre.fr/en/page/documentationJSON) | none | Curated ARK list + per-object `.json` |

## Backlog

Status: `idea` until a `Source` ships (then move to **Shipped**).

### Prefer next (no key / open access)

- **National Gallery of Art (NGA)** — open-data CSVs + IIIF images (no live search API; needs a curated subset or offline index).
- **Cooper Hewitt (Smithsonian Design)** — already reachable via Smithsonian `unit_code:CHNDM`; optional dedicated Source.
- **Paris Musées** — GraphQL at `apicollections.parismusees.paris.fr` (auth-token; City of Paris museums, not Louvre).
- **British Museum** — collection search exists; verify image reuse terms and rate limits.

### Explicitly out of scope (for now)

- **Wikimedia Commons** — not the famous-art connector (ADR 0008).
- Scraped museum websites or closed platforms without a public API / ToS path.

## Keys

```properties
EUROPEANA_API_KEY=
HARVARD_API_KEY=
SMITHSONIAN_API_KEY=
```

Signup notes:

- **Europeana** — [account + API key](https://www.europeana.eu/en/how-to-register-for-and-manage-an-api-key); prefer `X-Api-Key` header.
- **Harvard** — [key request form](https://docs.google.com/forms/d/1Fe1H4nOhFkrLpaeBpLAnSrIMYvcAxnYWm0IU9a6IkFA/viewform); ToS is non-commercial / free-access — review before Play listing.
- **Smithsonian** — [api.data.gov/signup](https://api.data.gov/signup/); `DEMO_KEY` works briefly.

## Implementation checklist (when shipping one)

1. `shared` Source: inject `httpGet`, fixture unit tests, PD/CC0 + image-required filter.
2. Register in `ArthurApp` `ContentEngine` sources list.
3. Folder title + order in `ArthurMediaBrowse`.
4. Add `MuseumTopic` + pack cover so Control Plane Museum / Painting / Sculpture packs list the Source.
5. Mention in `CONTEXT.md` Remote Source / privacy museum list.

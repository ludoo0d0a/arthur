# TODO: French quotes API

ZenQuotes (`https://zenquotes.io/api/quotes`) is English-only. French Ambient quotes use Citation.lecog.fr as a selectable provider.

## Decision

- **French:** [Citation.lecog.fr](https://citation.lecog.fr/public/api/docs.php) — native French literary / philosophical quotes, no API key.
- **English:** ZenQuotes (existing).
- **Settings:** user chooses provider; default matches device language (`fr*` → lecog, else ZenQuotes).
- **Endpoints (lecog):**
  - Random: `GET https://citation.lecog.fr/public/api/random-quote.php`
  - Prefetch 15 single quotes into local TTL cache (no batch endpoint; stay under 100 req/h).
- **Response shape:** `{ success, data: { text, author: { forename, name }, ... } }`

## Tasks

- [x] Add `QuoteProvider` (ZenQuotes / CitationLecog)
- [x] Prefetch N random lecog quotes + map author → `Quote`
- [x] Soft-fail (`nextQuote()` → null)
- [x] Cache keyed by provider; invalidate on provider change
- [x] Persist provider in `QuoteSettings`; default by language
- [x] Settings UI (phone) + car cycle row
- [x] Unit tests
- [x] UsedApis + strings EN/FR

## Current code

- `androidApp/src/main/kotlin/fr/geoking/arthur/source/QuoteProvider.kt`
- `androidApp/src/main/kotlin/fr/geoking/arthur/source/QuoteRepository.kt`
- `androidApp/src/main/kotlin/fr/geoking/arthur/source/QuoteSettings.kt`
- `androidApp/src/test/kotlin/fr/geoking/arthur/source/QuoteRepositoryTest.kt`

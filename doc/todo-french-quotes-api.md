# TODO: French quotes API

ZenQuotes (`https://zenquotes.io/api/quotes`) is English-only. Switch Ambient quotes to a French-capable source.

## Decision

- **Preferred:** [Citation.lecog.fr](https://citation.lecog.fr/public/api/docs.php) — native French literary / philosophical quotes, no API key.
- **Endpoints:**
  - Random: `GET https://citation.lecog.fr/public/api/random-quote.php`
  - Optional category: `?category=9` (philosophie), etc.
  - Of the day: `GET .../quote-of-the-day.php`
- **Limits:** 100 req/hour/IP. No batch endpoint (unlike ZenQuotes ~50 quotes).
- **Response shape:** `{ success, data: { text, author: { forename, name }, ... } }`

## Alternatives (not primary)

| API | Notes |
| --- | --- |
| [They Said So](https://quotes.rest/qod?language=fr) | `language=fr` for QOD only; free tier limited; key for richer use |
| [Kaamelott](https://kaamelott.chaudie.re/api) | French + `GET /all` batch-friendly, but TV comedy tone |
| Quotable / ZenQuotes | English only |

## Tasks

- [ ] Replace ZenQuotes URL / parser in `QuoteRepository` with Citation.lecog.fr
- [ ] Prefetch N random quotes (loop + local cache) instead of one batch response; stay under 100 req/h
- [ ] Map `data.text` + `author.forename` / `author.name` → `Quote(text, author)`
- [ ] Keep soft-fail behavior (`nextQuote()` → null on empty / network error)
- [ ] Reuse existing TTL prefs cache (`arthur_quotes_cache`); bump or clear cache key if JSON shape changes
- [ ] Update `SOURCE_ID` / comments (`zenquotes` → `citation.lecog` or similar)
- [ ] Update unit tests in `QuoteRepositoryTest` for the new JSON shape
- [ ] Decide locale strategy: French-only, or EN ZenQuotes + FR lecog by device language
- [ ] (Optional) Attribution / link back to citation.lecog.fr if required by their terms

## Current code

- `androidApp/src/main/kotlin/fr/geoking/arthur/source/QuoteRepository.kt`
- `androidApp/src/main/kotlin/fr/geoking/arthur/source/Quote.kt`
- `androidApp/src/test/kotlin/fr/geoking/arthur/source/QuoteRepositoryTest.kt`

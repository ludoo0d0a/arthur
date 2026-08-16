# i18n and Play listings live in geoking-tools

Arthur needs DeepL app-string translation and Play Console listing automation. Scora already has the reference pipelines (`i18n/`, `scripts/playstore/`) but must not become a shared package via in-place refactor. **Copy** Translate Tooling and Play Listing Tooling into **geoking-tools**; leave Scora unchanged; Arthur and other GeoKing apps consume the tools repo (alongside existing `play-api` / `whatsnew` / bootstrap).

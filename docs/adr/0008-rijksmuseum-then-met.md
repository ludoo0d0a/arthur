# Famous-art Remote Source: Rijksmuseum first, Met next; stock photos separate

v1 needs one solid museum connector behind the Source interface, not several half-wired catalogs. **Rijksmuseum** is the first Remote Source (existing KMP samples as learning reference, not a product fork). **The Met** is the second museum connector. Wikimedia is not the famous-art connector.

Later museum connectors (same rules: open access / PD-or-CC0 + images, prefer no API key) are tracked in [`docs/roadmap-museum-sources.md`](../roadmap-museum-sources.md). **Art Institute of Chicago** and **Cleveland Museum of Art** ship as additional museum Sources.

Separately, **Pexels** and **Unsplash** are Stock Photo Sources for ambient photography — not museum/famous-art substitutes. Pexels uses `PEXELS_API_KEY`. Unsplash uses an **Access Key** (`UNSPLASH_ACCESS_KEY`, Client-ID) for public catalog search; the **Secret Key** (`UNSPLASH_SECRET_KEY`) is for OAuth only and must not be compiled into the client. Blank keys yield an empty catalog. Follow provider ToS for attribution and rate limits.

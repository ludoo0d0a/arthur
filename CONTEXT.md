# Arthur

Ambient art and photo display — name is a play on **ART'hur**. Phone is the Control Plane; Auto and TV are Canvases. Single App Shell APK. v1 Content Engine: Bundled Pack, Rijksmuseum Remote Source (Met + Pexels/Unsplash photo Source in v1.1), Genart, Fractal Presets, Premium Custom Fractal, Photo Artwork, Premium Personal Photos. RevenueCat lifetime. Apple-Ready Shared only (no Apple UI in v1). Canvas Pairing LAN/QR. **Release Spine**: geoking-tools + geoking-ci (CI/CD Play, listings/i18n, Firebase Analytics/Crashlytics, In-App Updates, arthur.geoking.fr); Scora = reference copy source only.

## Language

**Arthur**:
The product name (wordplay on ART + Arthur). Android applicationId / root package: `fr.geoking.arthur`.
_Avoid_: Untitled, Ambient Gallery (as product name)

**App Shell**:
Single Android APK (`:androidApp`) hosting Control Plane UI, Auto Canvas (Media), and TV Canvas (screensaver + ambient launcher). Internal packages for phone / auto / tv; KMP `:shared` (+ `:fractal`, `:genart`) for domain logic and generative renderers. Not separate TV/phone store APKs in v1.
_Avoid_: Multi-APK store listing for v1, separate `:tv` application module as ship artifact

**Control Plane**:
The phone app where the user chooses sources, builds playlists/rotations, configures canvases, and unlocks Premium. It is not the primary long-form viewing surface.
_Avoid_: Settings hub, companion-only app

**Canvas**:
A display surface that shows ambient Artwork under Control Plane configuration (Android Auto, Android TV, phone wallpaper / Always-On Display, future Apple surfaces).
_Avoid_: Screen, output, client (when meaning a display target)

**v1 Canvases**:
Android Auto and Android TV only. Phone is Control Plane plus preview of Artwork — not a system wallpaper or Always-On Display Canvas yet.

**v2 Canvases**:
Adds phone live wallpaper (Muzei Pattern). Always-On Display / OEM AOD-style Canvas remains later than v2 unless explicitly promoted.

**Content Engine**:
The shared catalog and rotation logic that supplies Artwork to Canvases — curated famous works, sculptures, photos, genart, and fractals.
_Avoid_: Gallery app (as the product spine), media library (too Android-Media-specific)

**Artwork**:
One displayable piece in the Content Engine: a still image, generative frame, or fractal render, with metadata (title, artist/attribution, license, category).
_Avoid_: Photo (unless user-library photo), media item, asset

**Ambient Rotation**:
The scheduled or idle sequence of Artwork shown on a Canvas (Muzei-like gentle refresh), as configured on the Control Plane.
_Avoid_: Slideshow (implies rapid user-driven paging), playlist (audio connotation)

**Custom Fractal**:
A user-authored fractal Artwork defined by tap points (and related parameters) on an AMOLED-friendly dark field, in the spirit of Realme Always-On Display fractal themes. **Premium-only** (advanced). Distinct from free **Fractal Presets**.
_Avoid_: Wallpaper theme, background effect (Julius UI chrome)

**Fractal Presets**:
Packaged / auto-animated fractal Artwork (Julius-style Mandelbrot, Julia, Burning Ship, Tricorn zoom cycles, quality settings) available on the free tier for Ambient Rotation.
_Avoid_: Custom Fractal, theme background only

**Fractal Module**:
Local `:fractal` (or equivalent) module seeded by extracting Julius phone/Auto fractal renderers (`FractalEffectCanvas` / `FractalEffectSurface`). Near-term Android Compose; then deepen by moving computation into KMP `commonMain` with platform renderers for Apple later. Not a live Git dependency on the Julius app repo.
_Avoid_: Julius submodule, rewrite-from-scratch on day one

**Premium Entitlement**:
The RevenueCat entitlement that unlocks Premium features. v1 store product = **one-time** (lifetime) purchase on Play; StoreKit later. Feature gates read the entitlement, not raw Play Billing tokens.
_Avoid_: Checking Play Billing purchase tokens directly in UI feature flags; subscription as v1-only monetization

**Apple-Ready Shared**:
v1 ships Android only (phone Control Plane, Auto Canvas, TV Canvas). Domain logic — Content Engine, Ambient Rotation, Prepared Rotation, Premium Entitlement checks, fractal math as it moves to commonMain — lives in KMP `commonMain` so iOS/tvOS can attach later. No compiled Apple UI target in v1.
_Avoid_: iOS app in v1, Compose Multiplatform iOS ship gate for v1

**Canvas Pairing**:
v1 link from Control Plane to TV Canvas via LAN and/or QR — phone pushes Prepared Rotation (and related config) to the TV without requiring a cloud account. Auto needs no pairing (same device). Payload: **manifest** (Artwork/Source ids + remote URLs) for Remote Sources; **blobs** for Personal Photos (and any Bundled Pack pieces the TV lacks). Not a live phone-as-media-server session.
_Avoid_: Cloud-only sync as v1 requirement; unpaired “TV is a separate product”; phone must stay online as streaming origin

**Translate Tooling**:
DeepL-based **app string** i18n pipeline (Scora’s `i18n/translate.sh` / `translate.py` as reference) **copied into geoking-tools** and consumed by Arthur and other GeoKing apps. Scora left unchanged.
_Avoid_: Editing Scora’s i18n to become the shared package; Crowdin as v1 path; app-local-only copy with no tools-repo home

**Play Listing Tooling**:
Play Console listing management and listing translations (Scora’s `scripts/playstore/` — e.g. `listing_cli.py`, `translate-listing.sh`, screenshot validation — as reference) **copied into geoking-tools** alongside existing release helpers (`play-api.sh`, `whatsnew.py`, bootstrap). Arthur uses geoking-tools for **fiches Play + toutes les traductions** (app strings and store listing). Scora left unchanged.
_Avoid_: Maintaining a second listing/translate stack only inside Arthur; refactoring Scora in place as the shared source of truth

**Release Spine**:
v1 path from repo to Play publication, automated via **geoking-tools** + **geoking-ci**: bootstrap (`project.manifest.json`), CI (`android-ci.yml`), CD (`release-play.yml` → internal on `main`, versioned on `v*` tags), Play Listing Tooling + Translate Tooling, RevenueCat lifetime product, Firebase **Analytics** + **Crashlytics**, Play In-App Updates, privacy/marketing page on **arthur.geoking.fr** (Cloudflare). Growth dashboard / `pull-dashboard-metrics` = **v1.1**. No AI Worker / App Check gate required for v1.
_Avoid_: Hand-rolled CI unique to Arthur; shipping without Crashlytics/Analytics; blocking v1 on growth dashboard

**Genart**:
On-device procedural generative Artwork (Compose Canvas engines in `:genart` — particles, pseudo-3D lattice, soft shadows, tunnel, tonal geometry; Julius theme family excluding Fractal Presets / Custom Fractal). Not a pre-rendered image catalog and not prompt-based AI image generation in v1.
_Avoid_: AI image gen, stock “genart” photo packs as the definition of Genart

**Muzei Pattern**:
Product/UX reference: living art on a surface, dim/recede when UI needs focus, rotate sources, optional third-party sources. Not a commitment to ship as a Muzei Art Provider unless decided later.
_Avoid_: Muzei fork, Muzei plugin (until explicitly chosen)

**Auto Canvas (Media)**:
Android Auto host integration via the **Media** app category: Ambient Rotation is exposed as a browsable media tree / queue; the head unit shows large now-playing Artwork rather than a POI grid browser.
_Avoid_: POI car app, Car App Library templates (for the Auto Canvas — not the Auto host choice)

**Gaston AA Discipline**:
Reuse Gaston’s Android Auto *operational* guidelines (docs, DHU debug, host-constraint mindset, safe patterns) while implementing Auto as Media — do not copy Gaston’s POI/Car App Library screen architecture into Arthur.
_Avoid_: Porting Gaston POI templates as the art UI; ignoring Gaston AA lessons entirely

**Prepared Rotation**:
The Control Plane act of choosing sources, filters, and order so Canvases only receive host-safe Artwork — the product meaning of “bypass photo limitations,” not circumventing platform policy.
_Avoid_: Bypass, hack, unrestricted gallery

**TV Canvas (Screensaver)**:
Android TV ambient surface: system screensaver / Dream driven by Ambient Rotation, plus a minimal leanback launcher that only starts ambient fullscreen. No Leanback collection browse in v1 — browsing stays on the Control Plane.
_Avoid_: Leanback gallery, TV browse UI (v1)

**Source**:
A feed that supplies Artwork into the Content Engine (bundled pack, remote museum/Wikimedia/Unsplash-like API, generative fractal, genart).
_Avoid_: Provider (unless meaning Muzei Art Provider), channel

**Bundled Pack**:
A small offline set of Artwork shipped with the app so Auto/TV Canvases work before any network fetch.
_Avoid_: Full catalog on device

**Remote Source**:
An external catalog synced into the Content Engine under explicit license/attribution rules. v1 first connector = **Rijksmuseum** API. v1.1 adds **The Met** as a second museum Source and a **stock-photo** Source (**Pexels** or **Unsplash**, pick one at implementation). Content Engine stays Source-agnostic behind one interface.
_Avoid_: Scraping, unrestricted web images, multi-museum launch in v1; Wikimedia/Unsplash/Pexels as the v1 famous-art connector

**Rijksmuseum Source**:
The v1 museum Remote Source implementation (API + attribution). Reference learning from existing Rijksmuseum KMP samples is fine; Arthur does not fork that app as the product.
_Avoid_: Shipping the third-party Rijksmuseum sample app as Arthur

**Stock Photo Source**:
v1.1 Remote Source for high-quality ambient photography (nature, city, abstract) via a free legal API — **Pexels** or **Unsplash**. Distinct from museum Sources and from Personal Photos. Must follow the chosen provider’s ToS (API key, attribution, rate limits, hotlink/cache rules). Not a substitute for famous-art catalogs.
_Avoid_: Scraping 500px or other closed platforms; shipping without attribution when required; treating stock photos as museum/famous-art

**Generative Source**:
Artwork produced on-device: Custom Fractal (Premium), Fractal Presets (free), and Genart. First-class in v1 alongside curated Photo Artwork and Remote Sources.
_Avoid_: Background effect, theme chrome

**Photo Artwork**:
Curated or Remote Source still photography (famous / stock / museum photos) in the Content Engine. Distinct from Personal Photos.
_Avoid_: User photo (use Personal Photos), camera roll

**Personal Photos**:
The user's own device photos (Android Photo Picker / gallery), selectable on the Control Plane into a Prepared Rotation for Auto/TV Canvases. In v1 this Source is **Premium-gated**.
_Avoid_: Photo Artwork, unrestricted Auto photo browser

**Premium**:
Paid unlock via **Premium Entitlement** (RevenueCat **lifetime** one-time in v1). **Free tier**: a small allowance of Photo Artwork / Fractal Presets, Bundled Pack, Auto/TV Canvases with that limited pool — no Custom Fractal (tap authoring). **Premium**: Personal Photos, full Remote Sources, unlimited Genart, and Custom Fractal.
_Avoid_: Pro, VIP (until a SKU id is chosen); locking Auto/TV Canvases entirely behind paywall; gating features on raw Play Billing alone; subscription-only v1

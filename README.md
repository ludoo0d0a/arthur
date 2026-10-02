# Arthur (ART'hur)

Arthur is ambient art for Android: configure rotations on your phone, then enjoy museum works, photos, genart, and fractals on **Android Auto** and **Android TV**.

Phone is the Control Plane; Auto and TV are the Canvases. One App Shell APK.

Domain language: [`CONTEXT.md`](CONTEXT.md) · Decisions: [`docs/adr/`](docs/adr/) · Genart ideas: [`docs/roadmap-genart.md`](docs/roadmap-genart.md) · Museum Sources: [`docs/roadmap-museum-sources.md`](docs/roadmap-museum-sources.md) · Street art: [`docs/roadmap-streetart-sources.md`](docs/roadmap-streetart-sources.md) · Roadmap: [`docs/roadmap.md`](docs/roadmap.md)

## Modules

| Module | Role |
|--------|------|
| `:shared` | KMP Content Engine, pairing codec, Sources |
| `:fractal` | Fractal Presets / Custom Fractal gates (Julius extract later) |
| `:genart` | Procedural Compose Canvas engines (particles, tunnel, tonal geometry, …) |
| `:androidApp` | Phone UI, Media service, Dream + ambient launcher |

## Build

```bash
export JAVA_HOME="$HOME/.sdkman/candidates/java/21.0.2-open"
./gradlew :shared:testAndroidHostTest :fractal:testDebugUnitTest :genart:testDebugUnitTest :androidApp:assembleDebug
```

## Tests

- Unit: `./gradlew :shared:testAndroidHostTest :fractal:testDebugUnitTest :genart:testDebugUnitTest`
- UI: `./gradlew :androidApp:connectedDebugAndroidTest` (emulator)
- E2E (phone): `maestro test maestro/smoke.yaml`
- Android Auto (DHU): `./scripts/debug-play-dhu.sh --logcat` — see [`docs/android-auto-dhu-debug.md`](docs/android-auto-dhu-debug.md)

## Release spine

```bash
../geoking-tools/templates/bootstrap-new-app.sh --package fr.geoking.arthur --name Arthur
```

## Privacy / web

Static site in `website/` (monorepo), published on Cloudflare Workers (`wrangler.jsonc`, no npm build). Pattern: geoking-tools skill **gk-website-sync** (Scora-style sync).

- Hosting: https://arthur.geoking.fr
- Privacy: https://arthur.geoking.fr/privacy.html
- Deploy CI: `.github/workflows/cloudflare-pages.yml` on `website/**`
- Screenshot sync CI: `.github/workflows/website-screenshots.yml`

```bash
# Roborazzi → website/assets (via geoking-tools fill_website_screenshots.py)
./gradlew generateWebsiteScreenshots -PscreenshotLocales=en,fr
# or copy-only:
./scripts/fill_website_screenshots.py

npx wrangler dev      # local preview
npx wrangler deploy   # manual ship
```

Firebase project (Analytics / Crashlytics only): `arthur-geoking`.

## Screenshots (Roborazzi)

Same stack as Scora (Robolectric + Roborazzi 1.69.0, 411×891 dp @ xxhdpi):

```bash
export JAVA_HOME="$HOME/.sdkman/candidates/java/21.0.2-open"
./gradlew :androidApp:generatePhoneScreenshots -PscreenshotLocales=en,fr
./gradlew :androidApp:generatePhoneScreenshotsFramed -PscreenshotLocales=en,fr
```

Output: `screenshots/phone/{lang}/` and `screenshots/phone/framed/{lang}/`. Map into the landing via `website/screenshot-sources.json`.

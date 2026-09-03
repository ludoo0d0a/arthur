# Arthur (ART'hur)

Ambient art for **phone** (Control Plane), **Android Auto** (Media), and **Android TV** (screensaver).

Domain language: [`CONTEXT.md`](CONTEXT.md) · Decisions: [`docs/adr/`](docs/adr/)

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
./gradlew :shared:testDebugUnitTest :fractal:testDebugUnitTest :genart:testDebugUnitTest :androidApp:assembleDebug
```

## Tests

- Unit: `./gradlew :shared:testDebugUnitTest :fractal:testDebugUnitTest :genart:testDebugUnitTest`
- UI: `./gradlew :androidApp:connectedDebugAndroidTest` (emulator)
- E2E: `maestro test maestro/smoke.yaml`

## Release spine

```bash
../geoking-tools/templates/bootstrap-new-app.sh --package fr.geoking.arthur --name Arthur
```

## Privacy / web

Static site in `website/`, published on Cloudflare (`wrangler.jsonc`, no build step). Pushes to `main` that touch `website/` trigger `.github/workflows/cloudflare-pages.yml`.

- Hosting: https://arthur.geoking.fr
- Privacy: https://arthur.geoking.fr/privacy.html
- Firebase project (Analytics / Crashlytics only): `arthur-geoking`

Local preview: `npx wrangler dev`. Manual deploy: `npx wrangler deploy`.

## Screenshots (Roborazzi)

Same stack as Scora (Robolectric + Roborazzi 1.69.0, 411×891 dp @ xxhdpi):

```bash
export JAVA_HOME="$HOME/.sdkman/candidates/java/21.0.2-open"
./gradlew :androidApp:generatePhoneScreenshots -PscreenshotLocales=en,fr
./gradlew :androidApp:generatePhoneScreenshotsFramed -PscreenshotLocales=en,fr
```

Output: `screenshots/phone/{lang}/` and `screenshots/phone/framed/{lang}/`.

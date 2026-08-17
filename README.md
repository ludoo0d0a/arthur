# Arthur (ART'hur)

Ambient art for **phone** (Control Plane), **Android Auto** (Media), and **Android TV** (screensaver).

Domain language: [`CONTEXT.md`](CONTEXT.md) · Decisions: [`docs/adr/`](docs/adr/)

## Modules

| Module | Role |
|--------|------|
| `:shared` | KMP Content Engine, pairing codec, Sources |
| `:fractal` | Fractal Presets / Custom Fractal gates (Julius extract later) |
| `:androidApp` | Phone UI, Media service, Dream + ambient launcher |

## Build

```bash
export JAVA_HOME="$HOME/.sdkman/candidates/java/21.0.2-open"
./gradlew :shared:testDebugUnitTest :fractal:testDebugUnitTest :androidApp:assembleDebug
```

## Tests

- Unit: `./gradlew :shared:testDebugUnitTest :fractal:testDebugUnitTest`
- UI: `./gradlew :androidApp:connectedDebugAndroidTest` (emulator)
- E2E: `maestro test maestro/smoke.yaml`

## Release spine

```bash
../geoking-tools/templates/bootstrap-new-app.sh --package fr.geoking.arthur --name Arthur
```

## Privacy / web

- Hosting: https://arthur-geoking.web.app
- Privacy: https://arthur-geoking.web.app/privacy.html
- Firebase project: `arthur-geoking`

## Screenshots (Roborazzi)

Same stack as Scora (Robolectric + Roborazzi 1.69.0, 411×891 dp @ xxhdpi):

```bash
export JAVA_HOME="$HOME/.sdkman/candidates/java/21.0.2-open"
./gradlew :androidApp:generatePhoneScreenshots -PscreenshotLocales=en,fr
./gradlew :androidApp:generatePhoneScreenshotsFramed -PscreenshotLocales=en,fr
```

Output: `screenshots/phone/{lang}/` and `screenshots/phone/framed/{lang}/`.

# Debugging Android Auto with DHU

How to run Arthur's Android Auto Canvas locally with the **Desktop Head Unit
(DHU)** and reproduce/fix startup crashes. Tooling lives in **geoking-tools**
(`bin/run-dhu.sh`, `bin/debug-play-dhu.sh`), ported from Gaston's AA debug
setup per **Gaston AA Discipline** (see [`CONTEXT.md`](../CONTEXT.md)) —
operational patterns only, not Gaston's POI/Car App screen architecture.

Arthur exposes two Auto surfaces, both worth exercising during a session:

- `ArthurCarAppService` / `ArthurCarSession` / `ArtworkPaneScreen` — Car App
  Library (`androidx.car.app`), `PaneTemplate` large-image display.
- `ArthurMediaService` — `MediaBrowserServiceCompat` browse tree (source
  folders → playable art) + now-playing static album art.

## 1. Prerequisites

- **Android SDK / DHU installed**: Android Studio → Settings → Appearance &
  Behavior → System Settings → Android SDK → SDK Tools → enable **Android
  Auto Desktop Head Unit emulator**.
- **Physical Android phone** with **Android Auto** installed. DHU connects
  only to real devices — the Android emulator is not supported.
- On the phone: Android Auto developer mode + "Unknown sources" / "Add new
  cars" enabled (Android Auto → About → tap version ~10× to unlock).
- `ANDROID_HOME`/`ANDROID_SDK_ROOT` set (or SDK at `~/Library/Android/sdk`),
  `adb` on `PATH`.

## 2. One-shot: build, install, run DHU

```bash
./scripts/debug-play-dhu.sh              # USB accessory mode (default)
./scripts/debug-play-dhu.sh --adb        # ADB tunneling (use when USB mode is flaky)
./scripts/debug-play-dhu.sh --no-build   # reuse the already-installed debug build
./scripts/debug-play-dhu.sh --logcat     # also capture logcat to build/dhu-logs/
```

`--logcat` is the important one when chasing a startup crash: it clears the
device log, tees `adb logcat -v threadtime` to a timestamped file under
`build/dhu-logs/`, and keeps capturing until DHU exits. Reproduce the crash,
close DHU, then search the file for `FATAL EXCEPTION` or `fr.geoking.arthur`.

Options can be combined, e.g. `./scripts/debug-play-dhu.sh --adb --logcat`.

## 3. Manual steps (if you'd rather drive it yourself)

```bash
./gradlew :androidApp:assembleFullDebug
adb install -r androidApp/build/outputs/apk/full/debug/androidApp-full-debug.apk
./scripts/run-dhu.sh            # USB accessory mode
./scripts/run-dhu.sh --adb      # ADB tunneling
./scripts/run-dhu.sh -c config/default_1080p.ini
```

Unlock the phone and open Arthur once so the system doesn't put it to sleep
before starting a session in the DHU window.

## 4. Attaching the debugger

1. Install the debug build on the phone (steps above).
2. Open Arthur once on the phone so Android Auto knows about it.
3. Start an Android Auto session in DHU — Arthur should appear under Media.
4. In Android Studio: **Run → Attach Debugger to Android Process…**, select
   the phone as target device and pick the `fr.geoking.arthur` process.
5. Breakpoints in `ArthurCarAppService`, `ArthurCarSession`,
   `ArtworkPaneScreen` (Car App Library screen) or `ArthurMediaService`
   (`onCreate`, `onGetRoot`, `onLoadChildren`) will now be hit.

## 5. Automated regression coverage (no device needed)

`androidApp/src/test/kotlin/fr/geoking/arthur/auto/` has Robolectric tests
that boot the real Koin graph (via `ArthurApp`, same as on-device startup)
and exercise `ArthurCarAppService`/`ArthurMediaService` without a phone —
run them on every change:

```bash
./gradlew :androidApp:testFullDebugUnitTest --tests "fr.geoking.arthur.auto.*"
```

These catch DI-wiring and template-building regressions; they don't replace
a real DHU session for host-constraint issues (row/action limits, template
validation) which only surface against the real Android Auto host.

## 6. Phone e2e (Maestro)

Android Auto's DHU surface isn't drivable by Maestro (it renders outside the
phone's own view hierarchy), but the Control Plane (phone) flow is:

```bash
maestro test maestro/smoke.yaml
```

Run this alongside a DHU session when a change touches both the Control
Plane and a Canvas (e.g. rotation settings, source toggles).

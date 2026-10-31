# Android TV launcher assets (Play TV-LB / TV-BN)

Play rejects TV form-factor builds that lack a **bitmap** home-screen banner in
`drawable-xhdpi` and/or ship a padded launcher icon.

## Required in-APK resources

| Asset | Path | Size | Notes |
|-------|------|------|-------|
| Banner | `androidApp/src/main/res/drawable-xhdpi/ic_banner.png` | **320×180** | Opaque RGB; **must include the app name** (TV-BN). Manifest: `android:banner="@drawable/ic_banner"`. |
| Icon | `mipmap-*/ic_launcher.png` (+ round) | TV sizes (xhdpi **160×160**) | Logo fills the tile edge-to-edge; no letterboxing. |

Higher-density banner PNGs (`xxhdpi` / `xxxhdpi`) are also generated for sharpness.

Do **not** use a vector-only `drawable/ic_banner.xml` — review tooling looks for the
xhdpi PNG.

## Regenerate on demand

```bash
./scripts/gen-tv-assets.sh
# or:
python3 scripts/playstore/gen_tv_launcher_assets.py
```

Also refreshes `playstore/icon-512.png` and `playstore/tv-banner.png` (store listing).

```bash
./scripts/gen-tv-assets.sh --skip-playstore   # APK res only
./scripts/gen-tv-assets.sh --name Arthur      # banner title (TV-BN)
```

## Refs

- [Provide a home screen icon and banner](https://developer.android.com/training/tv/get-started/create#banner)
- [TV app quality — TV-LB / TV-BN](https://developer.android.com/docs/quality-guidelines/tv-app-quality)
- [TV app icon design guidelines](https://developer.android.com/design/ui/tv/guides/system/tv-app-icon-guidelines)

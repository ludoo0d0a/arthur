## Ad banner (AdMob) setup

Arthur shows an **AdMob banner** on the **phone Control Plane** for the **`playstore`** flavor only. Premium users never see ads (`PremiumEntitlement.isPremium`).

TV Ambient / Dream / Auto stay ad-free. TV may use a first-party Premium house promo later; AdMob is not used on TV (unsupported by Google Mobile Ads).

### What’s implemented

- **SDK**: Google Mobile Ads (`com.google.android.gms:play-services-ads`)
- **Init**: `MobileAds.initialize(...)` in `ArthurApp` when `BuildConfig.IS_PLAYSTORE_DISTRIBUTION`
- **UI**: `AdMobBanner` in `PhoneControlPlaneContent` `Scaffold.bottomBar`
- **Gate**: `PremiumEntitlement` (RevenueCat + debug simulate-premium)

### Required configuration

Add to uncommitted `local.properties` (or CI env):

```properties
# AdMob
ADMOB_APP_ID=ca-app-pub-xxxxxxxxxxxxxxxx~yyyyyyyyyy
ADMOB_BANNER_UNIT_ID=ca-app-pub-xxxxxxxxxxxxxxxx/zzzzzzzzzz
```

### Test IDs (defaults)

If unset, Google official test IDs are used:

- App ID: `ca-app-pub-3940256099942544~3347511713`
- Banner: `ca-app-pub-3940256099942544/6300978111`

### Play Console

- App declares **Contains ads: Yes** (`scripts/project.manifest.json`)
- Update Data safety for AdMob when shipping production unit IDs
- Future: IMA / Ad Manager VAST for CTV fill — not in this app yet

### Monetization model (summary)

- **Free**: capped content + phone banner
- **Premium** (lifetime): no ads + UX extras — does **not** unlock content packs
- **Packs / Diamond** (lifetime): content unlocks — separate from Premium
- **No subscription** in v1; Personal Photos SKU stays hidden until product-ready

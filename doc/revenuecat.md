# Arthur × RevenueCat catalog

Source of truth in code: `MarketplaceCatalog`, `GenartPackTopics`, `AudioPackCatalog`, `RevenueCatPremiumEntitlement`.

All v1 products are **one-time (lifetime) Play IAPs**, not subscriptions. Premium unlocks **global UX only** (no ads, favorites); packs unlock content. Premium does **not** grant packs. **Diamond** unlocks **all content packs** once-for-all; it does **not** grant Premium UX. Phone free tier shows AdMob banners (`docs/ADS_SETUP.md`). Marketplace home surfaces Diamond + curated featured packs; Personal Photos stays out of the storefront until `FeatureFlag.personalPhotosEnabled`.

**Prix FR** = prix catalogue proposé pour Google Play **France** (EUR TTC affiché à l’utilisateur). À saisir dans Play Console (option d’achat régionale `FR`) puis synchroniser avec RevenueCat. Ajustable avant prod.

RevenueCat project: **Arthur** (`projd7c31aba`) · Play app: **Arthur (Play Store)** (`app149d8ba719`, `fr.geoking.arthur`).

### Grille tarifaire (France)

| Tier | Prix FR | Usage |
|------|--------:|-------|
| A | **0,99 €** | Packs audio ambiance / thématiques simples |
| B | **1,99 €** | Packs Genart topic standards ; audio « instrument / style » |
| C | **2,99 €** | Personal Photos ; Genart fractal / custom ; audio neural / outils |
| D | **4,99 €** | Premium lifetime (UX) |
| E | **29,99 €** | Diamond — tous les packs contenu |

## Premium (UX)

| Pack id | Entitlement | Play product | Prix FR | RC | Play |
|---------|-------------|--------------|--------:|:--:|:----:|
| — | `premium` | `arthur_premium_lifetime` | **4,99 €** | done | done |

## Diamond (bundle)

| Pack id | Entitlement | Play product | Prix FR | RC | Play |
|---------|-------------|--------------|--------:|:--:|:----:|
| `diamond` | `pack_diamond` | `arthur_pack_diamond` | **29,99 €** | done | done |

Owns Diamond ⇒ `PackOwnership.owns(any content pack)` is true. Does **not** set `premium`.

## Marketplace packs

### Personal

| Pack id | Entitlement | Play product | Prix FR | RC | Play |
|---------|-------------|--------------|--------:|:--:|:----:|
| `personal_photos` | `pack_personal_photos` | `arthur_pack_personal_photos` | **2,99 €** | TODO | TODO |

### Genart topics (storefront)

Pattern: pack id `genart_<suffix>` · entitlement `pack_genart_<suffix>` · Play / RC store id `arthur_pack_genart_<suffix>` · type **one_time** (lifetime unlock).

Active packs offered in-app (~20):

| Pack id | UI label (FR) | Entitlement | Play product | Prix FR | Notes |
|---------|---------------|-------------|--------------|--------:|-------|
| `genart_nature` | Nature | `pack_genart_nature` | `arthur_pack_genart_nature` | **1,99 €** | + botanical, seasons, coast, micro |
| `genart_weather` | Météo | `pack_genart_weather` | `arthur_pack_genart_weather` | **1,99 €** | + storm_strobe, brumes |
| `genart_water` | Eau | `pack_genart_water` | `arthur_pack_genart_water` | **1,99 €** | |
| `genart_earth` | Terre | `pack_genart_earth` | `arthur_pack_genart_earth` | **1,99 €** | |
| `genart_planets` | Espace | `pack_genart_planets` | `arthur_pack_genart_planets` | **1,99 €** | + scifi |
| `genart_vintage` | Jeux vintage | `pack_genart_vintage` | `arthur_pack_genart_vintage` | **1,99 €** | + us_vintage |
| `genart_abstract` | Abstrait | `pack_genart_abstract` | `arthur_pack_genart_abstract` | **1,99 €** | |
| `genart_geometry` | Géométrie | `pack_genart_geometry` | `arthur_pack_genart_geometry` | **1,99 €** | |
| `genart_illusions` | Illusions | `pack_genart_illusions` | `arthur_pack_genart_illusions` | **1,99 €** | |
| `genart_animals` | Animaux | `pack_genart_animals` | `arthur_pack_genart_animals` | **1,99 €** | + life |
| `genart_light` | Lumière | `pack_genart_light` | `arthur_pack_genart_light` | **1,99 €** | + night |
| `genart_material` | Matière | `pack_genart_material` | `arthur_pack_genart_material` | **1,99 €** | + crystal, ink |
| `genart_interior` | Intérieur | `pack_genart_interior` | `arthur_pack_genart_interior` | **1,99 €** | |
| `genart_fire` | Feu | `pack_genart_fire` | `arthur_pack_genart_fire` | **1,99 €** | |
| `genart_zen` | Zen | `pack_genart_zen` | `arthur_pack_genart_zen` | **1,99 €** | + kaleido |
| `genart_dream` | Rêve | `pack_genart_dream` | `arthur_pack_genart_dream` | **1,99 €** | |
| `genart_auto` | Voyages | `pack_genart_auto` | `arthur_pack_genart_auto` | **1,99 €** | + moto, avion, velo, train |
| `genart_physarum` | Organique | `pack_genart_physarum` | `arthur_pack_genart_physarum` | **1,99 €** | + reactions |
| `genart_fractal` | Fractale | `pack_genart_fractal` | `arthur_pack_genart_fractal` | **2,99 €** | |
| `genart_custom` | Perso | `pack_genart_custom` | `arthur_pack_genart_custom` | **2,99 €** | |

No legacy / retired SKU aliases in-app. Only the active packs above appear in `MarketplaceCatalog.contentPacks()`.

**RevenueCat / Play:** genart one-time IAPs created 2026-11-22; storefront later reduced to the table above.

Free Genart engines (no purchase): `particles`, `pseudo3d`, `soft_shadows`, `tunnel`, `tonal_geometry`, `sphere`, `waves`, `micro`.

### Audio / music

| Pack id | Entitlement | Play product | Prix FR | RC | Play |
|---------|-------------|--------------|--------:|:--:|:----:|
| `audio_jazz_after_dark` | `pack_audio_jazz_after_dark` | `arthur_pack_audio_jazz_after_dark` | **0,99 €** | TODO | TODO |
| `audio_world_pulse` | `pack_audio_world_pulse` | `arthur_pack_audio_world_pulse` | **0,99 €** | TODO | TODO |
| `audio_wind_garden` | `pack_audio_wind_garden` | `arthur_pack_audio_wind_garden` | **0,99 €** | TODO | TODO |
| `audio_cosmic_drift` | `pack_audio_cosmic_drift` | `arthur_pack_audio_cosmic_drift` | **0,99 €** | TODO | TODO |
| `audio_hearth_weather` | `pack_audio_hearth_weather` | `arthur_pack_audio_hearth_weather` | **0,99 €** | TODO | TODO |
| `audio_dawn_chorus` | `pack_audio_dawn_chorus` | `arthur_pack_audio_dawn_chorus` | **0,99 €** | TODO | TODO |
| `audio_temple_resonance` | `pack_audio_temple_resonance` | `arthur_pack_audio_temple_resonance` | **0,99 €** | TODO | TODO |
| `audio_salon_classique` | `pack_audio_salon_classique` | `arthur_pack_audio_salon_classique` | **1,99 €** | TODO | TODO |
| `audio_grand_orchestra` | `pack_audio_grand_orchestra` | `arthur_pack_audio_grand_orchestra` | **1,99 €** | TODO | TODO |
| `audio_solo_violin` | `pack_audio_solo_violin` | `arthur_pack_audio_solo_violin` | **1,99 €** | TODO | TODO |
| `audio_rock_ballad` | `pack_audio_rock_ballad` | `arthur_pack_audio_rock_ballad` | **1,99 €** | TODO | TODO |
| `audio_bass_only` | `pack_audio_bass_only` | `arthur_pack_audio_bass_only` | **0,99 €** | TODO | TODO |
| `audio_midnight_ballad` | `pack_audio_midnight_ballad` | `arthur_pack_audio_midnight_ballad` | **1,99 €** | TODO | TODO |
| `audio_hawaiian_breeze` | `pack_audio_hawaiian_breeze` | `arthur_pack_audio_hawaiian_breeze` | **0,99 €** | TODO | TODO |
| `audio_arcade_chips` | `pack_audio_arcade_chips` | `arthur_pack_audio_arcade_chips` | **0,99 €** | TODO | TODO |
| `audio_atmospheres_nature` | `pack_audio_atmospheres_nature` | `arthur_pack_audio_atmospheres_nature` | **0,99 €** | TODO | TODO |
| `audio_museums_classical` | `pack_audio_museums_classical` | `arthur_pack_audio_museums_classical` | **1,99 €** | TODO | TODO |
| `audio_genart_digital` | `pack_audio_genart_digital` | `arthur_pack_audio_genart_digital` | **0,99 €** | TODO | TODO |
| `audio_stem` | `pack_audio_stem` | `arthur_pack_audio_stem` | **2,99 €** | TODO | TODO |
| `audio_wavetable` | `pack_audio_wavetable` | `arthur_pack_audio_wavetable` | **2,99 €** | TODO | TODO |
| `audio_executorch` | `pack_audio_executorch` | `arthur_pack_audio_executorch` | **2,99 €** | TODO | TODO |
| `audio_downloaded_music` | `pack_audio_downloaded_music` | `arthur_pack_audio_downloaded_music` | **2,99 €** | TODO | TODO |

Free music styles (no purchase): `jazz_piano`, `zen`, `soft_guitar`.

## Counts & panier FR

| Kind | Count | Prix unitaire typique |
|------|------:|----------------------:|
| Premium | 1 | 4,99 € |
| Diamond | 1 | 29,99 € |
| Personal | 1 | 2,99 € |
| Genart | 37 | 1,99 € (35) / 2,99 € (2) |
| Audio | 22 | 0,99 € (13) / 1,99 € (5) / 2,99 € (4) |
| **Content packs (excl. Diamond)** | **60** | |
| **Sellable packs (incl. Diamond)** | **61** | |
| **Products total (incl. Premium)** | **62** | |

Somme à l’unité (60 content packs, hors Premium / Diamond) ≈ **113,40 €**.  
Diamond **29,99 €** ≈ −74 % vs achat unitaire.  
Premium + Diamond = **34,98 €** pour UX + tout le contenu.

## Play Console checklist

Create each product as a **managed / one-time** IAP (not subscription). Price FR = column above. Product id must match **exactly**.

### Genart (37) — RC + Play done (2026-11-22)

RC store-state plans applied all 37 SKUs to Google Play (FR base price + equalize). Spot-check ACTIVE / sold in FR in Play Console; no manual create needed for genart.

### Still TODO in Play (+ RC)

```
arthur_pack_personal_photos        2,99 €
arthur_pack_audio_*               (22 SKUs — see audio table)
```

### Premium + Diamond — RC + Play done (2026-11-22)

Plan `3442c050-907c-4bb0-a56b-93caa539e4c6` applied. Entitlements: `premium` ↔ `arthur_premium_lifetime`; `pack_diamond` ↔ `arthur_pack_diamond` only (app expands Diamond ownership in `PackOwnership`).

## Dashboard checklist

1. **Play Console genart / Premium / Diamond:** done via RC store-state push (2026-11-22). Confirm products ACTIVE.
2. **Play Console remaining:** create Personal / audio one-time products (or push via RC store-state plans the same way).
3. **RevenueCat genart / Premium / Diamond:** already created + entitlements attached; Play `store_identifier`s should resolve.
4. **RevenueCat remaining:** create Personal / audio products + entitlements (`pack_*` ↔ `arthur_pack_*`).
5. Diamond: attach **only** `pack_diamond` to `arthur_pack_diamond` — do **not** attach every pack entitlement to Diamond (app expands ownership in `PackOwnership`). Done.
6. Add packages to the **current** offering (app lookup is by `StoreProduct.id` / product id).
7. SDK Android public API key → `REVENUECAT_API_KEY` / `BuildConfig.REVENUECAT_API_KEY`.

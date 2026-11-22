# Arthur × RevenueCat catalog

Source of truth in code: `MarketplaceCatalog`, `GenartPackTopics`, `AudioPackCatalog`, `RevenueCatPremiumEntitlement`.

All v1 products are **one-time (lifetime) Play IAPs**, not subscriptions. Premium unlocks **global UX only**; packs unlock content. Premium does **not** grant packs. **Diamond** unlocks **all content packs** once-for-all; it does **not** grant Premium UX.

**Prix FR** = prix catalogue proposé pour Google Play **France** (EUR TTC affiché à l’utilisateur). À saisir dans Play Console (option d’achat régionale `FR`) puis importer dans RevenueCat. Ajustable avant prod.

### Grille tarifaire (France)

| Tier | Prix FR | Usage |
|------|--------:|-------|
| A | **0,99 €** | Packs audio ambiance / thématiques simples |
| B | **1,99 €** | Packs Genart topic standards ; audio « instrument / style » |
| C | **2,99 €** | Personal Photos ; Genart fractal / custom ; audio neural / outils |
| D | **4,99 €** | Premium lifetime (UX) |
| E | **29,99 €** | Diamond — tous les packs contenu |

## Premium (UX)

| Pack id | Entitlement | Play product | Prix FR |
|---------|-------------|--------------|--------:|
| — | `premium` | `arthur_premium_lifetime` | **4,99 €** |

## Diamond (bundle)

| Pack id | Entitlement | Play product | Prix FR |
|---------|-------------|--------------|--------:|
| `diamond` | `pack_diamond` | `arthur_pack_diamond` | **29,99 €** |

Owns Diamond ⇒ `PackOwnership.owns(any content pack)` is true. Does **not** set `premium`.

## Marketplace packs

### Personal

| Pack id | Entitlement | Play product | Prix FR |
|---------|-------------|--------------|--------:|
| `personal_photos` | `pack_personal_photos` | `arthur_pack_personal_photos` | **2,99 €** |

### Genart topics

| Pack id | Entitlement | Play product | Prix FR |
|---------|-------------|--------------|--------:|
| `genart_nature` | `pack_genart_nature` | `arthur_pack_genart_nature` | **1,99 €** |
| `genart_weather` | `pack_genart_weather` | `arthur_pack_genart_weather` | **1,99 €** |
| `genart_water` | `pack_genart_water` | `arthur_pack_genart_water` | **1,99 €** |
| `genart_life` | `pack_genart_life` | `arthur_pack_genart_life` | **1,99 €** |
| `genart_earth` | `pack_genart_earth` | `arthur_pack_genart_earth` | **1,99 €** |
| `genart_planets` | `pack_genart_planets` | `arthur_pack_genart_planets` | **1,99 €** |
| `genart_scifi` | `pack_genart_scifi` | `arthur_pack_genart_scifi` | **1,99 €** |
| `genart_vintage` | `pack_genart_vintage` | `arthur_pack_genart_vintage` | **1,99 €** |
| `genart_abstract` | `pack_genart_abstract` | `arthur_pack_genart_abstract` | **1,99 €** |

Former wallpaper topic SKU `genart_tapet` is retired from the catalog; existing buyers still unlock Abstract via `LEGACY_GENART_WALLPAPER_PACK_ID`.
| `genart_geometry` | `pack_genart_geometry` | `arthur_pack_genart_geometry` | **1,99 €** |
| `genart_fractal` | `pack_genart_fractal` | `arthur_pack_genart_fractal` | **2,99 €** |
| `genart_custom` | `pack_genart_custom` | `arthur_pack_genart_custom` | **2,99 €** |

Free Genart engines (no purchase): `particles`, `pseudo3d`, `soft_shadows`, `tunnel`, `tonal_geometry`, `sphere`, `waves`, `micro`.

### Audio / music

| Pack id | Entitlement | Play product | Prix FR |
|---------|-------------|--------------|--------:|
| `audio_jazz_after_dark` | `pack_audio_jazz_after_dark` | `arthur_pack_audio_jazz_after_dark` | **0,99 €** |
| `audio_world_pulse` | `pack_audio_world_pulse` | `arthur_pack_audio_world_pulse` | **0,99 €** |
| `audio_wind_garden` | `pack_audio_wind_garden` | `arthur_pack_audio_wind_garden` | **0,99 €** |
| `audio_cosmic_drift` | `pack_audio_cosmic_drift` | `arthur_pack_audio_cosmic_drift` | **0,99 €** |
| `audio_hearth_weather` | `pack_audio_hearth_weather` | `arthur_pack_audio_hearth_weather` | **0,99 €** |
| `audio_dawn_chorus` | `pack_audio_dawn_chorus` | `arthur_pack_audio_dawn_chorus` | **0,99 €** |
| `audio_temple_resonance` | `pack_audio_temple_resonance` | `arthur_pack_audio_temple_resonance` | **0,99 €** |
| `audio_salon_classique` | `pack_audio_salon_classique` | `arthur_pack_audio_salon_classique` | **1,99 €** |
| `audio_grand_orchestra` | `pack_audio_grand_orchestra` | `arthur_pack_audio_grand_orchestra` | **1,99 €** |
| `audio_solo_violin` | `pack_audio_solo_violin` | `arthur_pack_audio_solo_violin` | **1,99 €** |
| `audio_rock_ballad` | `pack_audio_rock_ballad` | `arthur_pack_audio_rock_ballad` | **1,99 €** |
| `audio_bass_only` | `pack_audio_bass_only` | `arthur_pack_audio_bass_only` | **0,99 €** |
| `audio_midnight_ballad` | `pack_audio_midnight_ballad` | `arthur_pack_audio_midnight_ballad` | **1,99 €** |
| `audio_hawaiian_breeze` | `pack_audio_hawaiian_breeze` | `arthur_pack_audio_hawaiian_breeze` | **0,99 €** |
| `audio_arcade_chips` | `pack_audio_arcade_chips` | `arthur_pack_audio_arcade_chips` | **0,99 €** |
| `audio_atmospheres_nature` | `pack_audio_atmospheres_nature` | `arthur_pack_audio_atmospheres_nature` | **0,99 €** |
| `audio_museums_classical` | `pack_audio_museums_classical` | `arthur_pack_audio_museums_classical` | **1,99 €** |
| `audio_genart_digital` | `pack_audio_genart_digital` | `arthur_pack_audio_genart_digital` | **0,99 €** |
| `audio_stem` | `pack_audio_stem` | `arthur_pack_audio_stem` | **2,99 €** |
| `audio_wavetable` | `pack_audio_wavetable` | `arthur_pack_audio_wavetable` | **2,99 €** |
| `audio_executorch` | `pack_audio_executorch` | `arthur_pack_audio_executorch` | **2,99 €** |
| `audio_downloaded_music` | `pack_audio_downloaded_music` | `arthur_pack_audio_downloaded_music` | **2,99 €** |

Free music styles (no purchase): `jazz_piano`, `zen`, `soft_guitar`.

## Counts & panier FR

| Kind | Count | Prix unitaire typique |
|------|------:|----------------------:|
| Premium | 1 | 4,99 € |
| Diamond | 1 | 29,99 € |
| Personal | 1 | 2,99 € |
| Genart | 13 | 1,99 € (11) / 2,99 € (2) |
| Audio | 22 | 0,99 € (13) / 1,99 € (5) / 2,99 € (4) |
| **Content packs (excl. Diamond)** | **36** | |
| **Sellable packs (incl. Diamond)** | **37** | |
| **Products total (incl. Premium)** | **38** | |

Somme à l’unité (36 content packs, hors Premium / Diamond) ≈ **69,64 €**.  
Diamond **29,99 €** ≈ −57 % vs achat unitaire.  
Premium + Diamond = **34,98 €** pour UX + tout le contenu.

## Dashboard checklist

1. Create each Play one-time product with the **Play product** id above (incl. `arthur_pack_diamond`).
2. Set regional price **France** to the **Prix FR** column (Play charm tiers).
3. Import products into RevenueCat; attach the matching **Entitlement** (`pack_diamond` → Diamond product only; do not attach all pack entitlements to Diamond — the app expands ownership in `PackOwnership`).
4. Add packages to the **current** offering (lookup in app is by `StoreProduct.id` / product id).
5. SDK Android public API key → `REVENUECAT_API_KEY` / `BuildConfig.REVENUECAT_API_KEY`.

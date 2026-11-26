---
name: genart-upgrade
description: >-
  Use this skill when upgrading, improving, or polishing a genart engine's visual
  quality in the Arthur project. Covers the full workflow: audit → improve engine
  + still → bake before/after screenshots → record animation video → generate
  preview icon → write improvement notes. Includes guidelines for realistic
  vector-shape animals/objects, lighting, depth, animation smoothness, color
  palettes, and the before/after pipeline. Activate when the user asks to improve
  genart visuals, add wow-factor, or polish engines.
---

# Genart Upgrade — Visual Quality Improvement Skill

Systematic pipeline for upgrading Arthur genart engines from "working" to "wow".

## Overview

Each genart engine has two Kotlin files:
- **Engine** (`engines/<Name>Engine.kt`): live Compose Canvas animation
- **Still** (`stills/<Name>Still.kt`): frozen `android.graphics` frame for Auto album art / preview grid

Both must be improved **together** and stay visually coherent.

---

## Pipeline — Step by Step

### 1. Select Engine to Upgrade

Pick from `GenartEngineId` entries in
[GenartCatalog.kt](file:///Users/ludovic/dev/android/arthur/genart/src/main/kotlin/fr/geoking/arthur/genart/GenartCatalog.kt).

Priority tiers (upgrade order):
1. **Animals/Objects** (Cat, Dog, Panda, Bird, Fish, Flower, Tree, Aquarium, etc.) — highest impact, needs vector reference shapes
2. **Nature/Atmospheric** (Aurora, Storm, Ocean, Nebula, Dunes, etc.)
3. **Abstract/Mathematical** (Voronoi, Marble, Silk, Kaleidoscope, etc.)
4. **Scene/Vehicle** (Auto, Moto, Avion, Train, Velo packs)

### 2. Bake "Before" Artifacts

```bash
# From project root:
./scripts/genart-ollama/bake-previews.sh before <EngineName>
# Produces:
#   ~/arthur-genart-ollama/previews/before/stills/<slug>.png   (screenshot)
#   ~/arthur-genart-ollama/previews/before/videos/<slug>.mp4   (animation)
```

If the pipeline is not set up, generate artifacts manually:
```bash
./gradlew :androidApp:generateGenartScreenshots -PgenartScreenshotSize=720
./gradlew :androidApp:generateGenartPreviewFrames \
  -PgenartVideoSize=720 -PgenartVideoFps=12 -PgenartVideoSeconds=4 \
  -PgenartVideoEngines=<slug>
```

### 3. Audit Current Quality

Score on 4 axes (1–10):
- **Ambient** — calm, loopable, no flash, car-safe
- **Beauty** — visual impact, color harmony, composition
- **Performance** — density adapts to `GenartQuality.Low/Medium/High`
- **Motion** — smooth loop, no seam, natural speed

Identify:
- Bugs (loop seams, palette ignored, speed parameter unused, still ≠ live)
- Top 5 concrete improvements

### 4. Improve the Engine + Still

Apply these quality upgrades (checklist):

#### Universal Improvements
- [ ] **Depth layers** — 2–3 layers (far: soft/blurred, near: crisp)
- [ ] **Motion richness** — shared `loopedFbm` gust/drift, breath animation, parallax
- [ ] **Lighting** — soft radial glow, ambient gradients, anti-aliased paint + dither
- [ ] **Palette respect** — use `TonalPalette.pick(paletteColors, index)` and `brightness`
- [ ] **isActive dim** — reduce intensity/speed when `isActive = false`
- [ ] **Quality scaling** — `qualityCount(quality, low, med, high)` for particle counts
- [ ] **Loop seamless** — use `phase01(anim)` + `loopedFbm` (never raw time)
- [ ] **Vignette + contrast** — `GenartStillFx.polish()` applied by renderer; engine adds scene vignette
- [ ] **Still ↔ Live coherence** — same scene composition, colors, element positions

#### For Animals / Objects (Cat, Dog, Panda, Bird, Fish, Flower, etc.)

> [!IMPORTANT]
> Animal and object shapes must look recognizably like their real counterparts,
> not generic circles/ovals. Use **vector path outlines** that capture the
> characteristic silhouette.

See [Vector Shape Reference](./references/vector-shapes.md) for detailed path
construction guidelines.

**Technique**:
1. Find an SVG reference of the animal/object (simple silhouette style)
2. Translate the SVG path commands into `android.graphics.Path` / Compose `Path`
3. Use `cubicTo`/`quadraticTo` for organic curves (never approximate with circles)
4. Add anatomical details: for cats → pointed ears, curved back, tail arc;
   for dogs → floppy ears, snout, tail wag; for birds → wing chevron with body mass
5. Use `Shader.TileMode.CLAMP` radial gradients for fur/feather texture
6. Add subtle shadows under the body (soft `BlurMaskFilter`)
7. Eye glint / highlight dot for life

**Current vs Target**:
- ❌ `drawCircle` for head + `drawOval` for body = blob
- ✅ `Path` with bezier curves following real anatomy = recognizable animal

#### For Nature Scenes
- Atmospheric perspective (distant elements: lighter, bluer, blurred)
- Particle variety (not all same size/opacity)
- Volumetric light shafts (radial gradient strips)
- Subtle color temperature shifts (warm foreground, cool background)

#### For Abstract/Mathematical
- Higher harmonic complexity (more octaves in noise)
- Color cycling through palette (not just 2 colors)
- Depth via scale perspective (smaller = farther)
- Specular highlights / metallic reflections

### 5. Bake "After" Artifacts

```bash
./scripts/genart-ollama/bake-previews.sh after <EngineName>
# Produces after screenshots + videos
./scripts/genart-ollama/compare-previews.sh <EngineName>
# Produces side-by-side comparison video
```

### 6. Regenerate Preview Icon

The preview icon is the still PNG baked at screenshot resolution.
After improving the still, regenerate:

```bash
./gradlew :androidApp:generateGenartScreenshots -PgenartScreenshotSize=1080
```

The output lands in `screenshots/#N-Title.png`.

### 7. Write Improvement Notes

For each upgraded engine, produce a short improvement note (3–5 bullet points, FR):

```markdown
## <Engine Name>
- Silhouette réaliste via courbes de Bézier (chat avec oreilles pointues, dos courbé)
- Profondeur 3 couches : arrière-plan flou, sujet net, particules au premier plan
- Éclairage volumétrique doux avec dégradé radial
- Animation de respiration + balancement de queue synchronisés
- Still cohérent avec le live (même composition, couleurs, pose)
```

### 8. Run Tests

```bash
# Unit tests — catalog coverage, stable ids, stable titles
./gradlew :genart:testDebugUnitTest

# Visual regression — compare before/after screenshots
./scripts/genart-ollama/compare-previews.sh
```

---

## Architecture Reference

### Key Files
| File | Role |
|------|------|
| [GenartCatalog.kt](file:///Users/ludovic/dev/android/arthur/genart/src/main/kotlin/fr/geoking/arthur/genart/GenartCatalog.kt) | Engine enum + catalog entries |
| [GenartRegistry.kt](file:///Users/ludovic/dev/android/arthur/genart/src/main/kotlin/fr/geoking/arthur/genart/GenartRegistry.kt) | Descriptor registration (render + renderStill) |
| [GenartEffectCanvas.kt](file:///Users/ludovic/dev/android/arthur/genart/src/main/kotlin/fr/geoking/arthur/genart/GenartEffectCanvas.kt) | Live Compose wrapper (brightness, speed, vignette, quality scaling) |
| [GenartStillRenderer.kt](file:///Users/ludovic/dev/android/arthur/genart/src/main/kotlin/fr/geoking/arthur/genart/GenartStillRenderer.kt) | Still baking (draw + drawFrame + fallback) |
| [GenartStillFx.kt](file:///Users/ludovic/dev/android/arthur/genart/src/main/kotlin/fr/geoking/arthur/genart/GenartStillFx.kt) | Polish effects (vignette, contrast, scatter, glow) |
| [GenartPerf.kt](file:///Users/ludovic/dev/android/arthur/genart/src/main/kotlin/fr/geoking/arthur/genart/GenartPerf.kt) | Render scale + quality blur |
| [GenartCatalogTest.kt](file:///Users/ludovic/dev/android/arthur/genart/src/test/kotlin/fr/geoking/arthur/genart/GenartCatalogTest.kt) | Catalog coverage + stable id/title tests |

### Helper Functions (from NoiseUtils / TonalPalette)
- `phase01(anim: Float)`: normalized 0–1 loop phase
- `loopedFbm(t, radius, octaves, seedOffset)`: seamless noise loop
- `qualityCount(quality, low, medium, high)`: particle count per quality tier
- `seededRange(seed, min, max)` / `seededUnit(seed)`: deterministic random
- `TonalPalette.pick(colors, index)`: palette color by index
- `TonalPalette.mix(a, b, t)`: color lerp
- `TonalPalette.brightness(color, factor)`: brightness adjust
- `TonalPalette.withAlpha(color, alpha)`: alpha adjust

### Engine Contract (signatures that must NOT change)
```kotlin
// Live engine
@Composable fun <Name>Engine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier,
)

// Still baker
internal object <Name>Still {
    fun draw(
        canvas: Canvas,
        size: Int,
        generation: Long,
        phase: Float,
        rotationDeg: Float,
        pulse: Float,
        palette: AnimationPalette,
    )
}
```

### Existing Pipeline Scripts
| Script | Purpose |
|--------|---------|
| `scripts/genart-ollama/bake-previews.sh` | Bake stills + frames → encode videos |
| `scripts/genart-ollama/encode-preview-videos.sh` | Frames → MP4 |
| `scripts/genart-ollama/compare-previews.sh` | Side-by-side before/after MP4 |
| `scripts/genart-ollama/run-one.sh` | Full audit → fix → bake cycle for one engine |
| `scripts/genart-ollama/run-batch.sh` | Batch process engines by pack |

---

## Quality Targets

### Ambient Score ≥ 8/10
- Calm, hypnotic, no jarring transitions
- Seamless 24s+ loop cycle
- Car-safe (no strobe, no flash)

### Beauty Score ≥ 8/10
- Rich color palette (3+ colors from palette)
- Depth and atmosphere (2–3 visual layers)
- Lighting that draws the eye (radial glow, light shafts)
- For animals/objects: **immediately recognizable** silhouette

### Motion Score ≥ 8/10
- Smooth easing (no linear jitter)
- Multi-layered animation (breath + sway + drift + particle flow)
- `speed` parameter respected
- `isActive` dampening natural

### Perf Score ≥ 7/10
- Low quality: ≤ 30 particles, no blur passes
- Medium quality: moderate detail, lighter blur
- High quality: full detail, full blur
- 60fps on mid-range Android (Pixel 5 tier)

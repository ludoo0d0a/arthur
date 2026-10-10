# Genart

On-device procedural Ambient loops (Compose Canvas in `:genart`). Not AI image generation, not stock photo packs.

**Goal:** simple, satisfying, relaxing loops for phone preview, Auto album art, and TV Dream.

Canonical catalog: `GenartRegistry` / `GenartSource` (stable `genart.*` ids).

## Principles

- Slow motion, soft loops, low cognitive load.
- No sudden flashes or high-contrast flicker (car-safe).
- **Live creatures** = silhouettes, flocks, schools, tiny agents — not detailed anatomy sims.

## Status

**Backlog cleared.** Original category list, Round 2 (ray / texture / 3D soft), and Round 3 (ambient themes) are all shipped.

## Optional next ideas (unscoped)

Light Ambient themes — not committed, no stable ids yet:

- *Frosted Window Dawn* — condensation + soft sun disc behind frosted glass
- *Obsidian Shore* — lit pebbles + foam wash (Pebble Shore + terminator)
- *Volumetric Stairwell* — trapezoid perspective + one vertical light well
- *Aurora over Ice Shelf* — ice horizon + Aurora ribbons + frost rim
- *Paper Lantern Glow* — lantern silhouettes + warm cones

## Parked (too heavy for Ambient)

Sim / buffer-heavy concepts — impress but wrong cost for phone / Auto loops:

- `genart.slimegrowth` — Physarum-style transport filaments
- `genart.differentialgrowth` — expanding self-repelling lichen rim
- `genart.wetwash` — watercolor bleeding (needs diffusion buffers)
- `genart.hatching` — charcoal stroke physics
- Gray-Scott reaction-diffusion, hydraulic erosion — optional future GPU / AGSL only

## Out of scope

- Detailed pets walking or interacting
- City traffic sims
- Real storm lightning strobes (calm glow pulses already ship as `genart.storm`)
- Anything that fights “relax” on Auto / TV Ambient

## Ship filter

| Tier | Style | Ambient? |
|------|--------|----------|
| **A — Analytic** | Polar math, prebaked attractors, `loopedFbm` / `flowAngle01` | Preferred |
| **B — Light physics** | Shared wind scalar, short Verlet, Gerstner sines | OK if `qualityCount`-capped |
| **C — Field sims** | Physarum, Gray-Scott, wet diffusion | Park |
| **D — GPU shaders** | AGSL `RuntimeShader` | Flagship only, API 33+, Canvas fallback |

## Registry

Engines register through `GenartRegistry` (`GenartEngineDescriptor` = one live Composable + one
baked still, keyed by id/title) — `GenartEffectCanvas` and `GenartStillRenderer` just dispatch
through it, so `GenartCatalog.entries()`/`engineForId()` are derived, not hand-maintained.

Shipping an engine means:

1. Add a `GenartEngineId` case.
2. Write the engine Composable + `stills/XxxStill.kt` (or rely on the generic particle-scatter
   fallback for engines with no dedicated still).
3. Add one `GenartEngineDescriptor` to `GenartRegistry.all`.
4. Add the id/title to `GenartSource` in `:shared` — a separate KMP-only catalog, since
   `:genart` is Android-only and `:shared` can't depend on it, so the two stay in sync by hand.

`FreeTierLimits.maxGenart` / `StockPhotoSettings.MAX_GENART` (bumped 136 → 144 as the catalog
reached 127 engines) are sized to "cover the full shipped catalog" — bump both together again
once the count gets within ~5 of the ceiling.

## Realism upgrades (rendering techniques)

Codebase-compatible upgrades for new *and* existing engines, in rough order of effort/impact:

1. **Real blur** — wrap soft elements in `Modifier.blur(...)` (no-op below API 31). **Done** for
   Clouds, Fog, Aurora, Nebula, Storm (`Box` crisp layer + blurred soft layer; stills may use
   `BlurMaskFilter`). Remaining candidates: Spiral galaxy core glow, Soft caustics.
2. **Domain-warped noise (fbm)** — **Done.** `NoiseUtils.kt`: `valueNoise2D` / `fbm2D` /
   `loopedFbm`. Prefer `loopedFbm(t, …)` so `t = 0` and `t = 1` match under `RepeatMode.Restart`
   (plain `fbm2D(t * freq, 0f)` pops at the seam). Applied to Clouds, Fog, Nebula, Low-Frequency
   Noise Field. Remaining: Soft Caustics streak jitter, Spiral Galaxy core pulse.
3. **Depth/parallax layering** — 2–3 Canvas layers at different scroll speeds *and* blur radii
   (near = sharp + fast, far = blurred + slow).
4. **Physically-inspired motion, not full sims**:
   - Grass/Reeds/wind-chime: shared wind-gust scalar + light spring/Verlet tips
   - Rain/Snow: horizontal drift tied to that gust
   - Water: Gerstner-style summed sines (see `genart.oceanswell`) instead of one radial ripple
5. **Light/shading realism** — off-center radial + terminator for spheres; thin blurred atmosphere
   rim. Shipped for Solar System planets and Moonlight Ripples’ moon.
6. **AGSL `RuntimeShader`** (API 33+) — flagship path with Canvas fallback for `minSdk = 26`
   (pattern: Interference Wash, Soft Ray Orbs, Soap Film).
7. **Anti-pixelation hygiene** — still bakers: `Paint` with `isAntiAlias` + `isDither`.

## Parallel-build notes (large batches)

Do shared plumbing (steps 1, 3, 4 above) single-threaded first; hand each engine Composable +
still to an independent session that touches only those two files.

- **Prefix private top-level data classes with the engine name** (`LakeRippleSeed`, not
  `RippleSeed`) — Kotlin `private` is file-scoped for access but not for JVM class names;
  duplicate names collide across files in the same package.
- `Brush.radialGradient` — use vararg `Pair` form: `0f to c1, 0.5f to c2, …` (not
  `colors =` + `colorStops =`).
- Concurrent checkouts in a shared working tree: prefer `git worktree add` for isolated work;
  copy `local.properties` into a fresh worktree.
- macOS/BSD `sed -i ''` does not support `\b`; use substring replace or `\<...\>`.

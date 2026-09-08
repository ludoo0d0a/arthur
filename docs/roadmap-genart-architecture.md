# Genart architecture & rendering techniques

Part of the [Genart roadmap](roadmap-genart.md) — see that doc for principles and the
shipped-today summary, and [roadmap-genart-backlog.md](roadmap-genart-backlog.md) for the
category-by-category ideas backlog.

## Registry

Engines register through `GenartRegistry` (`GenartEngineDescriptor` = one live Composable + one
baked still, keyed by id/title) — `GenartEffectCanvas` and `GenartStillRenderer` just dispatch
through it, so `GenartCatalog.entries()`/`engineForId()` are derived, not hand-maintained.

Shipping an engine from the backlog means:

1. Add a `GenartEngineId` case.
2. Write the engine Composable + `stills/XxxStill.kt` (or rely on the generic particle-scatter
   fallback for engines with no dedicated still).
3. Add one `GenartEngineDescriptor` to `GenartRegistry.all`.
4. Add the id/title to `GenartSource` in `:shared` — a separate KMP-only catalog, since
   `:genart` is Android-only and `:shared` can't depend on it, so the two stay in sync by hand.

`FreeTierLimits.maxGenart` / `StockPhotoSettings.MAX_GENART` (bumped 64 → 96 as the catalog
reached 59 engines) are sized to "cover the full shipped catalog" — bump both together again
once the count gets within ~5 of the ceiling.

## Realism upgrades (rendering techniques)

None of the shipped engines use blur, shaders, or simulated physics today — softness is faked
entirely with gradients and overlapping low-alpha shapes. These are the concrete,
codebase-compatible upgrades to reach for on new *and* existing engines, in rough order of
effort/impact:

1. **Real blur** — wrap soft elements (cloud puffs, fog patches, aurora ribbons, nebula/galaxy
   gas, bokeh stars, distant storm glow, moon disc, corona ring) in `Modifier.blur(...)`. It's a
   no-risk drop-in: below API 31 it silently no-ops back to today's look, so it needs no version
   gating. **Done** for Clouds, Fog, Aurora, Nebula, and the new Storm (each now splits a crisp
   background/star layer from a separately-blurred soft-shape layer in a `Box`); the still bakers
   for Storm additionally use `BlurMaskFilter` for a real blur on the software `Bitmap` canvas.
   Remaining candidates: Spiral galaxy core glow, Soft caustics.
2. **Domain-warped noise (fbm)** — a small pure-Kotlin value-noise/fractal-Brownian-motion
   helper (a few octaves of the existing `seededUnit`-style hash, offset and summed) to replace
   pure-sine drift in Clouds, Fog, Nebula/Galaxy, Caustics. Reads as organic/non-repeating
   instead of visibly periodic; no new dependency.
3. **Depth/parallax layering** — 2-3 Canvas layers at different scroll speeds *and* blur radii
   (near = sharp + fast, far = blurred + slow). Cheap realism boost for wind streaks, dunes,
   starfields, mountains, asteroids.
4. **Physically-inspired motion, not full sims**:
   - Grass/Reeds/wind-chime: a lightweight spring/Verlet integrator per blade or shape tip,
     driven by one shared slowly-varying "wind gust" scalar, instead of independent per-blade
     sine phases — gusts move the whole scene together, which reads as real wind.
   - Rain/Snow: tie horizontal drift to that same shared wind scalar (all drops lean together
     during a gust) rather than fully independent per-drop phases.
   - Water surfaces: Gerstner-style summed sine trains (see Water & fluids in the backlog)
     instead of one radial ripple.
5. **Light/shading realism** — off-center radial gradient + darker terminator arc for spheres
   (Planets, Sun/corona) instead of a flat radial fill; a thin blurred rim ring for atmosphere.
   Shipped for Solar System's planets and Moonlight Ripples' moon disc.
6. **AGSL `RuntimeShader`** (API 33+ only) — reserve for 1-2 flagship engines (e.g. volumetric
   Fog/Clouds, Caustics) as a high-quality path gated behind `Build.VERSION.SDK_INT >= 33`, with
   today's Canvas version kept as the fallback for the `minSdk = 26` floor. Not needed for most
   of the list above.
7. **Anti-pixelation hygiene** — the `stills/*.kt` bakers use `android.graphics.Paint` directly;
   confirm `isAntiAlias = true` (and `isDither = true`) on every `Paint` there so baked Auto/TV
   stills match the anti-aliased look of the live Compose Canvas engines.

## Parallel-build notes (for future large batches)

When shipping many engines at once, do the shared-file plumbing (steps 1, 3, 4 above) yourself
single-threaded first, then hand each engine's Composable + still (step 2) to an independent
agent/session that touches only those two new files — this avoids merge conflicts since nothing
shared is edited concurrently. Two gotchas hit in practice:

- `Brush.radialGradient(colors = ..., colorStops = ...)` isn't a valid overload — use the
  vararg `Pair<Float, Color>` form: `Brush.radialGradient(0f to c1, 0.5f to c2, 1f to c3, ...)`.
- Kotlin `private` top-level classes are file-scoped for *accessibility* but not for their
  compiled class name — two files in the same package each declaring `private data class Foo`
  collide at the JVM level ("Redeclaration"). Give each engine's private seed/data classes a
  name unlikely to collide with another engine's (e.g. prefix with the engine name).

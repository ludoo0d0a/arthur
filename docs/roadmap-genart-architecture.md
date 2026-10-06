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

`FreeTierLimits.maxGenart` / `StockPhotoSettings.MAX_GENART` (bumped 120 → 128 as the catalog
reached 123 engines) are sized to "cover the full shipped catalog" — bump both together again
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
2. **Domain-warped noise (fbm)** — **Done.** `NoiseUtils.kt` adds `valueNoise2D`/`fbm2D`/
   `loopedFbm` (a few octaves of the existing `seededUnit`-style hash, offset and summed; no new
   dependency). `loopedFbm(t, radius, octaves, seedOffset)` is the one to reach for in an
   engine: it samples a fixed circle in 2D noise space so `t = 0` and `t = 1` land on the exact
   same point, which is what makes it safe to drive with the same `RepeatMode.Restart` time
   driver every engine already uses — plain `fbm2D(t * freq, 0f)` would NOT loop seamlessly and
   would pop at the seam. Applied to Clouds' cloud bob, Fog's bank bob, and Nebula's cloud pulse
   (each replaced a `sin(time * freq * 2π + phase)` term 1:1, same amplitude scale, now organic
   and guaranteed seam-free) and to the new Low-Frequency Noise Field engine. Remaining
   candidates: Soft Caustics' per-streak jitter (currently summed plain sines, not yet fbm),
   Spiral Galaxy's core glow pulse.
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
shared is edited concurrently. Gotchas hit in practice, roughly in order of how often they bite:

- **Private top-level class name collisions are the #1 recurring failure.** Kotlin `private`
  top-level classes are file-scoped for *accessibility* but not for their compiled class name —
  two files in the same package each declaring `private data class Foo` collide at the JVM level
  ("Redeclaration", plus confusing "Cannot access ... it is private in file" errors in whichever
  file's declaration didn't win). This happened repeatedly across two 20+ engine batches
  (`RippleSeed`, `BubbleSeed`, `FishSeed`, `PetalSeed`, `DuneLayerSeed`, `SandGrainSeed`, ...)
  because independent agents each pick an obvious generic name (`RippleSeed`, `BlobSeed`, ...)
  for their seed/data class without seeing sibling engines' choices. Prefix every such class
  with the engine name (e.g. `LakeRippleSeed`, not `RippleSeed`) in the brief up front — cheaper
  than fixing it after the fact. After a batch lands, `grep -rn "^private data class" engines/
  stills/` and diff for duplicate class names before trusting a first compile attempt.
- `Brush.radialGradient(colors = ..., colorStops = ...)` isn't a valid overload — use the
  vararg `Pair<Float, Color>` form: `Brush.radialGradient(0f to c1, 0.5f to c2, 1f to c3, ...)`.
- **This repo is used by multiple concurrent Claude Code sessions sharing one working
  directory.** Another session can `git checkout` a different branch out from under you between
  tool calls, silently swapping every file's on-disk content (edits then fail with confusing
  "File does not exist" or apply against stale content) or refuse a `git checkout` back with
  "local changes would be overwritten" (someone else's uncommitted WIP). If you notice files
  reverting to old content mid-task, immediately check `git branch --show-current` / `git log
  --oneline -3` before doing anything else — do NOT stash or discard what you find (a `git stash
  list` may reveal your own lost edits safely preserved, or someone else's WIP you must not
  touch). The reliable fix is `git worktree add /tmp/<name> main` and doing all remaining work
  there — a worktree has its own working directory, isolated from whatever branch the shared
  checkout is on, while still sharing the same `.git` (so stashes/branches/pushes are visible
  from both). A fresh worktree needs its own `local.properties` (gitignored, holds
  `sdk.dir`/API keys) copied over before Gradle will resolve the Android SDK.
- macOS/BSD `sed -i ''` does **not** support `\b` word-boundary regex (that's a GNU/PCRE
  extension) — a rename script using `s/\bFoo\b/Bar/g` silently matches nothing and leaves the
  file unchanged. Use plain substring replacement instead when the identifier is distinctive
  enough not to collide with a longer name, or use `\<...\>` (BSD's own boundary syntax) if not.

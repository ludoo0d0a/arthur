# Custom Fractal (Realme AOD Bezier)

Product + technical roadmap for **Custom Fractal**: user tap points on an AMOLED-dark field become a unique Bezier-driven animated fractal Artwork. Premium-only. Distinct from free **Fractal Presets**.

**Goal:** calm, luminous stroke loops for Control Plane preview, Auto album art, and TV Dream — each authored point set yields a deterministic, unique animation.

## Principles

- Sparse luminous strokes on near-black; slow morph/pulse; **no flicker** (car-safe).
- Uniqueness: same `points` + `colorSeed` + morph mode → same loop forever; any change → visibly different piece.
- Points: normalized `[0,1]` coords, **3–12** taps.
- Params travel in Canvas Pairing **manifests** (tiny encoded ids), not image blobs.
- Battery-aware (sparse pixels, pause when inactive) so a future AOD Canvas stays feasible.

## Visual / uniqueness freeze

| Rule | Value |
|------|--------|
| Background | True black / deep navy |
| Strokes | Soft neon from `colorSeed` palette |
| Motion | Slow loop `t ∈ [0,1)`; breathe / orbit / unfold morph modes |
| Quality | Low / Medium / High → recursion depth + stroke count |
| Id prefix | `customfractal.` |

## Phases

| Phase | Scope | Status |
|-------|--------|--------|
| 0 | Spec freeze (this doc) | Done |
| 1 | Pure Kotlin seed / Bezier engine + unit tests | Shipped in `:fractal` |
| 2 | `CustomFractalEffectCanvas` + ArtworkRenderer wiring | Shipped |
| 3 | Premium tap editor + persist into Content Engine | Shipped |
| 4 | Auto still bake + TV live; pairing via encoded params | Shipped |
| 5 | Polish: morph modes, perf pause | Shipped |
| 6 | Phone wallpaper (v2); OEM AOD / lock screen | **Deferred** (ADR 0001) |

## Out of scope

- Replacing Fractal Presets with Bezier math
- Prompt / AI image gen
- OEM AOD Canvas in v1
- Live Julius dependency

## Success criteria

1. Different tap sets → obviously different loops; identical sets match at the same `t`.
2. Premium-only author → Artwork in Auto/TV rotation.
3. Looks at home on black AMOLED; calm enough for Ambient / car.
4. Params serializable and small enough for pairing manifests.

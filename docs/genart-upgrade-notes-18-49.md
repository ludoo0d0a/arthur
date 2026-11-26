# Genart upgrade notes — #18–#49

Pipeline: skill `genart-upgrade` (commit f7adc91). Engine + still pairs updated. Unit tests `:genart:testDebugUnitTest` OK.

---

## #18 - Wind-Blown Dunes (Dunes)

**Catégorie**: nature  
**Score avant**: Ambient 7/10 | Beauté 6/10 | Perf 8/10 | Motion 4/10  
**Score après**: Ambient 8/10 | Beauté 7/10 | Perf 7/10 | Motion 8/10

### Améliorations
- Drift crêtes ×2–3 et amps plus lisibles
- Plus de grains + 2e couche rapide (parallax vent)
- Strokes/alpha grains renforcés (still aligné)

---

## #19 - Constellation Twinkle (Constellation)

**Catégorie**: abstract  
**Score avant**: Ambient 7/10 | Beauté 6/10 | Perf 9/10 | Motion 5/10  
**Score après**: Ambient 8/10 | Beauté 8/10 | Perf 8/10 | Motion 8/10

### Améliorations
- Étoiles filantes (2 streaks, trail + tête)
- Planète avec terminateur + anneau, rotation ~75s
- Still bake météore + planète

---

## #20 - Drifting Clouds (Clouds)

**Catégorie**: nature  
**Score avant**: Ambient 7/10 | Beauté 6/10 | Perf 8/10 | Motion 4/10  
**Score après**: Ambient 8/10 | Beauté 7/10 | Perf 7/10 | Motion 8/10

### Améliorations
- Counts 6/10/14 ; speedMul 0.18–0.45 ; loop ~28s
- Layers far/near ; alpha core ↑

---

## #22 - Soft Fog (Fog) / #42 Soft Smog (Smog)

**Catégorie**: nature  
**Score avant**: Motion 3–4/10  
**Score après**: Motion 8/10

### Améliorations
- speedMul ×~2, loops ~36–38s, contre-courant ±
- Banques foreground plus opaques ; still sans damp `*0.1`

---

## #23 - School of Fish (FishSchool)

**Catégorie**: animals  
**Score avant**: Ambient 7/10 | Beauté 4/10 | Perf 8/10 | Motion 5/10  
**Score après**: Ambient 8/10 | Beauté 8/10 | Perf 7/10 | Motion 8/10

### Améliorations
- Counts 20/32/48, orbites serrées, arcs groupés
- Heading partagé école + silhouettes Path (teardrop, dorsal, queue V, œil)
- Lead fish plus grands

---

## #26 - Sparse Meteors (Meteors)

**Catégorie**: abstract  
**Score avant**: Motion 3/10 (souvent vide)  
**Score après**: Motion 8/10

### Améliorations
- Counts 6/9/12 ; duration 0.25–0.45 ; offsets étalés
- Trails plus longs/épais ; peak alpha ~0.82

---

## #32 Soft Noise Field / #33 Voronoi Wash / #35 Gradient Mesh

**Catégorie**: abstract

### Améliorations
- Noise : drift ±0.16, breathe rayon/alpha, loop ~28s
- Voronoi : drift ×3, pulse fill/rayon/stroke
- Mesh : orbites autour milieu A/B + harmonique radius

---

## #36 - Arc Mosaic (ArcMosaic)

**Catégorie**: abstract

### Améliorations
- Envelope appear/disappear par tile (`sin01`, off &lt;0.05)
- Pulse stroke synchronisé

---

## #38 - Star Field Parallax (StarField)

**Catégorie**: abstract

### Améliorations
- Drift loop ~45s ; scales 0.04/0.12/0.25 + Y parallax
- Dust driftMul ↑

---

## #40 Candle Ember / #41 Soft Rainbow / #44 Heat Haze / #49 Landslide Dust

**Catégorie**: abstract / nature (`?`)

### Améliorations
- Candle : jitter centre + braises montantes + flicker HF
- Rainbow : dérive centre X, radius breathe, shimmer par bande
- HeatHaze : amp ↑, wobble vertical, formule sine unifiée engine/still
- Landslide : loop ~50s, speedMul ×2, puff + biais downhill

---

## #43 Rising Smoke / #45 Soft Sunshine

### Améliorations
- Smoke : plus de wisps, rise ↑, vent horizontal, cross-fade loop
- Sunshine : loop ~30s, swayAmp ×3, twinkle rayons, spin global

### Artifacts
- Preview icons : `screenshots/#N-*.png` (à régénérer via `upgrade-one.sh --after` si besoin)
- Notes bake before/after : optionnel par engine clé (#19, #23, #26, #36)

# Audit genart Ambient Arthur

MOTEUR: {{ENGINE}}

Previews (revue humaine / chemins):
- Still PNG: {{STILL_PNG}}
- Vidéo MP4: {{VIDEO_MP4}}

CONTRAINTES
- Ambient: lent, soft, loopable, pas de flash (phone / TV / Auto-safe).
- Live = Compose Engine ; Still = android.graphics (Auto + grille).
- Tier A/B ; helpers qualityCount, loopedFbm, phase01, TonalPalette.

Engine:
```kotlin
{{ENGINE_SRC}}
```

Still:
```kotlin
{{STILL_SRC}}
```

TÂCHE — checklist en français:
1. Score Ambient 1–10 (calme, loop, car-safe).
2. Score Beauté 1–10.
3. Score Perf (densité vs quality Low/Med/High).
4. Score motion (fluidité, seam de loop, vitesse Ambient) 1–10.
5. Bugs probables (seam, flash, palette ignorée, speed mort, still ≠ live).
6. Top 5 améliorations concrètes (facile → fort impact).
7. Décision: KEEP_LIGHT_FIX | REDESIGN | SKIP.

Ne réécris pas le code.

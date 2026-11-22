# Optimisation qualité visuelle genart Arthur

MOTEUR: {{ENGINE}}

AUDIT:
{{AUDIT}}

MISSION
Améliorer clairement le **rendu** et l’**animation** Ambient (phone / TV) tout en gardant le still Auto cohérent et car-safe.

CONTRAT STRICT
- Toucher UNIQUEMENT {{ENGINE}}Engine.kt et {{ENGINE}}Still.kt
- Signatures Engine / Still inchangées
- qualityCount ; loopedFbm pour le temps (boucle sans pop) ; speed sur les tween
- Soft, lent, pas de flash / strobe
- Still cohérent avec le live (même mood, phase animable)

AMÉLIORATIONS ATTENDUES (applique celles qui manquent)
1. Profondeur : 2–3 couches (loin soft/blur, près net)
2. Motion plus riche mais douce (gust partagé, breathe, parallax)
3. Lumière : glow contrôlé, gradients, anti-alias + dither sur Paint
4. Palette / brightness / isActive respectés
5. Densités plafonnées en Low quality

LIVRABLE
1. Résumé FR (5 puces max) — ce qui a été amélioré
2. Code COMPLET des 2 fichiers Kotlin dans des fences ```kotlin
   — d’abord le Engine, puis le Still

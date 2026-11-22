# Fix qualité visuelle genart Arthur

MOTEUR: {{ENGINE}}

AUDIT:
{{AUDIT}}

CONTRAT STRICT
- Toucher UNIQUEMENT {{ENGINE}}Engine.kt et {{ENGINE}}Still.kt
- Signatures Engine / Still inchangées
- Préférer délégation Engine → Still quand le pattern existe
- qualityCount ; loopedFbm si noise temporel ; speed sur les tween
- Motion plus riche mais soft ; profondeur ; glow contrôlé ; pas de flash
- Still cohérent avec le live (Auto)

Engine actuel:
```kotlin
{{ENGINE_SRC}}
```

Still actuel:
```kotlin
{{STILL_SRC}}
```

LIVRABLE
1. Résumé FR (5 puces max)
2. Code COMPLET des 2 fichiers Kotlin prêts à coller

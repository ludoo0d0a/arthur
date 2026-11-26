# Improvement Notes Template

Use this template to document each engine upgrade.

---

## Template

```markdown
## #<N> - <Title> (<EngineName>)

**Catégorie**: <animals|nature|abstract|scene>
**Score avant**: Ambient X/10 | Beauté X/10 | Perf X/10 | Motion X/10
**Score après**: Ambient X/10 | Beauté X/10 | Perf X/10 | Motion X/10

### Améliorations
- <amélioration 1>
- <amélioration 2>
- <amélioration 3>
- <amélioration 4>
- <amélioration 5>

### Artifacts
- Before still: `previews/before/stills/<slug>.png`
- After still:  `previews/after/stills/<slug>.png`
- Before video: `previews/before/videos/<slug>.mp4`
- After video:  `previews/after/videos/<slug>.mp4`
- Comparison:   `previews/compare/<slug>.mp4`
- Preview icon: `screenshots/#<N>-<Title>.png`
```

---

## Example

```markdown
## #160 - Cat at Window (CatWindow)

**Catégorie**: animals
**Score avant**: Ambient 7/10 | Beauté 5/10 | Perf 8/10 | Motion 6/10
**Score après**: Ambient 9/10 | Beauté 8/10 | Perf 8/10 | Motion 8/10

### Améliorations
- Silhouette chat réaliste via Path + cubicTo (oreilles pointues, dos courbé, queue en S)
- Profondeur 3 couches : mur flou → fenêtre nette → chat au premier plan avec ombre
- Éclairage aube volumétrique avec 4 stops de dégradé radial
- Poussières animées dans le rayon de lumière (pulsation + dérive loopée)
- Still cohérent : même composition, couleurs de palette, pose figée naturelle

### Artifacts
- Before still: `previews/before/stills/catwindow.png`
- After still:  `previews/after/stills/catwindow.png`
- Before video: `previews/before/videos/catwindow.mp4`
- After video:  `previews/after/videos/catwindow.mp4`
- Comparison:   `previews/compare/catwindow.mp4`
- Preview icon: `screenshots/#160-Cat at Window.png`
```

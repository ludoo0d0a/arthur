# Vector Shape Reference — Animals & Objects

Guidelines for making genart animals and objects look **recognizably real** using
`android.graphics.Path` and Compose `Path` with Bézier curves.

---

## Principle: Silhouette First

A viewer should recognize the animal/object from its **silhouette alone** (no
color, no detail). If the outline reads as a blob, the genart fails. Work from
simple SVG references and translate key control points.

---

## Cat Silhouette

### Characteristic Features
- **Pointed triangular ears** (key identifier)
- **Rounded head** slightly smaller than body
- **Curved back** (convex arch when sitting)
- **Slim legs** tucked under when sitting, extended when stretching
- **Long curved tail** (S-curve or gentle arc)

### Path Construction (Sitting Cat)
```kotlin
// Head
val headR = minDim * 0.08f
canvas.drawCircle(hx, hy, headR, paint)

// Ears (the most important feature — pointed triangles)
path.reset()
// Left ear
path.moveTo(hx - headR * 0.6f, hy - headR * 0.3f)
path.lineTo(hx - headR * 0.85f, hy - headR * 1.5f)  // tip
path.lineTo(hx - headR * 0.1f, hy - headR * 0.6f)
path.close()
// Right ear
path.moveTo(hx + headR * 0.1f, hy - headR * 0.6f)
path.lineTo(hx + headR * 0.85f, hy - headR * 1.5f)  // tip
path.lineTo(hx + headR * 0.6f, hy - headR * 0.3f)
path.close()
canvas.drawPath(path, paint)

// Body (teardrop shape via cubic bezier, not an oval)
path.reset()
path.moveTo(cx - bodyW * 0.3f, cy - bodyH * 0.8f)  // neck
path.cubicTo(
    cx - bodyW * 0.9f, cy - bodyH * 0.5f,  // left shoulder
    cx - bodyW * 0.8f, cy + bodyH * 0.6f,  // left hip
    cx, cy + bodyH * 0.8f,                  // bottom center
)
path.cubicTo(
    cx + bodyW * 0.8f, cy + bodyH * 0.6f,  // right hip
    cx + bodyW * 0.9f, cy - bodyH * 0.5f,  // right shoulder
    cx + bodyW * 0.3f, cy - bodyH * 0.8f,  // back to neck
)
path.close()

// Tail (smooth S-curve)
path.reset()
path.moveTo(tailBaseX, tailBaseY)
path.cubicTo(
    tailBaseX + w * 0.08f, tailBaseY - h * 0.05f,
    tailBaseX + w * 0.12f, tailBaseY - h * 0.12f,
    tailBaseX + w * 0.06f, tailBaseY - h * 0.18f,
)
paint.style = Paint.Style.STROKE
paint.strokeWidth = minDim * 0.02f
paint.strokeCap = Paint.Cap.ROUND
canvas.drawPath(path, paint)
```

### Inner Details
- **Eyes**: Two small circles at `hy - headR * 0.1f`, slightly apart
- **Whiskers** (optional, High quality): 3 thin lines from snout area
- **Fur texture**: Radial gradient on body (darker center, lighter edges)

---

## Dog Silhouette

### Characteristic Features
- **Floppy or pointed ears** (breed-dependent — default to semi-floppy)
- **Longer snout** than cat (protruding muzzle)
- **Thicker body** / barrel chest
- **Visible legs** (dogs sit with legs out, not tucked)
- **Wagging tail** (upward curve, animated)

### Path Construction (Sitting Dog)
```kotlin
// Head — slightly oval, not perfectly round
rect.set(hx - headR * 1.1f, hy - headR, hx + headR * 1.1f, hy + headR)
canvas.drawOval(rect, paint)

// Snout — protruding ellipse
rect.set(hx - headR * 1.5f, hy, hx - headR * 0.6f, hy + headR * 0.5f)
canvas.drawOval(rect, paint)

// Floppy ears (key differentiator from cat)
path.reset()
path.moveTo(hx - headR * 0.8f, hy - headR * 0.4f)
path.cubicTo(
    hx - headR * 1.5f, hy - headR * 0.2f,  // droop out
    hx - headR * 1.4f, hy + headR * 0.8f,   // droop down
    hx - headR * 0.6f, hy + headR * 0.3f,   // back to head
)
path.close()
canvas.drawPath(path, paint)
// Mirror for right ear

// Body — wider chest, tapers to waist
path.reset()
path.moveTo(cx - bodyW * 0.3f, cy - bodyH * 0.6f)
path.cubicTo(
    cx - bodyW, cy - bodyH * 0.3f,
    cx - bodyW * 0.7f, cy + bodyH * 0.5f,
    cx - bodyW * 0.2f, cy + bodyH * 0.7f,
)
// ... continue around

// Front legs (visible, not tucked)
paint.strokeWidth = minDim * 0.03f
canvas.drawLine(cx - bodyW * 0.5f, cy + bodyH * 0.3f,
                cx - bodyW * 0.5f, cy + bodyH * 1.1f, paint)
// Paw circle at bottom
canvas.drawCircle(cx - bodyW * 0.5f, cy + bodyH * 1.1f, minDim * 0.02f, paint)
```

### Key Differences from Cat
| Feature | Cat | Dog |
|---------|-----|-----|
| Ears | Pointed triangles | Floppy/rounded drops |
| Snout | Barely visible | Protruding ellipse |
| Body | Slim, curved back | Wider barrel chest |
| Legs | Tucked/thin | Visible, thicker |
| Tail | Thin S-curve | Thicker, upward wag |

---

## Panda Silhouette

### Characteristic Features
- **Round body** (rounder than cat or dog)
- **Distinct black/white patches** (the defining trait)
- **Round ears** on top of round head
- **Dark eye patches** (the most recognizable panda feature)
- **Short stubby limbs**

### Critical: Black & White Pattern
```kotlin
// 1. White base body
paint.color = white  // 0xFFE8E4DC
canvas.drawCircle(cx, cy, bodyR, paint)

// 2. Black patches (must be clearly separate zones)
paint.color = black  // 0xFF141210
// Eye patches — large ovals around small eyes
rect.set(cx - headR * 0.65f, hy - headR * 0.35f,
         cx - headR * 0.1f, hy + headR * 0.15f)
canvas.drawOval(rect, paint)
// Mirror right side

// Arms — full black circles (not tiny dots)
canvas.drawCircle(cx - bodyR * 0.85f, cy + bodyR * 0.2f, minDim * 0.08f, paint)

// Legs — black circles
canvas.drawCircle(cx - bodyR * 0.3f, cy + bodyR * 0.9f, minDim * 0.07f, paint)

// Ears — round (not pointed like cat!)
canvas.drawCircle(cx - headR * 0.7f, hy - headR * 0.75f, headR * 0.35f, paint)
```

---

## Bird Silhouette

### Characteristic Features
- **V-shaped wings** (chevron) — the classic flying bird silhouette
- **Small body mass** at the chevron center
- **Tail fan** (optional, adds realism)
- **Head protrusion** at front

### Improved Bird (Beyond Simple Chevron Lines)
```kotlin
// Instead of just two lines, draw a filled wing shape:
path.reset()
// Body center dot
val bodyR = wingLen * 0.15f
// Left wing
path.moveTo(bx, by - bodyR)
path.cubicTo(
    bx - wingLen * 0.4f, by - wingLen * 0.3f * flapAmount,
    bx - wingLen * 0.8f, by - wingLen * 0.5f * flapAmount,
    bx - wingLen, by + wingLen * 0.1f * (1f - flapAmount),
)
path.lineTo(bx - wingLen * 0.7f, by + bodyR * 0.5f)
path.lineTo(bx, by + bodyR)
// Right wing (mirror)
path.lineTo(bx + wingLen * 0.7f, by + bodyR * 0.5f)
path.cubicTo(
    bx + wingLen * 0.8f, by - wingLen * 0.5f * flapAmount,
    bx + wingLen * 0.4f, by - wingLen * 0.3f * flapAmount,
    bx, by - bodyR,
)
path.close()
canvas.drawPath(path, paint)
```

---

## Fish Silhouette

### Characteristic Features
- **Streamlined teardrop body** (wider at head, tapers at tail)
- **Tail fin** — forked V-shape
- **Dorsal fin** on top
- **Eye** dot near front

### Improved Fish (Beyond Simple Oval)
```kotlin
path.reset()
// Body outline — teardrop via cubic bezier
path.moveTo(fx + bodyLen, fy)  // nose
path.cubicTo(
    fx + bodyLen * 0.7f, fy - bodyH,     // top curve
    fx - bodyLen * 0.3f, fy - bodyH * 0.8f,
    fx - bodyLen * 0.6f, fy,             // tail junction
)
path.cubicTo(
    fx - bodyLen * 0.3f, fy + bodyH * 0.8f,
    fx + bodyLen * 0.7f, fy + bodyH,     // bottom curve
    fx + bodyLen, fy,                     // back to nose
)
path.close()

// Tail fin — V shape
path.moveTo(fx - bodyLen * 0.6f, fy)
path.lineTo(fx - bodyLen * 0.9f, fy - bodyH * 0.7f)
path.moveTo(fx - bodyLen * 0.6f, fy)
path.lineTo(fx - bodyLen * 0.9f, fy + bodyH * 0.7f)

// Eye
canvas.drawCircle(fx + bodyLen * 0.6f, fy - bodyH * 0.15f, bodyH * 0.1f, eyePaint)
```

---

## Flower Silhouette

### Petal Construction
```kotlin
// 5-petal flower using rotated ellipses
for (i in 0 until 5) {
    val angle = i * (2f * PI.toFloat() / 5f) + rotationDeg
    canvas.save()
    canvas.rotate(Math.toDegrees(angle.toDouble()).toFloat(), cx, cy)
    rect.set(cx - petalW, cy - petalLen, cx + petalW, cy)
    canvas.drawOval(rect, paint)
    canvas.restore()
}
// Center circle (pistil)
paint.color = centerColor
canvas.drawCircle(cx, cy, petalW * 0.6f, paint)
```

---

## Tree Silhouette

### Crown + Trunk
```kotlin
// Trunk — tapered rectangle
path.reset()
path.moveTo(cx - trunkW, groundY)
path.lineTo(cx - trunkW * 0.7f, groundY - trunkH)
path.lineTo(cx + trunkW * 0.7f, groundY - trunkH)
path.lineTo(cx + trunkW, groundY)
path.close()

// Crown — layered ovals (not a single circle!)
for (layer in 0 until 3) {
    val layerY = crownCY - crownR * 0.3f * layer
    val layerR = crownR * (1f - layer * 0.2f)
    val layerAlpha = 0.7f + layer * 0.1f
    paint.color = Color.argb((layerAlpha * 255).toInt(), r, g, b)
    canvas.drawOval(
        cx - layerR, layerY - layerR * 0.8f,
        cx + layerR, layerY + layerR * 0.8f, paint
    )
}
```

---

## Aquarium Scene

### Fish + Bubbles + Plants + Light
```kotlin
// Water gradient (deep blue top → lighter bottom)
// Caustic light patterns (radial gradient strips, animated)
// 3–5 fish at different depths (smaller = farther)
// Seaweed: sine-wave stroked paths from bottom
// Bubbles: circles with highlight dot, rising with wobble
// Sand floor: low horizontal gradient
```

---

## General Tips

1. **Always use `Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG`** — no jagged edges
2. **Soft shadows** under bodies: `BlurMaskFilter(blur, Blur.NORMAL)` with dark translucent paint
3. **Eye glint**: tiny white dot at 10 o'clock position in eye circle
4. **Fur/feather texture**: subtle radial gradient overlay (lighter center, darker edges)
5. **Scale by `minDim`** so the shape fills correctly on any screen ratio
6. **Breath animation**: scale body by `1f + 0.02f * sin(phase)` — subtle but alive
7. **Shadow paint**: `Color.argb(40, 0, 0, 0)` with blur, offset slightly down-right

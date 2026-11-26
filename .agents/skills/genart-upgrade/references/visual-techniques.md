# Visual Techniques Reference — Genart Upgrade

Advanced rendering techniques for achieving "wow" quality in Arthur genart engines.

---

## 1. Lighting & Glow

### Soft Radial Glow
```kotlin
// Multi-stop radial gradient for natural falloff
paint.shader = RadialGradient(
    cx, cy, radius,
    intArrayOf(
        Color.argb(alpha, r, g, b),          // bright core
        Color.argb(alpha / 2, r, g, b),      // mid falloff
        Color.argb(alpha / 6, r, g, b),      // outer halo
        Color.TRANSPARENT,                     // fade to nothing
    ),
    floatArrayOf(0f, 0.25f, 0.6f, 1f),
    Shader.TileMode.CLAMP,
)
```

### Volumetric Light Shafts
```kotlin
// Compose: draw angled translucent rectangles
for (i in 0 until shaftCount) {
    val angle = baseAngle + i * spreadAngle
    rotate(angle, pivot = Offset(lightSourceX, lightSourceY)) {
        drawRect(
            brush = Brush.verticalGradient(
                0f to Color.White.copy(alpha = 0.08f),
                0.5f to Color.White.copy(alpha = 0.03f),
                1f to Color.Transparent,
            ),
            topLeft = Offset(lightSourceX - shaftWidth / 2, lightSourceY),
            size = Size(shaftWidth, shaftLength),
        )
    }
}
```

### Rim Lighting (edge highlight)
```kotlin
// Draw a slightly larger outline behind the object in highlight color
paint.style = Paint.Style.STROKE
paint.strokeWidth = minDim * 0.004f
paint.color = Color.argb(60, 255, 240, 220)
canvas.drawPath(objectPath, paint)
```

---

## 2. Depth & Atmosphere

### Atmospheric Perspective
```kotlin
// Far layer: lighter, bluer, optionally blurred
val farColor = mixArgb(objectColor, 0xFF8EACC8.toInt(), 0.4f)  // blue shift
val farAlpha = 0.4f  // more transparent

// Mid layer: moderate
val midColor = mixArgb(objectColor, 0xFF8EACC8.toInt(), 0.15f)
val midAlpha = 0.7f

// Near layer: full color and opacity
val nearAlpha = 1.0f
```

### Parallax Layers (Compose Engine)
```kotlin
// Different drift speeds per layer
val farDrift = loopedFbm(phase01(anim), radius = 0.8f, octaves = 2, seedOffset = 0)
val midDrift = loopedFbm(phase01(anim), radius = 1.2f, octaves = 2, seedOffset = 7)
val nearDrift = loopedFbm(phase01(anim), radius = 2.0f, octaves = 3, seedOffset = 13)

// Far layer moves slowly
drawFarElements(offsetX = farDrift * w * 0.02f)
// Near layer moves more
drawNearElements(offsetX = nearDrift * w * 0.06f)
```

### Depth-of-Field Simulation
```kotlin
// Blur far elements in Compose
Box(modifier = Modifier.genartSoftBlur(quality, radius = 4.dp)) {
    FarLayerContent()
}
// Keep near elements sharp
NearLayerContent()
```

---

## 3. Animation Smoothness

### Shared Gust / Wind System
```kotlin
// One shared wind value affects all elements coherently
val windT = phase01(driftAnim)
val gust = loopedFbm(windT, radius = 1.5f, octaves = 3, seedOffset = 42)
val gustDirection = loopedFbm(windT, radius = 0.8f, octaves = 2, seedOffset = 99)

// Apply to grass, leaves, particles with different amplitudes
grassSway = gust * 0.3f
leafDrift = gust * 0.5f + gustDirection * 0.2f
particleWobble = gust * 0.1f
```

### Breath Animation Pattern
```kotlin
val breathT = phase01(breathAnim)
val breath = sin(breathT * 2f * PI.toFloat()) * 0.5f + 0.5f

// Scale: subtle 2–3% expansion
val scale = 1f + 0.025f * breath * (if (isActive) 1f else 0.4f)

// Apply to body dimensions
val bodyRx = baseRx * scale
val bodyRy = baseRy * scale
```

### Smooth Loop Transition (no pop/seam)
```kotlin
// Use loopedFbm for ANY time-varying parameter
// NEVER: sin(System.nanoTime() * 0.001f)  — not loopable
// ALWAYS: loopedFbm(phase01(anim), ...)   — seamless

val drift = loopedFbm(phase01(anim), radius = 1.4f, octaves = 2, seedOffset = 11)
```

---

## 4. Color & Palette

### Rich Palette Usage
```kotlin
// Use at least 3 colors from the palette
val primary = TonalPalette.pick(paletteColors, 0)
val secondary = TonalPalette.pick(paletteColors, 1)
val accent = TonalPalette.pick(paletteColors, 2)

// Mix base scene colors with palette for tinting
val skyColor = TonalPalette.mix(Color(0xFF1C2844), primary, 0.15f)
val glowColor = TonalPalette.mix(Color(0xFFFFD8A8), secondary, 0.25f)
val particleColor = TonalPalette.mix(Color(0xFFE8F0C8), accent, 0.3f)
```

### Color Temperature Gradient
```kotlin
// Warm foreground → cool background
val warmGradient = Brush.verticalGradient(
    0f to Color(0xFF1A1428),    // cool sky
    0.4f to Color(0xFF2A2030),  // transition
    0.7f to Color(0xFF3A2820),  // warm mid
    1f to Color(0xFF4A3220),    // warm ground
)
```

### Iridescence / Color Shift
```kotlin
// Hue shift based on angle or time
val hueShift = (phase * 60f) % 360f
val color = Color.hsv(
    hue = (baseHue + hueShift) % 360f,
    saturation = 0.6f,
    value = 0.8f * brightness,
)
```

---

## 5. Surface & Material

### Water Surface
```kotlin
// Animated horizontal wave distortion
for (y in 0 until waveCount) {
    val waveY = waterTop + y * waveSpacing
    val waveOffset = sin(phase * 2f + y * 0.3f) * amplitude
    drawLine(
        color = Color.White.copy(alpha = 0.05f + 0.03f * sin(phase + y)),
        start = Offset(waveOffset, waveY),
        end = Offset(w + waveOffset, waveY),
        strokeWidth = 1.5f,
    )
}
```

### Glass / Window Reflection
```kotlin
// Diagonal highlight strip
drawRect(
    brush = Brush.linearGradient(
        0f to Color.Transparent,
        0.4f to Color.White.copy(alpha = 0.06f),
        0.5f to Color.White.copy(alpha = 0.12f),
        0.6f to Color.White.copy(alpha = 0.06f),
        1f to Color.Transparent,
        start = Offset(paneLeft, paneTop),
        end = Offset(paneLeft + paneW * 0.6f, paneTop + paneH),
    ),
    topLeft = Offset(paneLeft, paneTop),
    size = Size(paneW, paneH),
)
```

### Soft Shadow Under Object
```kotlin
// Ground shadow ellipse with blur
val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
shadowPaint.maskFilter = BlurMaskFilter(minDim * 0.03f, BlurMaskFilter.Blur.NORMAL)
shadowPaint.color = Color.argb(45, 0, 0, 0)
canvas.drawOval(
    cx - shadowW, groundY - shadowH * 0.3f,
    cx + shadowW, groundY + shadowH * 0.3f,
    shadowPaint,
)
```

---

## 6. Particle Systems

### Layered Dust Motes
```kotlin
// 3 layers with different speeds and sizes
for (layer in 0 until 3) {
    val count = qualityCount(quality, 3, 6, 10)
    val moteAlpha = 0.1f + layer * 0.08f
    val moteSize = minDim * (0.003f + layer * 0.002f)
    val driftSpeed = 0.3f + layer * 0.4f

    for (i in 0 until count) {
        val baseX = seededUnit(i * 17 + layer * 100) * w
        val baseY = seededUnit(i * 31 + layer * 100) * h
        val driftX = loopedFbm(phase01(anim) * driftSpeed, radius = 0.5f,
                               octaves = 1, seedOffset = i + layer * 50)
        drawCircle(
            color = glowColor.copy(alpha = moteAlpha * (0.6f + 0.4f * driftX)),
            radius = moteSize,
            center = Offset(baseX + driftX * w * 0.03f,
                           baseY + driftX * h * 0.02f),
        )
    }
}
```

### Firefly Glow (pulsing particles)
```kotlin
val fireflyPulse = sin(phase01(anim) * 2f * PI.toFloat() * 3f + i * 1.5f)
val alpha = (0.3f + 0.4f * fireflyPulse.coerceAtLeast(0f))
// Core
drawCircle(color = glowColor.copy(alpha = alpha), radius = coreR, center = pos)
// Glow halo
drawCircle(
    brush = Brush.radialGradient(
        0f to glowColor.copy(alpha = alpha * 0.5f),
        1f to Color.Transparent,
        center = pos,
        radius = coreR * 4f,
    ),
    radius = coreR * 4f,
    center = pos,
)
```

---

## 7. Scene Composition

### Rule of Thirds
Place the main subject at one of the 4 intersection points:
- `(w * 0.33f, h * 0.33f)` — top-left third
- `(w * 0.67f, h * 0.33f)` — top-right third
- `(w * 0.33f, h * 0.67f)` — bottom-left third
- `(w * 0.67f, h * 0.67f)` — bottom-right third

### Vignette for Focus
Already handled by `GenartEffectCanvas` and `GenartStillFx.polish()`, but engines
can add scene-specific darkening:
```kotlin
// Extra scene vignette for mood
drawRect(
    brush = Brush.radialGradient(
        0.5f to Color.Transparent,
        1f to Color.Black.copy(alpha = 0.3f),
        center = Offset(cx, cy * 0.9f),
        radius = minDim * 0.65f,
    ),
)
```

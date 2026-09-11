package fr.geoking.arthur.fractal

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.pow

enum class FractalQuality { Low, Medium, High }
enum class FractalColorIntensity { Low, Medium, High }

/** Rich multi-stop palette for smooth escape-time gradients. */
private val DefaultPalette = listOf(
    Color(0xFF060919),
    Color(0xFF0B1026),
    Color(0xFF131843),
    Color(0xFF1B1F5C),
    Color(0xFF252A75),
    Color(0xFF2E3A8C),
    Color(0xFF2142B2),
    Color(0xFF1D4ED8),
    Color(0xFF0284C7),
    Color(0xFF0EA5E9),
    Color(0xFF06B6D4),
    Color(0xFF22D3EE),
    Color(0xFF38BDF8),
    Color(0xFF67E8F9),
    Color(0xFF818CF8),
    Color(0xFFA78BFA),
    Color(0xFFC084FC),
    Color(0xFFE879F9),
    Color(0xFFF472B6),
    Color(0xFFFB7185),
    Color(0xFFF87171),
    Color(0xFFFBBF24),
    Color(0xFFFDE68A),
    Color(0xFFFEF08A),
    Color(0xFFFFF7ED),
)

enum class FractalType {
    Mandelbrot,
    Julia,
    BurningShip,
    Tricorn,
    Multibrot,
    Celtic,
    Buffalo,
    Phoenix,
}

/**
 * Animated fractal background with a slow infinite zoom-in effect.
 * Cycles through different fractal types unless [forceType] is set.
 */
@Composable
fun FractalEffectCanvas(
    isActive: Boolean,
    paletteColors: List<Color> = DefaultPalette,
    quality: FractalQuality = FractalQuality.Medium,
    colorIntensity: FractalColorIntensity = FractalColorIntensity.High,
    forceType: FractalType? = null,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "fractal_zoom")
    // Slow infinite zoom: from 1 to 80 over 50 seconds, then restart
    val zoomProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(50000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "zoomProgress"
    )
    val zoom = 1f + zoomProgress * 79f

    // Track zoom cycles to change fractal type (ignored when forceType is set)
    var zoomCycleCount by remember { mutableStateOf(0) }
    var lastZoomProgress by remember { mutableStateOf(0f) }

    LaunchedEffect(zoomProgress, forceType) {
        if (forceType != null) return@LaunchedEffect
        if (zoomProgress < lastZoomProgress) {
            zoomCycleCount++
        }
        lastZoomProgress = zoomProgress
    }

    val cyclingType = remember(zoomCycleCount) {
        FractalType.entries[zoomCycleCount % FractalType.entries.size]
    }
    val fractalType = forceType ?: cyclingType

    // Optional phase for smooth color cycling
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )
    val brightness by animateFloatAsState(
        targetValue = if (isActive) 1.15f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "brightness"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val centerX = w / 2
        val centerY = h / 2

        // Different zoom targets for different fractals
        val (cx, cy) = when (fractalType) {
            FractalType.Mandelbrot -> 0.25f to 0.0f
            FractalType.Julia -> 0.0f to 0.0f
            FractalType.BurningShip -> -1.75f to -0.02f
            FractalType.Tricorn -> -0.25f to 0.0f
            FractalType.Multibrot -> 0.0f to 0.0f
            FractalType.Celtic -> -0.5f to 0.0f
            FractalType.Buffalo -> -0.65f to -0.45f
            FractalType.Phoenix -> 0.0f to 0.0f
        }

        val gridSize = when (quality) {
            FractalQuality.Low -> 128
            FractalQuality.Medium -> 216
            FractalQuality.High -> 320
        }
        val maxIter = when (quality) {
            FractalQuality.Low -> 160
            FractalQuality.Medium -> 320
            FractalQuality.High -> 512
        }

        val cellW = w / gridSize
        val cellH = h / gridSize
        // Visible range in complex plane: slightly wider span so fractals fill full screen
        val halfSpan = 2.2f / zoom

        // Deep atmospheric backdrop
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF1E1B4B),
                    Color(0xFF0F172A),
                    Color(0xFF020617),
                ),
                center = Offset(centerX, centerY),
                radius = maxOf(w, h) * 0.85f,
            )
        )

        for (iy in 0 until gridSize) {
            for (ix in 0 until gridSize) {
                // Pixel center in screen space
                val sx = (ix + 0.5f) * cellW
                val sy = (iy + 0.5f) * cellH
                // Map to complex plane (y flipped for display)
                val re = cx - halfSpan + (sx / w) * (2f * halfSpan)
                val im = cy - halfSpan + (1f - sy / h) * (2f * halfSpan)

                val continuous = when (fractalType) {
                    FractalType.Mandelbrot -> mandelbrotSmooth(re, im, maxIter)
                    FractalType.Julia -> juliaSmooth(re, im, maxIter)
                    FractalType.BurningShip -> burningShipSmooth(re, im, maxIter)
                    FractalType.Tricorn -> tricornSmooth(re, im, maxIter)
                    FractalType.Multibrot -> multibrotSmooth(re, im, maxIter)
                    FractalType.Celtic -> celticSmooth(re, im, maxIter)
                    FractalType.Buffalo -> buffaloSmooth(re, im, maxIter)
                    FractalType.Phoenix -> phoenixSmooth(re, im, maxIter)
                }

                val color = fractalColor(
                    continuous = continuous,
                    phase = phase,
                    brightness = brightness,
                    isActive = isActive,
                    paletteColors = paletteColors,
                    intensity = colorIntensity,
                    maxIter = maxIter,
                )
                drawRect(
                    color = color,
                    topLeft = Offset(ix * cellW, iy * cellH),
                    size = Size(cellW + 1f, cellH + 1f) // slight overlap to avoid gaps
                )
            }
        }

        // Soft vignette + warm center glow for a polished still look
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0x33F472B6),
                    Color(0x140EA5E9),
                    Color(0x99020617),
                ),
                center = Offset(centerX, centerY * 0.92f),
                radius = maxOf(w, h) * 0.78f,
            )
        )
    }
}

/** Continuous escape-time iteration, or -1 when inside the set. */
private fun smoothEscape(n: Int, zr2: Double, zi2: Double, power: Double = 2.0): Float {
    val logZn = ln((zr2 + zi2).coerceAtLeast(1e-12)) / 2.0
    val nu = ln((logZn / ln(2.0)).coerceAtLeast(1e-12)) / ln(power)
    return (n + 1.0 - nu).toFloat()
}

/** Mandelbrot iteration with smooth escape. */
private fun mandelbrotSmooth(cr: Float, ci: Float, maxIter: Int): Float {
    var zr = 0.0
    var zi = 0.0
    var n = 0
    while (n < maxIter) {
        val zr2 = zr * zr
        val zi2 = zi * zi
        if (zr2 + zi2 > 4.0) return smoothEscape(n, zr2, zi2)
        val newZi = 2 * zr * zi + ci
        val newZr = zr2 - zi2 + cr
        zi = newZi
        zr = newZr
        n++
    }
    return -1f
}

private fun juliaSmooth(zrStart: Float, ziStart: Float, maxIter: Int): Float {
    var zr = zrStart.toDouble()
    var zi = ziStart.toDouble()
    val cr = -0.7
    val ci = 0.27015
    var n = 0
    while (n < maxIter) {
        val zr2 = zr * zr
        val zi2 = zi * zi
        if (zr2 + zi2 > 4.0) return smoothEscape(n, zr2, zi2)
        val newZi = 2 * zr * zi + ci
        val newZr = zr2 - zi2 + cr
        zi = newZi
        zr = newZr
        n++
    }
    return -1f
}

/**
 * Burning Ship fractal: take absolute value of components each iteration.
 */
private fun burningShipSmooth(cr: Float, ci: Float, maxIter: Int): Float {
    var zr = 0.0
    var zi = 0.0
    var n = 0
    while (n < maxIter) {
        val zr2 = zr * zr
        val zi2 = zi * zi
        if (zr2 + zi2 > 4.0) return smoothEscape(n, zr2, zi2)
        // Burning Ship: z = (|Re(z)| + i|Im(z)|)^2 + c
        val newZr = zr2 - zi2 + cr
        val newZi = abs(2.0 * zr * zi) + ci
        zr = abs(newZr)
        zi = newZi
        n++
    }
    return -1f
}

/**
 * Tricorn fractal (Mandelbar): z = conj(z)^2 + c
 */
private fun tricornSmooth(cr: Float, ci: Float, maxIter: Int): Float {
    var zr = 0.0
    var zi = 0.0
    var n = 0
    while (n < maxIter) {
        val zr2 = zr * zr
        val zi2 = zi * zi
        if (zr2 + zi2 > 4.0) return smoothEscape(n, zr2, zi2)
        // conj(z) = (zr, -zi)
        // (zr - i zi)^2 = zr^2 - zi^2 - 2 i zr zi
        val nextZr = zr2 - zi2 + cr
        val nextZi = -2.0 * zr * zi + ci
        zr = nextZr
        zi = nextZi
        n++
    }
    return -1f
}

/** Cubic Multibrot: z³ + c */
private fun multibrotSmooth(cr: Float, ci: Float, maxIter: Int): Float {
    var zr = 0.0
    var zi = 0.0
    var n = 0
    while (n < maxIter) {
        val zr2 = zr * zr
        val zi2 = zi * zi
        if (zr2 + zi2 > 4.0) return smoothEscape(n, zr2, zi2, power = 3.0)
        // (zr + i zi)^3 = (zr² - 3 zi²) zr + i (3 zr² - zi²) zi
        val nextZr = zr * (zr2 - 3.0 * zi2) + cr
        val nextZi = zi * (3.0 * zr2 - zi2) + ci
        zr = nextZr
        zi = nextZi
        n++
    }
    return -1f
}

/** Celtic Mandelbrot: |Re(z²)| + i Im(z²) + c */
private fun celticSmooth(cr: Float, ci: Float, maxIter: Int): Float {
    var zr = 0.0
    var zi = 0.0
    var n = 0
    while (n < maxIter) {
        val zr2 = zr * zr
        val zi2 = zi * zi
        if (zr2 + zi2 > 4.0) return smoothEscape(n, zr2, zi2)
        val nextZr = abs(zr2 - zi2) + cr
        val nextZi = 2.0 * zr * zi + ci
        zr = nextZr
        zi = nextZi
        n++
    }
    return -1f
}

/** Buffalo: |Re(z²)| − i |Im(z²)| style absolute fold. */
private fun buffaloSmooth(cr: Float, ci: Float, maxIter: Int): Float {
    var zr = 0.0
    var zi = 0.0
    var n = 0
    while (n < maxIter) {
        val zr2 = zr * zr
        val zi2 = zi * zi
        if (zr2 + zi2 > 4.0) return smoothEscape(n, zr2, zi2)
        val nextZr = abs(zr2 - zi2) + cr
        val nextZi = -abs(2.0 * zr * zi) + ci
        zr = nextZr
        zi = nextZi
        n++
    }
    return -1f
}

/**
 * Phoenix Julia: zₙ₊₁ = zₙ² + Re(c) + Im(c)·zₙ₋₁
 * Classic parameters c ≈ 0.5667 − 0.5i.
 */
private fun phoenixSmooth(zrStart: Float, ziStart: Float, maxIter: Int): Float {
    var zr = zrStart.toDouble()
    var zi = ziStart.toDouble()
    var pr = 0.0
    var pi = 0.0
    val cr = 0.5667
    val ci = -0.5
    var n = 0
    while (n < maxIter) {
        val zr2 = zr * zr
        val zi2 = zi * zi
        if (zr2 + zi2 > 4.0) return smoothEscape(n, zr2, zi2)
        val nextZr = zr2 - zi2 + cr + ci * pr
        val nextZi = 2.0 * zr * zi + ci * pi
        pr = zr
        pi = zi
        zr = nextZr
        zi = nextZi
        n++
    }
    return -1f
}

private fun fractalColor(
    continuous: Float,
    phase: Float,
    brightness: Float,
    isActive: Boolean,
    paletteColors: List<Color>,
    intensity: FractalColorIntensity,
    maxIter: Int,
): Color {
    if (continuous < 0f) {
        return Color(0xFF020617) // Inside: near-black
    }

    val safePalette = if (paletteColors.size >= 2) paletteColors else DefaultPalette

    // Cycle density: more bands → richer gradients around the set boundary
    val cycles = when (intensity) {
        FractalColorIntensity.Low -> 2.2f
        FractalColorIntensity.Medium -> 4.5f
        FractalColorIntensity.High -> 7.5f
    }

    // Log remapping keeps fine detail near the boundary without crushing outer bands
    val normalized = (ln(1.0 + continuous.toDouble()) / ln(1.0 + maxIter.toDouble())).toFloat()
    val t = ((normalized * cycles + phase).mod(1f) + 1f).mod(1f)

    val color = samplePalette(safePalette, t)

    // Soft luminosity lift toward the set edge (high continuous ≈ boundary)
    val edge = (continuous / maxIter.toFloat()).coerceIn(0f, 1f)
    val glow = 0.88f + 0.22f * edge.pow(0.55f)
    val activeBoost = if (isActive) 1.06f else 1f
    val gain = (glow * brightness * activeBoost).coerceIn(0.75f, 1.35f)

    return Color(
        red = (color.red * gain).coerceIn(0f, 1f),
        green = (color.green * gain).coerceIn(0f, 1f),
        blue = (color.blue * gain).coerceIn(0f, 1f),
        alpha = 1f,
    )
}

/** Smooth Hermite interpolation across a multi-stop palette. */
private fun samplePalette(palette: List<Color>, t: Float): Color {
    val scaled = t * palette.size
    val i0 = scaled.toInt().mod(palette.size)
    val i1 = (i0 + 1).mod(palette.size)
    val frac = smoothstep(scaled - scaled.toInt())
    val c0 = palette[i0]
    val c1 = palette[i1]
    return Color(
        red = c0.red + (c1.red - c0.red) * frac,
        green = c0.green + (c1.green - c0.green) * frac,
        blue = c0.blue + (c1.blue - c0.blue) * frac,
        alpha = 1f,
    )
}

private fun smoothstep(x: Float): Float {
    val t = x.coerceIn(0f, 1f)
    return t * t * (3f - 2f * t)
}

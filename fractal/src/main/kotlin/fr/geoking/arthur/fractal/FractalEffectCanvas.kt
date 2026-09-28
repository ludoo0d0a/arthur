package fr.geoking.arthur.fractal

import android.os.Build
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
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.ln
import kotlin.math.pow

enum class FractalQuality { Low, Medium, High }
enum class FractalColorIntensity { Low, Medium, High }

/** Rich multi-stop palette for smooth escape-time gradients (CPU path). */
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
    /** Quartic multibrot — z⁴ + c (original AGSL showcase). */
    Nova,
    /** Newton basins for z³ − 1 = 0. */
    Newton,
    /**
     * Mandelbrot AGSL from [tbahlai/agsl](https://github.com/tbahlai/agsl) —
     * 4-stop palette, pinch/pan zoom.
     */
    MandelbrotGlow,
    /**
     * Interactive Julia AGSL from [tbahlai/agsl](https://github.com/tbahlai/agsl) —
     * touch drives the Julia constant.
     */
    JuliaTouch,
}

/**
 * Animated fractal background with a slow infinite zoom-in effect.
 * Cycles through different fractal types unless [forceType] is set.
 *
 * On API 33+ renders via AGSL [RuntimeShader] (GPU per-pixel). Older devices
 * keep the Compose Canvas CPU grid. Julia reacts to drag when forced or active.
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

    // Dedicated tbahlai/agsl Mandelbrot + Julia ports (API 33+)
    if (FractalAgslShaders.supportsAgsl() &&
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    ) {
        when (fractalType) {
            FractalType.MandelbrotGlow -> {
                MandelbrotBahlaiCanvas(isActive = isActive)
                return
            }
            FractalType.JuliaTouch -> {
                JuliaBahlaiCanvas(isActive = isActive)
                return
            }
            else -> Unit
        }
    }

    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(1_000_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "time",
    )
    val brightness by animateFloatAsState(
        targetValue = if (isActive) 1.15f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "brightness"
    )

    // Julia constant — animated drift, overridden by drag
    var juliaCx by remember { mutableFloatStateOf(-0.7f) }
    var juliaCy by remember { mutableFloatStateOf(0.27015f) }
    var draggingJulia by remember { mutableStateOf(false) }
    val juliaDrift by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(28000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "juliaDrift",
    )
    LaunchedEffect(juliaDrift, fractalType, draggingJulia) {
        if ((fractalType != FractalType.Julia && fractalType != FractalType.JuliaTouch) ||
            draggingJulia
        ) {
            return@LaunchedEffect
        }
        val a = juliaDrift * (2f * Math.PI.toFloat())
        juliaCx = -0.7f + 0.12f * kotlin.math.cos(a)
        juliaCy = 0.27015f + 0.1f * kotlin.math.sin(a * 1.3f)
    }

    val interactive = fractalType == FractalType.Julia || fractalType == FractalType.JuliaTouch
    val dragModifier = if (interactive) {
        Modifier.pointerInput(fractalType) {
            detectDragGestures(
                onDragStart = { draggingJulia = true },
                onDragEnd = { draggingJulia = false },
                onDragCancel = { draggingJulia = false },
                onDrag = { change, _ ->
                    change.consume()
                    val nx = (change.position.x / size.width).coerceIn(0f, 1f)
                    val ny = (change.position.y / size.height).coerceIn(0f, 1f)
                    juliaCx = (nx - 0.5f) * 2.4f
                    juliaCy = (0.5f - ny) * 2.4f
                },
            )
        }
    } else {
        Modifier
    }

    if (FractalAgslShaders.supportsAgsl() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        FractalAgslCanvas(
            fractalType = fractalType,
            zoom = zoom,
            phase = phase,
            time = time,
            brightness = brightness,
            quality = quality,
            juliaCx = juliaCx,
            juliaCy = juliaCy,
            modifier = Modifier.fillMaxSize().then(dragModifier),
        )
    } else {
        FractalCpuCanvas(
            fractalType = fractalType,
            zoom = zoom,
            phase = phase,
            brightness = brightness,
            isActive = isActive,
            paletteColors = paletteColors,
            quality = quality,
            colorIntensity = colorIntensity,
            juliaCx = juliaCx,
            juliaCy = juliaCy,
            modifier = Modifier.fillMaxSize().then(dragModifier),
        )
    }
}

@Composable
private fun FractalAgslCanvas(
    fractalType: FractalType,
    zoom: Float,
    phase: Float,
    time: Float,
    brightness: Float,
    quality: FractalQuality,
    juliaCx: Float,
    juliaCy: Float,
    modifier: Modifier,
) {
    val shader = remember { FractalAgslShaders.createEscapeShader() }
    val brush = remember(shader) { ShaderBrush(shader) }
    val maxIter = when (quality) {
        FractalQuality.Low -> 160f
        FractalQuality.Medium -> 280f
        FractalQuality.High -> 420f
    }
    val (cx, cy) = fractalCenter(fractalType)

    Canvas(modifier = modifier) {
        shader.setFloatUniform("iResolution", size.width, size.height)
        shader.setFloatUniform("iCenter", cx, cy)
        shader.setFloatUniform("iZoom", zoom)
        shader.setFloatUniform("iTime", time)
        shader.setFloatUniform("iMaxIter", maxIter)
        shader.setFloatUniform("iType", fractalType.ordinal.toFloat())
        shader.setFloatUniform("iJuliaC", juliaCx, juliaCy)
        shader.setFloatUniform("iPhase", phase)
        shader.setFloatUniform("iBrightness", brightness)
        drawRect(brush = brush)
    }
}

@Composable
private fun FractalCpuCanvas(
    fractalType: FractalType,
    zoom: Float,
    phase: Float,
    brightness: Float,
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: FractalQuality,
    colorIntensity: FractalColorIntensity,
    juliaCx: Float,
    juliaCy: Float,
    modifier: Modifier,
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val centerX = w / 2
        val centerY = h / 2
        val (cx, cy) = fractalCenter(fractalType)

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
        val halfSpan = 2.2f / zoom

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
                val sx = (ix + 0.5f) * cellW
                val sy = (iy + 0.5f) * cellH
                val re = cx - halfSpan + (sx / w) * (2f * halfSpan)
                val im = cy - halfSpan + (1f - sy / h) * (2f * halfSpan)

                val continuous = when (fractalType) {
                    FractalType.Mandelbrot -> mandelbrotSmooth(re, im, maxIter)
                    FractalType.Julia -> juliaSmooth(re, im, maxIter, juliaCx, juliaCy)
                    FractalType.BurningShip -> burningShipSmooth(re, im, maxIter)
                    FractalType.Tricorn -> tricornSmooth(re, im, maxIter)
                    FractalType.Multibrot -> multibrotSmooth(re, im, maxIter)
                    FractalType.Celtic -> celticSmooth(re, im, maxIter)
                    FractalType.Buffalo -> buffaloSmooth(re, im, maxIter)
                    FractalType.Phoenix -> phoenixSmooth(re, im, maxIter)
                    FractalType.Nova -> novaSmooth(re, im, maxIter)
                    FractalType.Newton -> newtonSmooth(re, im, maxIter)
                    FractalType.MandelbrotGlow -> mandelbrotSmooth(re, im, maxIter)
                    FractalType.JuliaTouch -> juliaSmooth(re, im, maxIter, juliaCx, juliaCy)
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
                    size = Size(cellW + 1f, cellH + 1f),
                )
            }
        }

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

internal fun fractalCenter(type: FractalType): Pair<Float, Float> = when (type) {
    FractalType.Mandelbrot -> 0.25f to 0.0f
    FractalType.Julia -> 0.0f to 0.0f
    FractalType.BurningShip -> -1.75f to -0.02f
    FractalType.Tricorn -> -0.25f to 0.0f
    FractalType.Multibrot -> 0.0f to 0.0f
    FractalType.Celtic -> -0.5f to 0.0f
    FractalType.Buffalo -> -0.65f to -0.45f
    FractalType.Phoenix -> 0.0f to 0.0f
    FractalType.Nova -> 0.0f to 0.0f
    FractalType.Newton -> 0.0f to 0.0f
    FractalType.MandelbrotGlow -> -0.5f to 0.0f
    FractalType.JuliaTouch -> 0.0f to 0.0f
}

/** Continuous escape-time iteration, or -1 when inside the set. */
private fun smoothEscape(n: Int, zr2: Double, zi2: Double, power: Double = 2.0): Float {
    val logZn = ln((zr2 + zi2).coerceAtLeast(1e-12)) / 2.0
    val nu = ln((logZn / ln(2.0)).coerceAtLeast(1e-12)) / ln(power)
    return (n + 1.0 - nu).toFloat()
}

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

private fun juliaSmooth(zrStart: Float, ziStart: Float, maxIter: Int, cr: Float, ci: Float): Float {
    var zr = zrStart.toDouble()
    var zi = ziStart.toDouble()
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

private fun burningShipSmooth(cr: Float, ci: Float, maxIter: Int): Float {
    var zr = 0.0
    var zi = 0.0
    var n = 0
    while (n < maxIter) {
        val zr2 = zr * zr
        val zi2 = zi * zi
        if (zr2 + zi2 > 4.0) return smoothEscape(n, zr2, zi2)
        val newZr = zr2 - zi2 + cr
        val newZi = abs(2.0 * zr * zi) + ci
        zr = abs(newZr)
        zi = newZi
        n++
    }
    return -1f
}

private fun tricornSmooth(cr: Float, ci: Float, maxIter: Int): Float {
    var zr = 0.0
    var zi = 0.0
    var n = 0
    while (n < maxIter) {
        val zr2 = zr * zr
        val zi2 = zi * zi
        if (zr2 + zi2 > 4.0) return smoothEscape(n, zr2, zi2)
        val nextZr = zr2 - zi2 + cr
        val nextZi = -2.0 * zr * zi + ci
        zr = nextZr
        zi = nextZi
        n++
    }
    return -1f
}

private fun multibrotSmooth(cr: Float, ci: Float, maxIter: Int): Float {
    var zr = 0.0
    var zi = 0.0
    var n = 0
    while (n < maxIter) {
        val zr2 = zr * zr
        val zi2 = zi * zi
        if (zr2 + zi2 > 4.0) return smoothEscape(n, zr2, zi2, power = 3.0)
        val nextZr = zr * (zr2 - 3.0 * zi2) + cr
        val nextZi = zi * (3.0 * zr2 - zi2) + ci
        zr = nextZr
        zi = nextZi
        n++
    }
    return -1f
}

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

/** Quartic multibrot: z⁴ + c. */
private fun novaSmooth(cr: Float, ci: Float, maxIter: Int): Float {
    var zr = 0.0
    var zi = 0.0
    var n = 0
    while (n < maxIter) {
        val zr2 = zr * zr
        val zi2 = zi * zi
        if (zr2 + zi2 > 4.0) return smoothEscape(n, zr2, zi2, power = 4.0)
        val z3r = zr * (zr2 - 3.0 * zi2)
        val z3i = zi * (3.0 * zr2 - zi2)
        val nextZr = z3r * zr - z3i * zi + cr
        val nextZi = z3r * zi + z3i * zr + ci
        zr = nextZr
        zi = nextZi
        n++
    }
    return -1f
}

/** Newton basins for f(z) = z³ − 1. */
private fun newtonSmooth(zrStart: Float, ziStart: Float, maxIter: Int): Float {
    var zr = zrStart.toDouble()
    var zi = ziStart.toDouble()
    var n = 0
    while (n < maxIter) {
        val zr2 = zr * zr
        val zi2 = zi * zi
        val z2r = zr2 - zi2
        val z2i = 2.0 * zr * zi
        val invDen = 1.0 / (z2r * z2r + z2i * z2i).coerceAtLeast(1e-12)
        val invR = z2r * invDen
        val invI = -z2i * invDen
        val nextZr = (2.0 / 3.0) * zr + (1.0 / 3.0) * invR
        val nextZi = (2.0 / 3.0) * zi + (1.0 / 3.0) * invI
        val dr = nextZr - zr
        val di = nextZi - zi
        zr = nextZr
        zi = nextZi
        if (dr * dr + di * di < 1e-10) {
            val angle = atan2(zi, zr)
            return ((angle / (2.0 * Math.PI) + 0.5 + n * 0.002) * maxIter * 0.15).toFloat()
        }
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
        return Color(0xFF020617)
    }

    val safePalette = if (paletteColors.size >= 2) paletteColors else DefaultPalette

    val cycles = when (intensity) {
        FractalColorIntensity.Low -> 2.2f
        FractalColorIntensity.Medium -> 4.5f
        FractalColorIntensity.High -> 7.5f
    }

    val normalized = (ln(1.0 + continuous.toDouble()) / ln(1.0 + maxIter.toDouble())).toFloat()
    val t = ((normalized * cycles + phase).mod(1f) + 1f).mod(1f)

    val color = samplePalette(safePalette, t)

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

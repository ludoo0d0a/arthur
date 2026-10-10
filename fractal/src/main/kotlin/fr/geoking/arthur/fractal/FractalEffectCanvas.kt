package fr.geoking.arthur.fractal

import android.graphics.Bitmap
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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield

enum class FractalQuality { Low, Medium, High }
enum class FractalColorIntensity { Low, Medium, High }

/** Rich multi-stop palette for smooth escape-time gradients (CPU path). */
private val DefaultPalette = FractalCoherentPalette.escapeStops(seed = 1, count = 12)

private const val CpuBakeLongSidePx = 1120
private const val CpuBakeIntervalMs = 90L
private const val CpuCrossfadeMs = 280

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
 * bake a medium-res bitmap and upscale with soft crossfades. Julia reacts to
 * drag when forced or active.
 */
@Composable
fun FractalEffectCanvas(
    isActive: Boolean,
    paletteColors: List<Color> = DefaultPalette,
    quality: FractalQuality = FractalQuality.Medium,
    colorIntensity: FractalColorIntensity = FractalColorIntensity.Medium,
    forceType: FractalType? = null,
    /** Picks a coherent theme for AGSL Mandelbrot Glow / Julia Touch / escape uniforms. */
    colorSeed: Int = 1,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "fractal_zoom")
    val zoomProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            // Zen: slow zoom (~2.5 min one way), reverse avoids hard restart jump
            animation = tween(150_000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "zoomProgress",
    )
    val zoom = 1f + zoomProgress * 39f

    var zoomCycleCount by remember { mutableStateOf(0) }
    var lastZoomProgress by remember { mutableFloatStateOf(0f) }
    var zoomAscending by remember { mutableStateOf(true) }

    LaunchedEffect(zoomProgress, forceType) {
        if (forceType != null) return@LaunchedEffect
        if (zoomAscending && zoomProgress < lastZoomProgress) {
            zoomAscending = false
        } else if (!zoomAscending && zoomProgress > lastZoomProgress) {
            zoomCycleCount++
            zoomAscending = true
        }
        lastZoomProgress = zoomProgress
    }

    val cyclingType = remember(zoomCycleCount) {
        FractalType.entries[zoomCycleCount % FractalType.entries.size]
    }
    val fractalType = forceType ?: cyclingType
    val resolvedPalette = remember(colorSeed, paletteColors) {
        if (paletteColors === DefaultPalette || paletteColors.size < 4) {
            FractalCoherentPalette.escapeStops(colorSeed, count = 12)
        } else {
            paletteColors
        }
    }

    // Dedicated tbahlai/agsl Mandelbrot + Julia ports (API 33+)
    if (FractalAgslShaders.supportsAgsl() &&
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    ) {
        when (fractalType) {
            FractalType.MandelbrotGlow -> {
                MandelbrotBahlaiCanvas(isActive = isActive, colorSeed = colorSeed)
                return
            }
            FractalType.JuliaTouch -> {
                JuliaBahlaiCanvas(isActive = isActive, colorSeed = colorSeed)
                return
            }
            else -> Unit
        }
    }

    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(180_000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "phase",
    )
    val brightness by animateFloatAsState(
        targetValue = if (isActive) 1.15f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "brightness",
    )

    // Julia constant — gentle orbit like JuliaBahlaiCanvas, overridden by drag
    var juliaCx by remember { mutableFloatStateOf(-0.7f) }
    var juliaCy by remember { mutableFloatStateOf(0.27015f) }
    var draggingJulia by remember { mutableStateOf(false) }
    val juliaOrbit by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(120_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "juliaOrbit",
    )
    LaunchedEffect(juliaOrbit, fractalType, draggingJulia) {
        if ((fractalType != FractalType.Julia && fractalType != FractalType.JuliaTouch) ||
            draggingJulia
        ) {
            return@LaunchedEffect
        }
        val a = juliaOrbit * (2f * PI.toFloat())
        juliaCx = -0.7f + 0.12f * cos(a)
        juliaCy = 0.27015f + 0.1f * sin(a * 1.15f)
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
            brightness = brightness,
            quality = quality,
            juliaCx = juliaCx,
            juliaCy = juliaCy,
            colorSeed = colorSeed,
            modifier = Modifier.fillMaxSize().then(dragModifier),
        )
    } else {
        FractalCpuCanvas(
            fractalType = fractalType,
            zoom = zoom,
            phase = phase,
            brightness = brightness,
            isActive = isActive,
            paletteColors = resolvedPalette,
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
    brightness: Float,
    quality: FractalQuality,
    juliaCx: Float,
    juliaCy: Float,
    colorSeed: Int,
    modifier: Modifier,
) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

    val shader = remember { FractalAgslShaders.createEscapeShader() }
    val stops = remember(colorSeed) { FractalCoherentPalette.fourStops(colorSeed) }
    val maxIter = when (quality) {
        FractalQuality.Low -> 120f
        FractalQuality.Medium -> 180f
        FractalQuality.High -> 240f
    }
    val (cx, cy) = fractalCenter(fractalType)

    BoxWithConstraints(modifier = modifier) {
        val density = LocalDensity.current
        val boxMaxWidth = maxWidth
        val boxMaxHeight = maxHeight
        val minSidePx = with(density) {
            minOf(boxMaxWidth.toPx(), boxMaxHeight.toPx())
        }
        val renderScale = if (minSidePx >= 1080f) 0.75f else 1f

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            val shaderModifier = if (renderScale < 1f) {
                Modifier
                    .requiredSize(
                        width = boxMaxWidth * renderScale,
                        height = boxMaxHeight * renderScale,
                    )
                    .graphicsLayer {
                        scaleX = 1f / renderScale
                        scaleY = 1f / renderScale
                        transformOrigin = TransformOrigin.Center
                    }
            } else {
                Modifier.fillMaxSize()
            }

            AgslShaderBox(
                shader = shader,
                modifier = shaderModifier,
                cacheKey = colorSeed,
                onConfigure = { size, s ->
                    s.setFloatUniform("iResolution", size.width, size.height)
                    s.setColorUniform("color1", stops[0].toArgb())
                    s.setColorUniform("color2", stops[1].toArgb())
                    s.setColorUniform("color3", stops[2].toArgb())
                    s.setColorUniform("color4", stops[3].toArgb())
                },
                onDrawFrame = { _, s ->
                    s.setFloatUniform("iCenter", cx, cy)
                    s.setFloatUniform("iZoom", zoom)
                    s.setFloatUniform("iMaxIter", maxIter)
                    s.setFloatUniform("iType", fractalType.ordinal.toFloat())
                    s.setFloatUniform("iJuliaC", juliaCx, juliaCy)
                    s.setFloatUniform("iPhase", phase)
                    s.setFloatUniform("iBrightness", brightness)
                },
            )
        }
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
    var base by remember { mutableStateOf<Bitmap?>(null) }
    var overlay by remember { mutableStateOf<Bitmap?>(null) }
    var overlayAlphaTarget by remember { mutableFloatStateOf(0f) }
    var layoutSize by remember { mutableStateOf(IntSize.Zero) }

    val overlayAlpha by animateFloatAsState(
        targetValue = overlayAlphaTarget,
        animationSpec = tween(CpuCrossfadeMs, easing = LinearEasing),
        label = "cpu_fractal_crossfade",
    )

    LaunchedEffect(overlayAlpha, overlay) {
        if (overlay != null && overlayAlpha >= 0.99f) {
            val done = overlay ?: return@LaunchedEffect
            base?.recycle()
            base = done
            overlay = null
            overlayAlphaTarget = 0f
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            base?.recycle()
            overlay?.recycle()
        }
    }

    LaunchedEffect(
        fractalType,
        quality,
        colorIntensity,
        paletteColors,
        layoutSize,
    ) {
        val w = layoutSize.width
        val h = layoutSize.height
        if (w <= 0 || h <= 0) return@LaunchedEffect
        val maxIter = when (quality) {
            FractalQuality.Low -> 120
            FractalQuality.Medium -> 200
            FractalQuality.High -> 280
        }
        val aspect = w.toFloat() / h.toFloat()
        while (true) {
            while (overlay != null) {
                yield()
                delay(16)
            }
            val baked = withContext(Dispatchers.Default) {
                bakeFractalBitmap(
                    longSide = CpuBakeLongSidePx,
                    aspect = aspect,
                    fractalType = fractalType,
                    zoom = zoom,
                    phase = phase,
                    brightness = brightness,
                    sceneActive = isActive,
                    paletteColors = paletteColors,
                    colorIntensity = colorIntensity,
                    juliaCx = juliaCx,
                    juliaCy = juliaCy,
                    maxIter = maxIter,
                )
            }
            overlay = baked
            overlayAlphaTarget = 1f
            delay(if (isActive) CpuBakeIntervalMs else CpuBakeIntervalMs * 3)
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { layoutSize = it },
    ) {
        val dst = IntSize(
            size.width.roundToInt().coerceAtLeast(1),
            size.height.roundToInt().coerceAtLeast(1),
        )
        val baseBmp = base
        val overlayBmp = overlay
        if (baseBmp != null && !baseBmp.isRecycled) {
            drawImage(
                image = baseBmp.asImageBitmap(),
                dstSize = dst,
                alpha = 1f,
                filterQuality = FilterQuality.High,
            )
        } else {
            drawRect(Color(0xFF020617))
        }
        if (overlayBmp != null && !overlayBmp.isRecycled && overlayAlpha > 0f) {
            drawImage(
                image = overlayBmp.asImageBitmap(),
                dstSize = dst,
                alpha = overlayAlpha,
                filterQuality = FilterQuality.High,
            )
        }
    }
}

private fun bakeFractalBitmap(
    longSide: Int,
    aspect: Float,
    fractalType: FractalType,
    zoom: Float,
    phase: Float,
    brightness: Float,
    sceneActive: Boolean,
    paletteColors: List<Color>,
    colorIntensity: FractalColorIntensity,
    juliaCx: Float,
    juliaCy: Float,
    maxIter: Int,
): Bitmap {
    val safeAspect = aspect.coerceIn(0.4f, 2.8f)
    val (bw, bh) = if (safeAspect >= 1f) {
        longSide to (longSide / safeAspect).roundToInt().coerceAtLeast(1)
    } else {
        (longSide * safeAspect).roundToInt().coerceAtLeast(1) to longSide
    }
    val bitmap = Bitmap.createBitmap(bw, bh, Bitmap.Config.ARGB_8888)
    val pixels = IntArray(bw * bh)
    val (cx, cy) = fractalCenter(fractalType)
    val halfSpan = 2.2f / zoom

    for (iy in 0 until bh) {
        for (ix in 0 until bw) {
            val sx = (ix + 0.5f) / bw
            val sy = (iy + 0.5f) / bh
            val re = cx - halfSpan + sx * (2f * halfSpan)
            val im = cy - halfSpan + (1f - sy) * (2f * halfSpan)

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
                isActive = sceneActive,
                paletteColors = paletteColors,
                intensity = colorIntensity,
                maxIter = maxIter,
            )
            pixels[iy * bw + ix] = color.toArgb()
        }
    }
    bitmap.setPixels(pixels, 0, bw, 0, 0, bw, bh)
    return bitmap
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

    val normalized = (ln(1.0 + continuous.toDouble()) / ln(1.0 + maxIter.toDouble())).toFloat()
    // Match AGSL / Julia Touch soft curve; intensity gently stretches the ramp.
    val stretch = when (intensity) {
        FractalColorIntensity.Low -> 0.85f
        FractalColorIntensity.Medium -> 1f
        FractalColorIntensity.High -> 1.15f
    }
    val t = ((normalized * stretch + phase * 0.08f).coerceIn(0f, 1f)).pow(0.8f)

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
    val scaled = t * (palette.size - 1).coerceAtLeast(1)
    val i0 = scaled.toInt().coerceIn(0, palette.lastIndex)
    val i1 = (i0 + 1).coerceAtMost(palette.lastIndex)
    val frac = smoothstep(scaled - i0)
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

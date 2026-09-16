package fr.geoking.arthur.genart.engines

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import fr.geoking.arthur.genart.GenartQuality
import fr.geoking.arthur.genart.TonalPalette
import fr.geoking.arthur.genart.phase01
import fr.geoking.arthur.genart.qualityCount
import fr.geoking.arthur.genart.seededRange
import fr.geoking.arthur.genart.seededUnit
import kotlin.math.PI
import kotlin.math.sin

/**
 * Stacked undulating bands with paper-cut drop shadows — Tapet silk / Mitmita feel.
 *
 * Tweakables via [quality]: fold count, segment density, shadow depth.
 * Shape/color layout is seeded-random per fold (cycles, thickness, palette pick).
 */
@Composable
internal fun SilkFoldsEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val count = qualityCount(quality, low = 5, medium = 7, high = 10)
    val segments = qualityCount(quality, low = 28, medium = 40, high = 52)
    val shadowDepth = when (quality) {
        GenartQuality.Low -> 0.55f
        GenartQuality.Medium -> 0.85f
        GenartQuality.High -> 1.1f
    }
    val folds = remember(count) {
        List(count) { i ->
            SilkFold(
                baseY = seededRange(i * 17 + 3, 0.06f, 0.92f),
                amplitude = seededRange(i * 29 + 7, 0.018f, 0.08f),
                cycles = seededRange(i * 41 + 11, 0.8f, 2.8f),
                cycles2 = seededRange(i * 101 + 37, 1.6f, 4.2f),
                amp2Frac = seededRange(i * 107 + 41, 0.15f, 0.45f),
                phase = seededRange(i * 53 + 13, 0f, 2f * PI.toFloat()),
                speedMul = seededRange(i * 67 + 19, 0.35f, 1.15f),
                thickness = seededRange(i * 79 + 23, 0.05f, 0.16f),
                colorIndex = (seededUnit(i * 113 + 47) * 8).toInt(),
                colorMix = seededUnit(i * 127 + 53),
                alpha = seededRange(i * 131 + 59, 0.55f, 0.92f),
            )
        }
    }
    val transition = rememberInfiniteTransition(label = "silk_folds")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((38000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "silk_folds_t",
    )
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val bgA = TonalPalette.brightness(
            TonalPalette.mix(
                TonalPalette.pick(paletteColors, 0),
                Color(0xFF0A0810),
                0.82f,
            ),
            brightness * 0.35f,
        )
        drawRect(brush = Brush.verticalGradient(colors = listOf(bgA, Color(0xFF030208))))
        val time = phase01(t) * 2f * PI.toFloat()
        val dim = if (isActive) 1f else 0.55f
        val shadowPx = (h * 0.012f * shadowDepth).coerceIn(4f, 22f)
        folds.sortedBy { it.baseY }.forEach { fold ->
            val path = buildSilkBand(fold, w, h, time, segments)
            drawPath(
                path = Path().apply { addPath(path, Offset(0f, shadowPx)) },
                color = TonalPalette.withAlpha(Color.Black, 0.28f * dim * shadowDepth),
            )
            val cA = TonalPalette.pick(paletteColors, fold.colorIndex)
            val cB = TonalPalette.pick(paletteColors, fold.colorIndex + 1)
            val tint = TonalPalette.brightness(
                TonalPalette.mix(cA, cB, fold.colorMix),
                brightness,
            )
            drawPath(
                path = path,
                color = TonalPalette.withAlpha(tint, fold.alpha * 0.42f * dim),
            )
            val lip = buildSilkLip(fold, w, h, time, segments, strokeDown = h * 0.008f)
            drawPath(
                path = lip,
                color = TonalPalette.withAlpha(Color.Black, 0.14f * dim * shadowDepth),
            )
        }
    }
}

private fun silkWaveY(
    fold: SilkFold,
    x: Float,
    w: Float,
    h: Float,
    time: Float,
    ampScale: Float,
): Float {
    val amp = fold.amplitude * h * ampScale
    val freq = fold.cycles * 2f * PI.toFloat() / w.coerceAtLeast(1f)
    val freq2 = fold.cycles2 * 2f * PI.toFloat() / w.coerceAtLeast(1f)
    return fold.baseY * h +
        amp * sin(x * freq + time * fold.speedMul + fold.phase) +
        amp * fold.amp2Frac * sin(x * freq2 + time * fold.speedMul * 0.7f + fold.phase * 1.3f)
}

private fun buildSilkBand(
    fold: SilkFold,
    w: Float,
    h: Float,
    time: Float,
    segments: Int,
): Path {
    val path = Path()
    val thick = fold.thickness * h
    for (k in 0..segments) {
        val x = (k / segments.toFloat()) * w
        val y = silkWaveY(fold, x, w, h, time, ampScale = 1f)
        if (k == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    for (k in segments downTo 0) {
        val x = (k / segments.toFloat()) * w
        val y = silkWaveY(fold, x, w, h, time, ampScale = 0.7f) + thick
        path.lineTo(x, y)
    }
    path.close()
    return path
}

private fun buildSilkLip(
    fold: SilkFold,
    w: Float,
    h: Float,
    time: Float,
    segments: Int,
    strokeDown: Float,
): Path {
    val path = Path()
    for (k in 0..segments) {
        val x = (k / segments.toFloat()) * w
        val y = silkWaveY(fold, x, w, h, time, ampScale = 1f)
        if (k == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    for (k in segments downTo 0) {
        val x = (k / segments.toFloat()) * w
        val y = silkWaveY(fold, x, w, h, time, ampScale = 1f) + strokeDown
        path.lineTo(x, y)
    }
    path.close()
    return path
}

private data class SilkFold(
    val baseY: Float,
    val amplitude: Float,
    val cycles: Float,
    val cycles2: Float,
    val amp2Frac: Float,
    val phase: Float,
    val speedMul: Float,
    val thickness: Float,
    val colorIndex: Int,
    val colorMix: Float,
    val alpha: Float,
)

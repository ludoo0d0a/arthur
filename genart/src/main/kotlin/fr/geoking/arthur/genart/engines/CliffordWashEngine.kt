package fr.geoking.arthur.genart.engines

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import fr.geoking.arthur.genart.GenartQuality
import fr.geoking.arthur.genart.TonalPalette
import fr.geoking.arthur.genart.loopedFbm
import fr.geoking.arthur.genart.phase01
import fr.geoking.arthur.genart.qualityCount
import fr.geoking.arthur.genart.seededUnit
import kotlin.math.cos
import kotlin.math.sin

/**
 * Dense luminous wash from a Clifford attractor: points are iterated once in [remember],
 * then drawn with slow rotation and soft bloom — zero per-frame integration.
 */
@Composable
internal fun CliffordWashEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val pointCount = qualityCount(quality, low = 800, medium = 1600, high = 2800)
    val preset = remember {
        val presets = CliffordWashPreset.table
        presets[(seededUnit(101) * presets.size).toInt().coerceIn(0, presets.lastIndex)]
    }
    val points = remember(pointCount, preset) {
        bakeCliffordWashPoints(pointCount, preset)
    }

    val transition = rememberInfiniteTransition(label = "clifford_wash")
    val rotT by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((120000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "clifford_wash_rot",
    )
    val pulseT by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((28000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "clifford_wash_pulse",
    )

    val dim = if (isActive) 1f else 0.55f
    val rot = phase01(rotT) * 360f
    val pulse = 0.85f + 0.15f * loopedFbm(phase01(pulseT), radius = 1.5f, seedOffset = 13)

    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(color = Color(0xFF04020A))
        }

        Canvas(modifier = Modifier.fillMaxSize().blur(14.dp)) {
            val w = size.width
            val h = size.height
            val cx = w * 0.5f
            val cy = h * 0.5f
            val bloom = TonalPalette.brightness(
                TonalPalette.mix(TonalPalette.pick(paletteColors, 0), Color(0xFFAA88FF), 0.3f),
                brightness,
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        TonalPalette.withAlpha(bloom, 0.28f * dim * pulse),
                        Color.Transparent,
                    ),
                    center = Offset(cx, cy),
                    radius = minOf(w, h) * 0.42f,
                ),
                radius = minOf(w, h) * 0.42f,
                center = Offset(cx, cy),
            )
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val minDim = minOf(w, h)
            val cx = w * 0.5f
            val cy = h * 0.5f
            val scale = minDim * 0.22f
            val inkA = TonalPalette.brightness(TonalPalette.pick(paletteColors, 0), brightness)
            val inkB = TonalPalette.brightness(TonalPalette.pick(paletteColors, 2), brightness)
            val dotR = (minDim / 900f).coerceIn(0.6f, 1.8f)

            rotate(degrees = rot, pivot = Offset(cx, cy)) {
                for (i in points.indices) {
                    val p = points[i]
                    val mixT = i / points.size.toFloat()
                    val ink = TonalPalette.mix(inkA, inkB, mixT)
                    val alpha = (0.12f + 0.45f * (1f - mixT * 0.35f)) * dim * pulse
                    drawCircle(
                        color = TonalPalette.withAlpha(ink, alpha),
                        radius = dotR,
                        center = Offset(cx + p.x * scale, cy + p.y * scale),
                    )
                }
            }
        }
    }
}

private data class CliffordWashPreset(
    val a: Float,
    val b: Float,
    val c: Float,
    val d: Float,
) {
    companion object {
        val table = listOf(
            CliffordWashPreset(-1.4f, 1.6f, 1.0f, 0.7f),
            CliffordWashPreset(1.7f, 1.7f, 0.6f, 1.2f),
            CliffordWashPreset(-1.7f, 1.3f, -0.1f, -1.2f),
            CliffordWashPreset(1.5f, -1.8f, 1.6f, 0.9f),
            CliffordWashPreset(-1.8f, -2.0f, -0.5f, -0.9f),
        )
    }
}

private data class CliffordWashPoint(val x: Float, val y: Float)

private fun bakeCliffordWashPoints(count: Int, preset: CliffordWashPreset): List<CliffordWashPoint> {
    var x = 0.1f
    var y = 0.1f
    // Warm up attractor
    repeat(40) {
        val nx = sin(preset.a * y) + preset.c * cos(preset.a * x)
        val ny = sin(preset.b * x) + preset.d * cos(preset.b * y)
        x = nx
        y = ny
    }
    return List(count) {
        val nx = sin(preset.a * y) + preset.c * cos(preset.a * x)
        val ny = sin(preset.b * x) + preset.d * cos(preset.b * y)
        x = nx
        y = ny
        CliffordWashPoint(x, y)
    }
}

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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import fr.geoking.arthur.genart.GenartQuality
import fr.geoking.arthur.genart.TonalPalette
import fr.geoking.arthur.genart.loopedFbm
import fr.geoking.arthur.genart.phase01
import fr.geoking.arthur.genart.qualityCount
import fr.geoking.arthur.genart.seededRange
import fr.geoking.arthur.genart.seededUnit
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private data class AutoDashboardGlowMote(
    val x0: Float,
    val y0: Float,
    val radius: Float,
    val driftX: Float,
    val driftY: Float,
    val phaseOffset: Float,
    val colorIndex: Int,
)

/** Soft Ambient POV Auto Dashboard Glow — seamless loop via loopedFbm (no traffic sim). */
@Composable
internal fun AutoDashboardGlowEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val count = qualityCount(quality, low = 4, medium = 7, high = 11)
    val motes = remember(count) {
        List(count) { i ->
            AutoDashboardGlowMote(
                x0 = seededUnit(i * 17 + 3),
                y0 = seededUnit(i * 29 + 7),
                radius = seededRange(i * 41 + 11, 0.03f, 0.12f),
                driftX = seededRange(i * 53 + 13, 0.02f, 0.08f),
                driftY = seededRange(i * 67 + 19, 0.01f, 0.04f),
                phaseOffset = seededRange(i * 71 + 23, 0f, 2f * PI.toFloat()),
                colorIndex = i,
            )
        }
    }
    val transition = rememberInfiniteTransition(label = "autodashboardglow")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((68000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "autodashboardglow_t",
    )
    val dim = if (isActive) 1f else 0.55f
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val time = phase01(t)
        val wash = loopedFbm(time, radius = 1.35f, octaves = 2, seedOffset = 11)
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    TonalPalette.brightness(Color(0xFF0A0E12), brightness * dim * (0.92f + 0.08f * wash)),
                    TonalPalette.brightness(Color(0xFF141A22), brightness * dim),
                ),
            ),
        )
        val accent = TonalPalette.brightness(
            TonalPalette.mix(Color(0xFF40E0A0), TonalPalette.pick(paletteColors, 0), 0.35f),
            brightness * dim,
        )
        // Soft horizon / vanishing point
        val horizon = h * (0.55f + 0.04f * (wash - 0.5f))
        drawRect(
            brush = Brush.verticalGradient(
                0f to Color.Transparent,
                1f to TonalPalette.withAlpha(accent, 0.12f * dim),
                startY = horizon - h * 0.08f,
                endY = horizon + h * 0.08f,
            ),
        )
        motes.forEach { mote ->
            val n = loopedFbm(time, radius = 1.2f + mote.radius, seedOffset = mote.colorIndex * 97 + 5)
            val x = (mote.x0 + mote.driftX * (n * 2f - 1f) + time * 0.15f + 1f) % 1f * w
            val y = (mote.y0 + mote.driftY * sin(time * 2f * PI.toFloat() + mote.phaseOffset) * 0.5f + 1f) % 1f * h
            val r = minOf(w, h) * mote.radius * (0.85f + 0.3f * n)
            drawCircle(
                color = TonalPalette.withAlpha(
                    TonalPalette.brightness(TonalPalette.pick(paletteColors, mote.colorIndex), brightness * dim),
                    (0.10f + 0.16f * n) * dim,
                ),
                radius = r,
                center = Offset(x, y),
            )
        }
        // Low dashboard silhouette and windscreen edge.
        val dashboardTop = h * 0.70f
        drawRect(
            brush = Brush.verticalGradient(
                0f to TonalPalette.withAlpha(Color(0xFF0A0C10), 0.78f * dim),
                1f to TonalPalette.withAlpha(Color(0xFF050609), 0.98f * dim),
                startY = dashboardTop,
                endY = h,
            ),
            topLeft = Offset(0f, dashboardTop),
            size = Size(w, h - dashboardTop),
        )
        drawLine(
            color = TonalPalette.withAlpha(Color(0xFF29343A), 0.48f * dim),
            start = Offset(0f, dashboardTop),
            end = Offset(w, dashboardTop),
            strokeWidth = minOf(w, h) * 0.008f,
        )

        // Soft instrument spill reflected upward onto the windshield.
        val cyan = TonalPalette.brightness(Color(0xFF40E0A0), brightness * dim)
        val amber = TonalPalette.brightness(Color(0xFFE6A45A), brightness * dim)
        drawCircle(
            brush = Brush.radialGradient(
                0f to TonalPalette.withAlpha(cyan, 0.22f * dim),
                0.55f to TonalPalette.withAlpha(cyan, 0.07f * dim),
                1f to Color.Transparent,
                center = Offset(w * 0.30f, h * 0.70f),
                radius = minOf(w, h) * 0.30f,
            ),
            radius = minOf(w, h) * 0.30f,
            center = Offset(w * 0.30f, h * 0.70f),
        )
        drawCircle(
            brush = Brush.radialGradient(
                0f to TonalPalette.withAlpha(amber, 0.17f * dim),
                1f to Color.Transparent,
                center = Offset(w * 0.72f, h * 0.72f),
                radius = minOf(w, h) * 0.24f,
            ),
            radius = minOf(w, h) * 0.24f,
            center = Offset(w * 0.72f, h * 0.72f),
        )

        // Two recessed gauges with subtle animated needles and tick marks.
        val gaugeY = h * 0.81f
        val gaugeRadius = minOf(w, h) * 0.105f
        listOf(w * 0.27f, w * 0.73f).forEachIndexed { index, gaugeX ->
            val gaugeColor = if (index == 0) cyan else amber
            drawCircle(Color(0xFF07090C), gaugeRadius, Offset(gaugeX, gaugeY))
            drawCircle(
                TonalPalette.withAlpha(Color(0xFF54616A), 0.72f * dim),
                gaugeRadius,
                Offset(gaugeX, gaugeY),
                style = Stroke(width = minOf(w, h) * 0.006f),
            )
            drawCircle(
                TonalPalette.withAlpha(gaugeColor, 0.18f * dim),
                gaugeRadius * 0.82f,
                Offset(gaugeX, gaugeY),
                style = Stroke(width = minOf(w, h) * 0.004f),
            )
            for (tick in 0..8) {
                val angle = (-PI * 0.82 + tick * (PI * 1.64 / 8.0)).toFloat()
                val inner = gaugeRadius * 0.66f
                val outer = gaugeRadius * 0.76f
                drawLine(
                    color = TonalPalette.withAlpha(gaugeColor, 0.55f * dim),
                    start = Offset(gaugeX + cos(angle) * inner, gaugeY + sin(angle) * inner),
                    end = Offset(gaugeX + cos(angle) * outer, gaugeY + sin(angle) * outer),
                    strokeWidth = minOf(w, h) * 0.003f,
                )
            }
            val needleAngle = (-PI * 0.60 + (0.42 + 0.06 * sin(time * 2f * PI.toFloat() + index)) * PI * 1.2).toFloat()
            drawLine(
                color = TonalPalette.withAlpha(gaugeColor, 0.95f * dim),
                start = Offset(gaugeX, gaugeY),
                end = Offset(
                    gaugeX + cos(needleAngle) * gaugeRadius * 0.60f,
                    gaugeY + sin(needleAngle) * gaugeRadius * 0.60f,
                ),
                strokeWidth = minOf(w, h) * 0.004f,
            )
            drawCircle(gaugeColor, minOf(w, h) * 0.008f, Offset(gaugeX, gaugeY))
        }

        // Central console, softly lit controls, and a small shifter silhouette.
        drawRoundRect(
            color = Color(0xFF0B0D11),
            topLeft = Offset(w * 0.43f, h * 0.74f),
            size = Size(w * 0.14f, h * 0.25f),
            cornerRadius = CornerRadius(minOf(w, h) * 0.035f),
        )
        drawRoundRect(
            color = TonalPalette.withAlpha(cyan, 0.28f * dim),
            topLeft = Offset(w * 0.455f, h * 0.77f),
            size = Size(w * 0.09f, h * 0.035f),
            cornerRadius = CornerRadius(minOf(w, h) * 0.008f),
        )
        for (i in 0..2) {
            drawCircle(
                color = TonalPalette.withAlpha(if (i == 1) amber else cyan, 0.74f * dim),
                radius = minOf(w, h) * 0.009f,
                center = Offset(w * (0.46f + 0.04f * i), h * 0.84f),
            )
        }
        drawLine(
            color = TonalPalette.withAlpha(Color(0xFF42474A), 0.65f * dim),
            start = Offset(w * 0.59f, h * 0.92f),
            end = Offset(w * 0.63f, h * 0.82f),
            strokeWidth = minOf(w, h) * 0.018f,
        )
        drawCircle(Color(0xFF22272A), minOf(w, h) * 0.022f, Offset(w * 0.635f, h * 0.81f))
    }
}

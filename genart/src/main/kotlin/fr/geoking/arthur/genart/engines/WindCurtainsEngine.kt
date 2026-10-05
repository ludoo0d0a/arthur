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
import fr.geoking.arthur.genart.sin01
import kotlin.math.PI
import kotlin.math.sin

/**
 * Floor-to-ceiling translucent curtains with deep vertical folds that sway and ripple in a shared gust.
 */
@Composable
internal fun WindCurtainsEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val panelCount = qualityCount(quality, low = 4, medium = 5, high = 7)
    val foldsPerPanel = qualityCount(quality, low = 3, medium = 4, high = 5)
    val segments = qualityCount(quality, low = 22, medium = 32, high = 42)
    val panels = remember(panelCount, foldsPerPanel) {
        List(panelCount) { i ->
            CurtainPanel(
                x0 = i / panelCount.toFloat(),
                widthFrac = 1.1f / panelCount,
                phase = seededRange(i * 17 + 3, 0f, 2f * PI.toFloat()),
                speedMul = seededRange(i * 29 + 7, 0.45f, 1.1f),
                swayAmp = seededRange(i * 41 + 11, 0.018f, 0.045f),
                rippleCycles = seededRange(i * 53 + 13, 1.4f, 2.8f),
                colorIndex = i,
                colorMix = seededUnit(i * 67 + 19),
                alpha = seededRange(i * 79 + 23, 0.28f, 0.52f),
                folds = List(foldsPerPanel) { f ->
                    CurtainFold(
                        offset = (f + 0.5f) / foldsPerPanel,
                        ampFrac = seededRange(i * 100 + f * 13 + 5, 0.35f, 1f),
                        phase = seededRange(i * 100 + f * 17 + 7, 0f, 2f * PI.toFloat()),
                    )
                },
            )
        }
    }

    val transition = rememberInfiniteTransition(label = "wind_curtains")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((36000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "wind_curtains_t",
    )
    val gustT by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((13000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "wind_curtains_gust",
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF1A1420), Color(0xFF0A0810), Color(0xFF040308)),
                center = Offset(w * 0.5f, h * 0.4f),
                radius = maxOf(w, h) * 0.9f,
            ),
        )
        // Soft window light behind the drapes.
        drawRect(
            brush = Brush.horizontalGradient(
                colors = listOf(
                    Color.Transparent,
                    TonalPalette.withAlpha(Color(0xFFE8D5B8), 0.12f),
                    TonalPalette.withAlpha(Color(0xFFF2E6D0), 0.18f),
                    TonalPalette.withAlpha(Color(0xFFE8D5B8), 0.12f),
                    Color.Transparent,
                ),
            ),
        )

        val time = phase01(t) * 2f * PI.toFloat()
        val gust = 0.35f + 0.65f * sin01(phase01(gustT) * 2f * PI.toFloat())
        val dim = if (isActive) 1f else 0.55f

        panels.forEach { panel ->
            val cA = TonalPalette.pick(paletteColors, panel.colorIndex)
            val cB = TonalPalette.pick(paletteColors, panel.colorIndex + 1)
            val tint = TonalPalette.brightness(TonalPalette.mix(cA, cB, panel.colorMix), brightness)
            panel.folds.forEach { fold ->
                val path = buildCurtainFold(panel, fold, w, h, time, gust, segments)
                drawPath(
                    path = path,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            TonalPalette.withAlpha(TonalPalette.mix(tint, Color.White, 0.25f), panel.alpha * 0.9f * dim),
                            TonalPalette.withAlpha(tint, panel.alpha * dim),
                            TonalPalette.withAlpha(tint, panel.alpha * 0.7f * dim),
                        ),
                    ),
                )
                // Dark valley between folds for depth.
                drawPath(
                    path = path,
                    color = TonalPalette.withAlpha(Color.Black, 0.08f * dim * fold.ampFrac),
                )
            }
        }
    }
}

private fun buildCurtainFold(
    panel: CurtainPanel,
    fold: CurtainFold,
    w: Float,
    h: Float,
    time: Float,
    gust: Float,
    segments: Int,
): Path {
    val baseX = (panel.x0 + fold.offset * panel.widthFrac) * w
    val half = panel.widthFrac * w * 0.22f
    val sway = panel.swayAmp * w * gust * fold.ampFrac
    val path = Path()
    for (k in 0..segments) {
        val u = k / segments.toFloat()
        val y = u * h
        val hang = u * u
        val ripple = sin(u * panel.rippleCycles * 2f * PI.toFloat() + time * panel.speedMul + panel.phase + fold.phase)
        val x = baseX - half + sway * hang * ripple
        if (k == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    for (k in segments downTo 0) {
        val u = k / segments.toFloat()
        val y = u * h
        val hang = u * u
        val ripple = sin(u * panel.rippleCycles * 2f * PI.toFloat() + time * panel.speedMul + panel.phase + fold.phase + 0.4f)
        val x = baseX + half + sway * hang * ripple
        path.lineTo(x, y)
    }
    path.close()
    return path
}

private data class CurtainPanel(
    val x0: Float,
    val widthFrac: Float,
    val phase: Float,
    val speedMul: Float,
    val swayAmp: Float,
    val rippleCycles: Float,
    val colorIndex: Int,
    val colorMix: Float,
    val alpha: Float,
    val folds: List<CurtainFold>,
)

private data class CurtainFold(
    val offset: Float,
    val ampFrac: Float,
    val phase: Float,
)

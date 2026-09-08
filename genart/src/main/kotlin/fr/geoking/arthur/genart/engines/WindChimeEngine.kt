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
import androidx.compose.ui.graphics.StrokeCap
import fr.geoking.arthur.genart.GenartQuality
import fr.geoking.arthur.genart.TonalPalette
import fr.geoking.arthur.genart.phase01
import fr.geoking.arthur.genart.qualityCount
import fr.geoking.arthur.genart.seededRange
import fr.geoking.arthur.genart.sin01
import kotlin.math.PI

private data class WindChimeSeed(
    val xFrac: Float,
    val lengthFrac: Float,
    val widthFrac: Float,
    val phaseOffset: Float,
    val colorIndex: Int,
)

/** Cozy hanging silhouettes swaying together with one shared, slowly gusting breeze. */
@Composable
internal fun WindChimeEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val count = qualityCount(quality, low = 2, medium = 3, high = 4)
    val chimes = remember(count) {
        List(count) { i ->
            WindChimeSeed(
                xFrac = (i + 0.5f) / count + seededRange(i * 13 + 3, -0.04f, 0.04f),
                lengthFrac = seededRange(i * 19 + 7, 0.22f, 0.42f),
                widthFrac = seededRange(i * 23 + 11, 0.045f, 0.07f),
                phaseOffset = seededRange(i * 29 + 17, 0f, 2f * PI.toFloat()),
                colorIndex = i,
            )
        }
    }

    val transition = rememberInfiniteTransition(label = "windchime")
    val gustT by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((13000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "windchime_gust",
    )
    val swayT by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((4200 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "windchime_sway",
    )

    val dim = if (isActive) 1f else 0.55f
    val gust = 0.25f + 0.75f * sin01(phase01(gustT) * 2f * PI.toFloat())
    val swayAngle = sin01(phase01(swayT) * 2f * PI.toFloat()) * 2f - 1f

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF3A2E28), Color(0xFF211A16)),
            ),
        )

        val railY = h * 0.12f
        drawLine(
            color = TonalPalette.withAlpha(Color(0xFFD8C3A5), 0.35f * dim),
            start = Offset(w * 0.08f, railY),
            end = Offset(w * 0.92f, railY),
            strokeWidth = 2f,
            cap = StrokeCap.Round,
        )

        chimes.forEach { chime ->
            val localSway = sin01(swayAngle * PI.toFloat() * 0.5f + chime.phaseOffset)
            val sway = (localSway * 2f - 1f) * gust * 0.16f
            val anchor = Offset(chime.xFrac * w, railY)
            val length = chime.lengthFrac * h
            val bottom = Offset(anchor.x + sway * length, anchor.y + length)

            drawLine(
                color = TonalPalette.withAlpha(Color(0xFFC9B79A), 0.5f * dim),
                start = anchor,
                end = bottom,
                strokeWidth = 1.5f,
                cap = StrokeCap.Round,
            )

            val color = TonalPalette.brightness(
                TonalPalette.pick(paletteColors, chime.colorIndex),
                brightness,
            )
            val bodyColor = TonalPalette.mix(Color(0xFF8A6E52), color, 0.35f)
            val width = chime.widthFrac * w
            val bodyHeight = length * 0.32f

            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        TonalPalette.withAlpha(bodyColor, 0.9f * dim),
                        TonalPalette.withAlpha(bodyColor, 0.6f * dim),
                    ),
                    startY = bottom.y - bodyHeight / 2f,
                    endY = bottom.y + bodyHeight / 2f,
                ),
                topLeft = Offset(bottom.x - width / 2f, bottom.y - bodyHeight / 2f),
                size = Size(width, bodyHeight),
                cornerRadius = CornerRadius(width * 0.4f, width * 0.4f),
            )
        }
    }
}

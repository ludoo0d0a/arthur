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

/** Layered curtain fold shapes with angular color transitions and soft drop shadow contours. */
@Composable
internal fun SpectralFoldsEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val count = qualityCount(quality, low = 5, medium = 8, high = 12)
    val folds = remember(count) {
        List(count) { i ->
            SpectralFold(
                xCenterFrac = seededUnit(i * 13 + 7),
                widthFrac = seededRange(i * 19 + 11, 0.25f, 0.5f),
                waveSpeed = seededRange(i * 29 + 17, 0.7f, 1.4f),
                phase = seededRange(i * 37 + 23, 0f, 2f * PI.toFloat()),
                colorIdx = i,
            )
        }
    }
    val transition = rememberInfiniteTransition(label = "spectral_folds")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((32000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "spectral_folds_t",
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        drawRect(color = Color(0xFF06040A))
        val time = phase01(t)
        val dim = if (isActive) 1f else 0.55f

        folds.forEach { fold ->
            val c = TonalPalette.brightness(TonalPalette.pick(paletteColors, fold.colorIdx), brightness)
            val wave = sin(time * 2f * PI.toFloat() * fold.waveSpeed + fold.phase)
            val cx = (fold.xCenterFrac + wave * 0.08f) * w
            val fw = fold.widthFrac * w
            val shadowOffset = w * 0.03f

            val shadowPath = Path().apply {
                moveTo(cx - fw / 2f + shadowOffset, 0f)
                lineTo(cx + fw / 2f + shadowOffset, 0f)
                lineTo(cx + fw * 0.2f + shadowOffset, h)
                lineTo(cx - fw * 0.2f + shadowOffset, h)
                close()
            }

            val foldPath = Path().apply {
                moveTo(cx - fw / 2f, 0f)
                lineTo(cx + fw / 2f, 0f)
                lineTo(cx + fw * 0.2f, h)
                lineTo(cx - fw * 0.2f, h)
                close()
            }

            // Drop shadow contour
            drawPath(
                path = shadowPath,
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.45f * dim),
                        Color.Transparent,
                    ),
                    startX = cx - fw / 2f + shadowOffset,
                    endX = cx + fw / 2f + shadowOffset,
                ),
            )

            // Spectral fold body
            drawPath(
                path = foldPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        TonalPalette.withAlpha(c, 0.65f * dim),
                        TonalPalette.withAlpha(c, 0.25f * dim),
                        Color.Transparent,
                    ),
                    startY = 0f,
                    endY = h,
                ),
            )
        }
    }
}

private data class SpectralFold(
    val xCenterFrac: Float,
    val widthFrac: Float,
    val waveSpeed: Float,
    val phase: Float,
    val colorIdx: Int,
)

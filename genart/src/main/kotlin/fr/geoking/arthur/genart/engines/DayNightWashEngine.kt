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
import fr.geoking.arthur.genart.GenartQuality
import fr.geoking.arthur.genart.TonalPalette
import fr.geoking.arthur.genart.qualityCount
import fr.geoking.arthur.genart.seededRange
import fr.geoking.arthur.genart.seededUnit
import fr.geoking.arthur.genart.sin01
import kotlin.math.PI

/** Very slow crossfade between a warm day sky and a cool night sky. */
@Composable
internal fun DayNightWashEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val starCount = qualityCount(quality, low = 10, medium = 18, high = 28)
    val stars = remember(starCount) {
        List(starCount) { i ->
            DayNightStar(
                x = seededUnit(i * 17 + 3),
                y = seededUnit(i * 29 + 7),
                radius = seededRange(i * 41 + 11, 1.0f, 2.6f),
                twinkleFreq = seededRange(i * 53 + 13, 0.3f, 1.0f),
                phaseOffset = seededRange(i * 67 + 19, 0f, 2f * PI.toFloat()),
                colorIndex = i,
            )
        }
    }

    val transition = rememberInfiniteTransition(label = "daynightwash")
    val cycleMs = (180000 / speed.coerceAtLeast(0.2f)).toInt()
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(cycleMs, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "daynightwash_t",
    )

    val activeScale = if (isActive) 1f else 0.55f
    val dayNightT = sin01(t * 2f * PI.toFloat() - PI.toFloat() / 2f)

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        val dayTop = TonalPalette.brightness(TonalPalette.pick(paletteColors, 0), brightness * 1.05f)
        val dayBot = TonalPalette.brightness(TonalPalette.pick(paletteColors, 1), brightness * 0.9f)
        val nightTop = Color(0xFF060814)
        val nightBot = Color(0xFF01020A)

        val skyTop = TonalPalette.mix(dayTop, nightTop, dayNightT)
        val skyBot = TonalPalette.mix(dayBot, nightBot, dayNightT)

        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(skyTop, skyBot),
            ),
        )

        val nightAmount = (dayNightT * activeScale).coerceIn(0f, 1f)
        if (nightAmount > 0.02f) {
            stars.forEach { star ->
                val twinkle = 0.5f + 0.5f * sin01(t * 2f * PI.toFloat() * star.twinkleFreq + star.phaseOffset)
                val base = TonalPalette.pick(paletteColors, star.colorIndex)
                val color = TonalPalette.brightness(base, brightness.coerceAtMost(1.2f))
                val alpha = nightAmount * (0.25f + 0.55f * twinkle)
                drawCircle(
                    color = TonalPalette.withAlpha(color, alpha),
                    radius = star.radius,
                    center = Offset(star.x * w, star.y * h),
                )
            }
        }
    }
}

private data class DayNightStar(
    val x: Float,
    val y: Float,
    val radius: Float,
    val twinkleFreq: Float,
    val phaseOffset: Float,
    val colorIndex: Int,
)

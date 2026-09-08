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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import fr.geoking.arthur.genart.GenartQuality
import fr.geoking.arthur.genart.TonalPalette
import fr.geoking.arthur.genart.phase01
import fr.geoking.arthur.genart.qualityCount
import fr.geoking.arthur.genart.seededRange
import fr.geoking.arthur.genart.sin01
import kotlin.math.PI

private val skylineColor = Color(0xFF060912)
private val windowWarm = Color(0xFFFFC873)
private const val BUILDING_COUNT = 16

/** Dark navy skyline with independently twinkling warm window lights that never drift. */
@Composable
internal fun CityLightsEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val buildings = remember {
        val rawWidths = List(BUILDING_COUNT) { i -> seededRange(i * 31 + 5, 0.55f, 1.25f) }
        val total = rawWidths.sum()
        var cursor = 0f
        List(BUILDING_COUNT) { i ->
            val widthFrac = rawWidths[i] / total
            val xFrac = cursor
            cursor += widthFrac
            BuildingSeed(
                xFrac = xFrac,
                widthFrac = widthFrac,
                heightFrac = seededRange(i * 43 + 9, 0.14f, 0.52f),
                shade = seededRange(i * 97 + 29, 0.75f, 1.05f),
            )
        }
    }
    val lightCount = qualityCount(quality, low = 20, medium = 40, high = 64)
    val lights = remember(lightCount) {
        List(lightCount) { i ->
            val building = buildings[i % buildings.size]
            CityLightSeed(
                xFrac = building.xFrac + seededRange(i * 53 + 13, 0.15f, 0.85f) * building.widthFrac,
                yFrac = 1f - seededRange(i * 61 + 17, 0.12f, 0.9f) * building.heightFrac,
                twinkleFreq = seededRange(i * 71 + 19, 0.15f, 0.6f),
                twinklePhase = seededRange(i * 83 + 23, 0f, 2f * PI.toFloat()),
                colorIndex = i,
            )
        }
    }
    val transition = rememberInfiniteTransition(label = "citylights")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((42000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "citylights_t",
    )
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF060B1C), Color(0xFF01030A)),
            ),
        )
        val angle = phase01(t) * 2f * PI.toFloat()
        val dim = if (isActive) 1f else 0.55f

        buildings.forEach { building ->
            drawRect(
                color = TonalPalette.brightness(skylineColor, building.shade),
                topLeft = Offset(building.xFrac * w, h * (1f - building.heightFrac)),
                size = Size(building.widthFrac * w + 1f, h * building.heightFrac),
            )
        }

        lights.forEach { light ->
            val twinkle = 0.5f + 0.5f * sin01(angle * light.twinkleFreq + light.twinklePhase)
            val pulse = 0.6f + 0.4f * twinkle
            val x = light.xFrac * w
            val y = light.yFrac * h
            val base = TonalPalette.mix(windowWarm, TonalPalette.pick(paletteColors, light.colorIndex), 0.2f)
            val glow = TonalPalette.brightness(base, brightness)
            val glowR = 6.5f * pulse
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        TonalPalette.withAlpha(glow, 0.45f * pulse * dim),
                        Color.Transparent,
                    ),
                    center = Offset(x, y),
                    radius = glowR * 2.4f,
                ),
                radius = glowR * 2.4f,
                center = Offset(x, y),
            )
            drawCircle(
                color = TonalPalette.withAlpha(glow, (0.8f * pulse * dim).coerceIn(0f, 1f)),
                radius = 1.6f * pulse,
                center = Offset(x, y),
            )
        }
    }
}

private data class BuildingSeed(
    val xFrac: Float,
    val widthFrac: Float,
    val heightFrac: Float,
    val shade: Float,
)

private data class CityLightSeed(
    val xFrac: Float,
    val yFrac: Float,
    val twinkleFreq: Float,
    val twinklePhase: Float,
    val colorIndex: Int,
)

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
import androidx.compose.ui.graphics.StrokeCap
import fr.geoking.arthur.genart.GenartQuality
import fr.geoking.arthur.genart.TonalPalette
import fr.geoking.arthur.genart.lerp
import fr.geoking.arthur.genart.phase01
import fr.geoking.arthur.genart.qualityCount
import fr.geoking.arthur.genart.seededRange
import fr.geoking.arthur.genart.seededUnit
import fr.geoking.arthur.genart.sin01
import kotlin.math.PI

private val skylineColor = Color(0xFF0B1430)
private val windowWarm = Color(0xFFFFC873)
private val windowCold = Color(0xFF8FE8FF)
private const val BUILDING_COUNT = 7
private const val CITY_LOOP_SECONDS = 16f
private const val LIGHT_CYCLE_SECONDS = 8f
private const val TWO_PI = 6.2831855f

private fun smooth01(value: Float): Float = value * value * (3f - 2f * value)

private data class BuildingSeed(
    val xFrac: Float,
    val widthFrac: Float,
    val heightFrac: Float,
    val shade: Float,
)

private data class CityLightSeed(
    val xFrac: Float,
    val yFrac: Float,
    val radiusFrac: Float,
    val cold: Boolean,
    val phase: Float,
    val breathe: Float,
    val colorIndex: Int,
)

private data class TrailSeed(
    val yFrac: Float,
    val fromLeft: Boolean,
    val lengthFrac: Float,
)

/** Night city skyline: readable dark silhouettes, gentle window glow, cruising headlight trails. */
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
                heightFrac = seededRange(i * 43 + 9, 0.30f, 0.62f),
                shade = seededRange(i * 97 + 29, 0.78f, 1.02f),
            )
        }
    }
    val lightCount = qualityCount(quality, low = 12, medium = 40, high = 64)
    val lights = remember(lightCount) {
        List(lightCount) { i ->
            val building = buildings[i % buildings.size]
            CityLightSeed(
                xFrac = building.xFrac + seededRange(i * 53 + 13, 0.15f, 0.85f) * building.widthFrac,
                yFrac = 1f - seededRange(i * 61 + 17, 0.12f, 0.9f) * building.heightFrac,
                radiusFrac = 0.011f + seededRange(i * 71 + 19, 0f, 0.008f),
                cold = seededRange(i * 89 + 31, 0f, 1f) > 0.38f,
                phase = seededRange(i * 83 + 23, 0f, TWO_PI),
                breathe = seededRange(i * 97 + 37, 0.5f, 1.2f),
                colorIndex = i,
            )
        }
    }
    val transition = rememberInfiniteTransition(label = "citylights")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(((CITY_LOOP_SECONDS * 1000f) / speed).coerceIn(8000f, 24000f).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "citylights_t",
    )
    val dim = if (isActive) 1f else 0.55f
    val time01 = phase01(t)
    val lightTurn = CITY_LOOP_SECONDS / LIGHT_CYCLE_SECONDS

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val minDim = minOf(w, h)
        val lum = brightness * dim

        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    TonalPalette.brightness(Color(0xFF030510), lum),
                    TonalPalette.brightness(Color(0xFF0A1230), lum),
                    TonalPalette.brightness(Color(0xFF131F3F), lum),
                ),
            ),
        )

        // Urban horizon haze, slowly breathing amber/rose.
        val horizonY = h * 0.8f
        val halo = (0.30f + 0.16f * sin01(time01 * TWO_PI * 1.3f + 0.8f)) * lum
        val haloColor = TonalPalette.mix(Color(0xFFFFC98A), TonalPalette.pick(paletteColors, 0), 0.25f)
        drawCircle(
            brush = Brush.radialGradient(
                0f to TonalPalette.withAlpha(haloColor, halo),
                1f to Color.Transparent,
                center = Offset(w * 0.5f, horizonY),
                radius = h * 0.32f,
            ),
            radius = h * 0.32f,
            center = Offset(w * 0.5f, horizonY),
        )

        // Weak stars in the upper sky.
        val starCount = qualityCount(quality, low = 8, medium = 16, high = 24)
        for (i in 0 until starCount) {
            val x = seededUnit(i * 13 + 7) * w
            val y = seededUnit(i * 29 + 3) * h * 0.62f
            val r = minDim * (0.0009f + seededUnit(i * 37 + 11) * 0.0013f)
            val tw = sin01(time01 * TWO_PI * 1.6f + seededUnit(i * 41 + 5) * TWO_PI)
            val a = (0.16f + 0.5f * tw * tw) * lum
            drawCircle(
                color = TonalPalette.withAlpha(Color(0xFFE8F2FF), a),
                radius = r,
                center = Offset(x, y),
            )
        }

        // Dark blue-night skyline silhouettes with a faint upper-edge liseré.
        buildings.forEach { building ->
            val left = building.xFrac * w
            val top = h * (1f - building.heightFrac)
            val right = left + building.widthFrac * w
            drawRect(
                color = TonalPalette.brightness(skylineColor, building.shade * lum),
                topLeft = Offset(left, top),
                size = Size(building.widthFrac * w + 1f, h * building.heightFrac),
            )
            drawLine(
                color = TonalPalette.withAlpha(TonalPalette.brightness(Color(0xFF3A5178), building.shade), 0.55f * lum),
                start = Offset(left, top),
                end = Offset(right, top),
                strokeWidth = if (minDim * 0.0012f > 1f) minDim * 0.0012f else 1f,
                cap = StrokeCap.Round,
            )
        }

        // Independently fading window lights: warm or cold, softly crossfaded.
        lights.forEach { light ->
            val lit = smooth01(sin01(time01 * TWO_PI * lightTurn + light.phase))
            if (lit < 0.02f) return@forEach
            val intensity = (0.62f + 0.38f * sin01(time01 * TWO_PI * light.breathe + light.phase * 0.8f)) * lum
            val color = if (light.cold) {
                TonalPalette.mix(windowWarm, windowCold, 0.35f)
            } else {
                TonalPalette.mix(windowWarm, TonalPalette.pick(paletteColors, light.colorIndex), 0.2f)
            }
            val coreAlpha = (lit * intensity * 0.85f).coerceIn(0f, 1f)
            val glowAlpha = (lit * intensity * 1.15f).coerceIn(0f, 1f)
            val radius = light.radiusFrac * minDim * 2f
            val cx = light.xFrac * w
            val cy = light.yFrac * h
            drawCircle(
                brush = Brush.radialGradient(
                    0f to TonalPalette.withAlpha(color, glowAlpha),
                    1f to Color.Transparent,
                    center = Offset(cx, cy),
                    radius = radius * 2.2f,
                ),
                radius = radius * 2.2f,
                center = Offset(cx, cy),
            )
            drawCircle(
                color = TonalPalette.withAlpha(color, coreAlpha),
                radius = radius * 0.55f,
                center = Offset(cx, cy),
            )
        }

        // Two headlight trails cruising through the lower frame, looping seamlessly.
        val trails = listOf(
            TrailSeed(yFrac = 0.74f, fromLeft = true, lengthFrac = 0.05f),
            TrailSeed(yFrac = 0.84f, fromLeft = false, lengthFrac = 0.045f),
        )
        for (trail in trails) {
            val stretched = smooth01(time01)
            val len = trail.lengthFrac * minDim
            val headX = if (trail.fromLeft) lerp(-len, w + len, stretched) else lerp(w + len, -len, stretched)
            val segStartX = if (trail.fromLeft) headX - len else headX
            val segEndX = if (trail.fromLeft) headX else headX + len
            val y = trail.yFrac * h
            val trailAlpha = (0.38f * dim * brightness).coerceIn(0.05f, 0.8f)
            drawLine(
                color = TonalPalette.withAlpha(Color(0xFFFFE3B0), trailAlpha),
                start = Offset(segStartX, y),
                end = Offset(segEndX, y),
                strokeWidth = minDim * 0.0035f,
                cap = StrokeCap.Round,
            )
            drawCircle(
                color = TonalPalette.withAlpha(Color(0xFFFFF3DA), (0.9f * dim * brightness).coerceIn(0.1f, 1f)),
                radius = minDim * 0.006f,
                center = Offset(headX, y),
            )
        }
    }
}

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
import fr.geoking.arthur.genart.loopedFbm
import fr.geoking.arthur.genart.phase01
import fr.geoking.arthur.genart.qualityCount
import fr.geoking.arthur.genart.seededRange
import fr.geoking.arthur.genart.seededUnit
import kotlin.math.PI
import kotlin.math.sin

private data class CloudBank(
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val drift: Float,
    val seed: Int,
)

/** Quiet airplane-window view of layered clouds and a luminous high-altitude horizon. */
@Composable
internal fun AvionHorizonCloudsEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val count = qualityCount(quality, low = 6, medium = 10, high = 15)
    val clouds = remember(count) {
        List(count) { i ->
            CloudBank(
                x = seededUnit(i * 17 + 3),
                y = seededRange(i * 29 + 7, 0.52f, 0.91f),
                width = seededRange(i * 41 + 11, 0.18f, 0.42f),
                height = seededRange(i * 53 + 13, 0.045f, 0.13f),
                drift = seededRange(i * 67 + 19, 0.012f, 0.035f),
                seed = i * 97 + 5,
            )
        }
    }
    val transition = rememberInfiniteTransition(label = "avionhorizonclouds")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((68000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "avionhorizonclouds_t",
    )
    val dim = if (isActive) 1f else 0.55f

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val minDim = minOf(w, h)
        val time = phase01(t)
        val driftNoise = loopedFbm(time, radius = 1.25f, octaves = 3, seedOffset = 31)
        val horizon = h * (0.49f + 0.012f * (driftNoise - 0.5f))
        val skyTop = TonalPalette.brightness(Color(0xFF080A0C), brightness * dim)
        val skyBlue = TonalPalette.brightness(Color(0xFF7AB0E0), brightness * dim)
        val cloudWhite = TonalPalette.brightness(Color(0xFFD8E8F8), brightness * dim)
        val sunGlow = TonalPalette.brightness(Color(0xFFFFFFFF), brightness * dim)

        // Deep upper sky fades into a thin luminous band at the horizon.
        drawRect(
            brush = Brush.verticalGradient(
                0f to skyTop,
                0.34f to TonalPalette.mix(skyTop, skyBlue, 0.74f),
                0.49f to skyBlue,
                0.54f to cloudWhite,
                0.68f to skyBlue,
                1f to TonalPalette.mix(skyBlue, skyTop, 0.34f),
            ),
        )

        // Diffuse dawn/dusk bloom, broad enough to feel seen through a window.
        val glowCenter = Offset(w * (0.51f + (driftNoise - 0.5f) * 0.05f), horizon)
        drawCircle(
            brush = Brush.radialGradient(
                0f to TonalPalette.withAlpha(sunGlow, 0.42f * dim),
                0.32f to TonalPalette.withAlpha(cloudWhite, 0.22f * dim),
                1f to Color.Transparent,
                center = glowCenter,
                radius = (minDim * 0.48f).coerceAtLeast(1f),
            ),
            radius = (minDim * 0.48f).coerceAtLeast(1f),
            center = glowCenter,
        )
        drawRect(
            brush = Brush.verticalGradient(
                0f to Color.Transparent,
                0.5f to TonalPalette.withAlpha(sunGlow, 0.17f * dim),
                1f to Color.Transparent,
                startY = horizon - h * 0.09f,
                endY = horizon + h * 0.10f,
            ),
        )

        // A soft distant cloud shelf makes the horizon read as a high-altitude view.
        drawOval(
            brush = Brush.verticalGradient(
                0f to TonalPalette.withAlpha(cloudWhite, 0.04f * dim),
                0.4f to TonalPalette.withAlpha(cloudWhite, 0.36f * dim),
                1f to TonalPalette.withAlpha(skyBlue, 0.08f * dim),
            ),
            topLeft = Offset(-w * 0.08f, horizon - h * 0.025f),
            size = Size(w * 1.16f, h * 0.12f),
        )

        // Broad, softly feathered cloud banks drift slowly beneath the horizon.
        clouds.forEachIndexed { index, cloud ->
            val n = loopedFbm(time, radius = 1.1f + index * 0.03f, octaves = 2, seedOffset = cloud.seed)
            val travel = (time * cloud.drift + (n - 0.5f) * 0.018f + 1f) % 1f
            val x = ((cloud.x + travel) % 1.35f - 0.16f) * w
            val y = h * cloud.y + sin(time * 2f * PI.toFloat() + index) * h * 0.006f
            val cw = w * cloud.width * (0.9f + n * 0.18f)
            val ch = h * cloud.height * (0.88f + n * 0.18f)
            val cloudColor = if (index % 3 == 0) cloudWhite else skyBlue
            val alpha = (0.13f + n * 0.14f) * dim
            drawOval(
                brush = Brush.radialGradient(
                    0f to TonalPalette.withAlpha(cloudColor, alpha),
                    0.58f to TonalPalette.withAlpha(cloudWhite, alpha * 0.62f),
                    1f to Color.Transparent,
                    center = Offset(x + cw * 0.5f, y + ch * 0.5f),
                    radius = (cw * 0.62f).coerceAtLeast(1f),
                ),
                topLeft = Offset(x, y),
                size = Size(cw, ch),
            )
        }

        // Near cloud tops provide depth while keeping the lower edge quiet and soft.
        for (i in 0 until 4) {
            val phase = time * 2f * PI.toFloat() + i * 1.7f
            val cx = w * (0.12f + i * 0.27f) + sin(phase) * w * 0.025f
            val cy = h * (0.83f + (i % 2) * 0.035f)
            val radius = minDim * (0.30f + i * 0.025f)
            drawCircle(
                brush = Brush.radialGradient(
                    0f to TonalPalette.withAlpha(cloudWhite, 0.16f * dim),
                    0.48f to TonalPalette.withAlpha(skyBlue, 0.10f * dim),
                    1f to Color.Transparent,
                    center = Offset(cx, cy),
                    radius = radius,
                ),
                radius = radius,
                center = Offset(cx, cy),
            )
        }
    }
}

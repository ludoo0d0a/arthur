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
import fr.geoking.arthur.genart.loopedFbm
import fr.geoking.arthur.genart.phase01
import fr.geoking.arthur.genart.positiveRadius
import fr.geoking.arthur.genart.qualityCount
import fr.geoking.arthur.genart.seededRange
import fr.geoking.arthur.genart.seededUnit
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private data class WinterFlake(
    val x: Float,
    val y: Float,
    val radius: Float,
    val drift: Float,
    val speed: Float,
    val phase: Float,
    val seed: Int,
)

/** Soft Ambient Season Winter Wash — a quiet frost-blue wash with drifting snow. */
@Composable
internal fun SeasonWinterWashEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val count = qualityCount(quality, low = 4, medium = 7, high = 11)
    val flakeCount = qualityCount(quality, low = 12, medium = 22, high = 34)
    val flakes = remember(flakeCount) {
        List(flakeCount) { i ->
            WinterFlake(
                x = seededUnit(i * 31 + 3),
                y = seededUnit(i * 37 + 7),
                radius = seededRange(i * 41 + 11, 0.0025f, 0.009f),
                drift = seededRange(i * 53 + 13, 0.012f, 0.045f),
                speed = seededRange(i * 67 + 19, 0.6f, 1.5f),
                phase = seededRange(i * 71 + 23, 0f, 2f * PI.toFloat()),
                seed = i * 97 + 5,
            )
        }
    }
    val transition = rememberInfiniteTransition(label = "seasonwinterwash")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((72000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "seasonwinterwash_t",
    )
    val dim = if (isActive) 1f else 0.55f

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val minDim = minOf(w, h)
        val time = phase01(t)
        val wash = loopedFbm(time, radius = 1.35f, octaves = 3, seedOffset = 11)
        val warp = loopedFbm(time, radius = 1.2f, octaves = 2, seedOffset = 29)
        val deep = TonalPalette.brightness(Color(0xFF7A90A8), brightness * dim)
        val pale = TonalPalette.brightness(Color(0xFFDDE6F0), brightness * dim * (0.94f + 0.06f * wash))
        val snow = TonalPalette.brightness(Color(0xFFF5FBFF), brightness * dim)
        val accent = TonalPalette.brightness(
            TonalPalette.mix(snow, TonalPalette.pick(paletteColors, 0), 0.28f),
            brightness * dim,
        )

        drawRect(
            brush = Brush.verticalGradient(
                0f to pale,
                0.48f to TonalPalette.mix(pale, deep, 0.48f),
                1f to deep,
            ),
        )

        // Broad, feathered glows create the impression of snow haze in cold light.
        val glowX = w * (0.28f + 0.44f * warp)
        val glowY = h * (0.24f + 0.12f * loopedFbm(time, radius = 1.45f, seedOffset = 47))
        val glowRadius = (minDim * 0.68f).positiveRadius()
        drawCircle(
            brush = Brush.radialGradient(
                0f to TonalPalette.withAlpha(snow, 0.32f * dim),
                0.52f to TonalPalette.withAlpha(accent, 0.12f * dim),
                1f to Color.Transparent,
                center = Offset(glowX, glowY),
                radius = glowRadius,
            ),
            radius = glowRadius,
            center = Offset(glowX, glowY),
        )
        drawCircle(
            brush = Brush.radialGradient(
                0f to TonalPalette.withAlpha(Color(0xFFB8D4EC), 0.20f * dim),
                1f to Color.Transparent,
                center = Offset(w * 0.82f, h * 0.70f),
                radius = (minDim * 0.55f).positiveRadius(),
            ),
            radius = (minDim * 0.55f).positiveRadius(),
            center = Offset(w * 0.82f, h * 0.70f),
        )

        // Slow, seamless snow drift. Most flakes stay soft and small; a few catch light.
        flakes.forEach { flake ->
            val n = loopedFbm(time, radius = 1.1f, octaves = 2, seedOffset = flake.seed)
            val travel = (time * flake.speed + flake.y) % 1f
            val x = (flake.x + sin(time * 2f * PI.toFloat() + flake.phase) * flake.drift +
                (n - 0.5f) * 0.025f + 1f) % 1f * w
            val y = ((travel - 0.06f) % 1f + 1f) % 1f * h
            val radius = (minDim * flake.radius * (0.72f + 0.45f * n)).positiveRadius(0.6f)
            val alpha = (0.16f + 0.38f * n) * dim
            drawCircle(
                brush = Brush.radialGradient(
                    0f to TonalPalette.withAlpha(accent, alpha),
                    1f to Color.Transparent,
                    center = Offset(x, y),
                    radius = radius * 2.8f,
                ),
                radius = radius * 2.8f,
                center = Offset(x, y),
            )
            drawCircle(color = TonalPalette.withAlpha(snow, alpha), radius = radius, center = Offset(x, y))
        }

        // An occasional crystalline glint, kept faint so the scene remains a soft wash.
        repeat(count) { i ->
            val phase = time * 2f * PI.toFloat() + i * (2f * PI.toFloat() / count)
            val x = w * (0.12f + 0.76f * seededUnit(i * 23 + 101)) + minDim * 0.012f * sin(phase)
            val y = h * (0.16f + 0.72f * seededUnit(i * 29 + 103)) + minDim * 0.010f * cos(phase)
            val sparkle = (0.18f + 0.18f * (0.5f + 0.5f * sin(phase))) * dim
            val r = minDim * 0.010f
            drawLine(TonalPalette.withAlpha(snow, sparkle), Offset(x - r, y), Offset(x + r, y), minDim * 0.0015f)
            drawLine(TonalPalette.withAlpha(snow, sparkle), Offset(x, y - r), Offset(x, y + r), minDim * 0.0015f)
        }
    }
}

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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import fr.geoking.arthur.genart.GenartQuality
import fr.geoking.arthur.genart.TonalPalette
import fr.geoking.arthur.genart.loopedFbm
import fr.geoking.arthur.genart.phase01
import fr.geoking.arthur.genart.qualityCount
import fr.geoking.arthur.genart.seededRange
import fr.geoking.arthur.genart.seededUnit
import kotlin.math.cos
import kotlin.math.sin

private data class WindStreak(
    val angle: Float,
    val startPhase: Float,
    val length: Float,
    val width: Float,
    val bend: Float,
    val colorIndex: Int,
)

/** Soft Ambient POV Moto Wind Streaks — seamless wind rush without traffic simulation. */
@Composable
internal fun MotoWindStreaksEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val count = qualityCount(quality, low = 9, medium = 15, high = 23)
    val streaks = remember(count) {
        List(count) { i ->
            val side = if (i % 2 == 0) -1f else 1f
            WindStreak(
                angle = side * seededRange(i * 13 + 5, 0.22f, 1.15f),
                startPhase = seededUnit(i * 17 + 9),
                length = seededRange(i * 29 + 11, 0.10f, 0.34f),
                width = seededRange(i * 37 + 15, 0.8f, 2.4f),
                bend = side * seededRange(i * 43 + 19, 0.01f, 0.07f),
                colorIndex = i,
            )
        }
    }
    val transition = rememberInfiniteTransition(label = "motowindstreaks")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((68000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "motowindstreaks_t",
    )
    val dim = if (isActive) 1f else 0.55f

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val minDim = minOf(w, h)
        val time = phase01(t)
        val drift = loopedFbm(time, radius = 1.25f, octaves = 2, seedOffset = 11)
        val horizon = h * (0.52f + (drift - 0.5f) * 0.025f)
        val vanishing = Offset(w * 0.5f, horizon)
        val dark = TonalPalette.brightness(Color(0xFF0A0C10), brightness * dim)
        val upper = TonalPalette.brightness(Color(0xFF12161C), brightness * dim)
        val icy = TonalPalette.brightness(
            TonalPalette.mix(Color(0xFFC0D0E0), TonalPalette.pick(paletteColors, 0), 0.22f),
            brightness * dim,
        )
        drawRect(brush = Brush.verticalGradient(listOf(upper, dark, Color(0xFF080A0C))))

        // Distant, diffuse glow gives the road a forward point without adding a hard light source.
        drawCircle(
            brush = Brush.radialGradient(
                0f to TonalPalette.withAlpha(icy, 0.12f * dim),
                0.45f to TonalPalette.withAlpha(Color(0xFF566578), 0.045f * dim),
                1f to Color.Transparent,
                center = vanishing,
                radius = (minDim * 0.48f).coerceAtLeast(1f),
            ),
            radius = (minDim * 0.48f).coerceAtLeast(1f),
            center = vanishing,
        )

        // Roadside flow lines radiate from the vanishing point and stretch outward toward the rider.
        streaks.forEach { streak ->
            val travel = (streak.startPhase + time) % 1f
            val near = 0.08f + travel * 0.94f
            val far = (near - streak.length).coerceAtLeast(0f)
            val ux = sin(streak.angle)
            val uy = cos(streak.angle)
            val end = Offset(
                (vanishing.x + ux * minDim * near * 1.35f).coerceIn(-w * 0.2f, w * 1.2f),
                (vanishing.y + uy * minDim * near * 1.65f).coerceIn(-h * 0.15f, h * 1.15f),
            )
            val start = Offset(
                vanishing.x + ux * minDim * far * 1.35f,
                vanishing.y + uy * minDim * far * 1.65f,
            )
            val warp = loopedFbm(time, radius = 1.15f, octaves = 2, seedOffset = streak.colorIndex * 31 + 29) - 0.5f
            val control = Offset(
                (start.x + end.x) * 0.5f + streak.bend * minDim * (0.5f + warp),
                (start.y + end.y) * 0.5f,
            )
            val alpha = (0.025f + 0.11f * travel).coerceAtMost(0.14f) * dim
            val color = if (streak.colorIndex % 7 == 0) {
                TonalPalette.brightness(Color(0xFF9DB8D0), brightness * dim)
            } else {
                TonalPalette.pick(paletteColors, streak.colorIndex)
            }
            val path = Path().apply {
                moveTo(start.x, start.y)
                quadraticTo(control.x, control.y, end.x, end.y)
            }
            drawPath(
                path = path,
                brush = Brush.linearGradient(
                    colors = listOf(Color.Transparent, TonalPalette.withAlpha(color, alpha), Color.Transparent),
                    start = start,
                    end = end,
                ),
                style = Stroke(width = streak.width * minDim * 0.0018f, cap = StrokeCap.Round),
            )
        }

        // A soft lower silhouette anchors the first-person road view.
        drawRect(
            brush = Brush.verticalGradient(
                0f to Color.Transparent,
                1f to TonalPalette.withAlpha(Color(0xFF05070A), 0.72f * dim),
                startY = h * 0.79f,
                endY = h,
            ),
            topLeft = Offset(0f, h * 0.79f),
            size = androidx.compose.ui.geometry.Size(w, h * 0.21f),
        )
    }
}

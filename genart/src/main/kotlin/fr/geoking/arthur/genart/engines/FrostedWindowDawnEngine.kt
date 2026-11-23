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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import fr.geoking.arthur.genart.GenartQuality
import fr.geoking.arthur.genart.TonalPalette
import fr.geoking.arthur.genart.loopedFbm
import fr.geoking.arthur.genart.phase01
import fr.geoking.arthur.genart.positiveRadius
import fr.geoking.arthur.genart.qualityCount
import fr.geoking.arthur.genart.seededRange
import fr.geoking.arthur.genart.seededUnit
import kotlin.math.PI
import kotlin.math.sin

private data class FrostedWindowCrystal(val x: Float, val y: Float, val scale: Float, val seed: Int)

/** A softly glowing dawn seen through a window laced with frost. */
@Composable
internal fun FrostedWindowDawnEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val crystalCount = qualityCount(quality, low = 7, medium = 12, high = 18)
    val crystals = remember(crystalCount) {
        List(crystalCount) { i ->
            FrostedWindowCrystal(
                x = seededUnit(i * 31 + 3),
                y = seededUnit(i * 37 + 7),
                scale = seededRange(i * 41 + 11, 0.025f, 0.075f),
                seed = i * 97 + 5,
            )
        }
    }
    val transition = rememberInfiniteTransition(label = "frostedwindowdawn")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((72000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "frostedwindowdawn_t",
    )
    val dim = if (isActive) 1f else 0.55f

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val shortSide = minOf(w, h)
        val time = phase01(t)
        val dawn = loopedFbm(time, radius = 1.2f, octaves = 2, seedOffset = 29)
        val glowX = w * (0.28f + dawn * 0.44f)
        val glowY = h * (0.23f + loopedFbm(time, radius = 1.4f, seedOffset = 47) * 0.16f)
        val warm = TonalPalette.brightness(
            TonalPalette.mix(Color(0xFFFFE6C0), TonalPalette.pick(paletteColors, 0), 0.22f),
            brightness * dim,
        )
        val frost = TonalPalette.brightness(Color(0xFFE8F0F8), brightness * dim)
        val pane = TonalPalette.brightness(Color(0xFF9BB4C8), brightness * dim)

        drawRect(brush = Brush.verticalGradient(listOf(Color(0xFF667F9B), frost, pane)))

        // Pale, hazy sky and the soft first glow of sunrise behind the glass.
        drawCircle(
            brush = Brush.radialGradient(
                0f to TonalPalette.withAlpha(warm, 0.62f * dim),
                0.36f to TonalPalette.withAlpha(Color(0xFFFFDDB0), 0.30f * dim),
                1f to Color.Transparent,
                center = Offset(glowX, glowY),
                radius = (shortSide * 0.72f).positiveRadius(),
            ),
            radius = (shortSide * 0.72f).positiveRadius(),
            center = Offset(glowX, glowY),
        )
        drawRect(
            brush = Brush.verticalGradient(
                0f to Color.Transparent,
                0.46f to TonalPalette.withAlpha(Color(0xFFFFE3BE), 0.09f * dim),
                1f to TonalPalette.withAlpha(Color(0xFF6B8195), 0.30f * dim),
            ),
        )

        // Frost gathers around the perimeter, leaving the dawn visible in the center.
        drawRect(
            brush = Brush.horizontalGradient(
                0f to TonalPalette.withAlpha(Color(0xFFF4FAFF), 0.76f * dim),
                0.10f to TonalPalette.withAlpha(frost, 0.36f * dim),
                0.28f to Color.Transparent,
                0.72f to Color.Transparent,
                0.90f to TonalPalette.withAlpha(frost, 0.36f * dim),
                1f to TonalPalette.withAlpha(Color(0xFFF4FAFF), 0.76f * dim),
            ),
        )
        drawRect(
            brush = Brush.verticalGradient(
                0f to TonalPalette.withAlpha(Color(0xFFF4FAFF), 0.58f * dim),
                0.12f to TonalPalette.withAlpha(frost, 0.25f * dim),
                0.34f to Color.Transparent,
                0.68f to Color.Transparent,
                0.90f to TonalPalette.withAlpha(frost, 0.32f * dim),
                1f to TonalPalette.withAlpha(Color(0xFFF4FAFF), 0.72f * dim),
            ),
        )

        // Icy fern-like dendrites grow inward from scattered frost anchors.
        crystals.forEach { crystal ->
            val x = crystal.x * w
            val y = crystal.y * h
            val size = shortSide * crystal.scale
            val shimmer = 0.75f + 0.25f * loopedFbm(time, radius = 1.1f, octaves = 2, seedOffset = crystal.seed)
            val alpha = 0.30f * shimmer * dim
            val direction = if (crystal.seed % 2 == 0) 1f else -1f
            val path = Path().apply {
                moveTo(x, y + size)
                lineTo(x, y - size)
                moveTo(x, y - size * 0.15f)
                lineTo(x + direction * size * 0.50f, y - size * 0.58f)
                moveTo(x, y - size * 0.15f)
                lineTo(x - direction * size * 0.50f, y - size * 0.58f)
                moveTo(x, y + size * 0.28f)
                lineTo(x + direction * size * 0.34f, y - size * 0.02f)
                moveTo(x, y + size * 0.28f)
                lineTo(x - direction * size * 0.34f, y - size * 0.02f)
            }
            drawPath(
                path = path,
                color = TonalPalette.withAlpha(Color(0xFFFFFFFF), alpha),
                style = Stroke(width = (shortSide * 0.0022f).coerceAtLeast(1f)),
            )
            drawCircle(
                color = TonalPalette.withAlpha(Color(0xFFFFFFFF), alpha * 0.65f),
                radius = (shortSide * 0.004f).positiveRadius(),
                center = Offset(x, y),
            )
        }

        // Window muntins: slim, cool shadows with a warm reflected edge.
        val frame = shortSide * 0.035f
        val vertical = w * 0.5f
        val horizontal = h * 0.52f
        drawRect(
            color = TonalPalette.withAlpha(Color(0xFF63798D), 0.65f * dim),
            topLeft = Offset(vertical - frame * 0.5f, 0f),
            size = Size(frame, h),
        )
        drawRect(
            color = TonalPalette.withAlpha(Color(0xFF63798D), 0.60f * dim),
            topLeft = Offset(0f, horizontal - frame * 0.5f),
            size = Size(w, frame),
        )
        drawLine(
            color = TonalPalette.withAlpha(warm, 0.28f * dim),
            start = Offset(vertical + frame * 0.5f, 0f),
            end = Offset(vertical + frame * 0.5f, h),
            strokeWidth = shortSide * 0.004f,
        )
        drawLine(
            color = TonalPalette.withAlpha(warm, 0.24f * dim),
            start = Offset(0f, horizontal + frame * 0.5f),
            end = Offset(w, horizontal + frame * 0.5f),
            strokeWidth = shortSide * 0.004f,
        )

        // Fine condensation beads catch a faint, slow-moving dawn shimmer.
        repeat(qualityCount(quality, low = 8, medium = 14, high = 22)) { i ->
            val x0 = seededUnit(i * 53 + 19)
            val y0 = seededUnit(i * 67 + 23)
            val drift = loopedFbm(time, radius = 1.3f, octaves = 2, seedOffset = i * 79 + 31) - 0.5f
            val x = (x0 + drift * 0.025f) * w
            val y = (y0 + 0.012f * sin(time * 2f * PI.toFloat() + i)) * h
            val sparkle = 0.5f + 0.5f * loopedFbm(time, radius = 1.4f, seedOffset = i * 89 + 37)
            drawCircle(
                color = TonalPalette.withAlpha(Color.White, (0.10f + 0.18f * sparkle) * dim),
                radius = (shortSide * 0.0025f).positiveRadius(),
                center = Offset(x, y),
            )
        }
    }
}

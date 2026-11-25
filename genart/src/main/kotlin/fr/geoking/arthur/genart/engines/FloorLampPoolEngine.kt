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
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import fr.geoking.arthur.genart.GenartQuality
import fr.geoking.arthur.genart.TonalPalette
import fr.geoking.arthur.genart.loopedFbm
import fr.geoking.arthur.genart.phase01
import fr.geoking.arthur.genart.qualityCount
import fr.geoking.arthur.genart.seededRange
import kotlin.math.PI
import kotlin.math.sin

/** Loop duration at default speed (ms) — gentle breathing within the 8–14 s brief. */
private const val FLOOR_LAMP_POOL_LOOP_MS = 11000f

/** Soft Ambient Floor Lamp Pool — a gently breathing pool of lamplight. */
@Composable
internal fun FloorLampPoolEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val count = qualityCount(quality, low = 4, medium = 7, high = 11)
    val motes = (0 until count).map { i ->
        Triple(
            seededRange(i * 17 + 3, 0.18f, 0.82f),
            seededRange(i * 29 + 7, 0.72f, 0.92f),
            seededRange(i * 41 + 11, 0.0018f, 0.0045f),
        )
    }
    val transition = rememberInfiniteTransition(label = "floorlamppool")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((FLOOR_LAMP_POOL_LOOP_MS / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "floorlamppool_t",
    )
    val dim = if (isActive) 1f else 0.55f
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val m = minOf(w, h)
        val time = phase01(t)
        val breath = loopedFbm(time, radius = 1.25f, octaves = 2, seedOffset = 17)
        val flicker = 0.88f + 0.12f * breath
        val accent = TonalPalette.brightness(
            TonalPalette.mix(Color(0xFFFFD8A0), TonalPalette.pick(paletteColors, 0), 0.18f),
            brightness * dim,
        )
        val dark = TonalPalette.brightness(Color(0xFF0E1014), brightness * dim)
        val wall = TonalPalette.brightness(Color(0xFF1C2028), brightness * dim)

        drawRect(brush = Brush.verticalGradient(0f to dark, 0.68f to wall, 1f to Color(0xFF101116)))

        // Floor line and quiet room geometry.
        val floorY = h * 0.78f
        drawRect(
            brush = Brush.verticalGradient(
                0f to Color(0xFF15161A),
                1f to Color(0xFF090A0D),
                startY = floorY,
                endY = h,
            ),
            topLeft = Offset(0f, floorY),
            size = Size(w, h - floorY),
        )
        drawLine(Color(0xFF38312A).copy(alpha = 0.42f * dim), Offset(0f, floorY), Offset(w, floorY), m * 0.002f)

        // Broad, feathered light spill across the floor.
        val poolCenter = Offset(w * 0.58f, h * 0.805f)
        drawOval(
            brush = Brush.radialGradient(
                0f to accent.copy(alpha = 0.42f * flicker),
                0.38f to accent.copy(alpha = 0.26f * flicker),
                0.72f to accent.copy(alpha = 0.10f * flicker),
                1f to Color.Transparent,
                center = poolCenter,
                radius = m * 0.66f,
            ),
            topLeft = Offset(w * 0.10f, h * 0.60f),
            size = Size(w * 0.94f, h * 0.50f),
        )
        // A soft cone carries the light from the shade down to the pool.
        val shadeX = w * 0.66f
        val cone = Path().apply {
            moveTo(shadeX - m * 0.11f, h * 0.34f)
            lineTo(w * 0.22f, floorY)
            lineTo(w * 0.98f, floorY)
            lineTo(shadeX + m * 0.11f, h * 0.34f)
            close()
        }
        drawPath(
            path = cone,
            brush = Brush.verticalGradient(
                0f to accent.copy(alpha = 0.16f * flicker),
                1f to accent.copy(alpha = 0.025f * flicker),
                startY = h * 0.34f,
                endY = floorY,
            ),
        )

        // Standing lamp and warm fabric shade — scaled up for clear silhouette.
        val poleX = w * 0.66f
        drawLine(Color(0xFF29241F), Offset(poleX, h * 0.35f), Offset(poleX, floorY), m * 0.022f, cap = StrokeCap.Round)
        drawLine(Color(0xFF68523B).copy(alpha = 0.55f), Offset(poleX - m * 0.005f, h * 0.36f), Offset(poleX - m * 0.005f, floorY * 0.98f), m * 0.0025f)
        drawOval(Color(0xFF211D19), Offset(poleX - m * 0.13f, floorY - m * 0.018f), Size(m * 0.26f, m * 0.042f))
        val shade = Path().apply {
            moveTo(poleX - m * 0.12f, h * 0.21f)
            quadraticTo(poleX, h * 0.19f, poleX + m * 0.12f, h * 0.21f)
            lineTo(poleX + m * 0.19f, h * 0.35f)
            quadraticTo(poleX, h * 0.37f, poleX - m * 0.19f, h * 0.35f)
            close()
        }
        drawPath(
            shade,
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF6B5640), Color(0xFF2E261F)),
                startY = h * 0.19f,
                endY = h * 0.37f,
            ),
        )
        drawLine(Color(0xFFB2956A).copy(alpha = 0.45f), Offset(poleX - m * 0.19f, h * 0.35f), Offset(poleX + m * 0.19f, h * 0.35f), m * 0.007f)
        drawOval(
            brush = Brush.radialGradient(
                0f to accent.copy(alpha = 0.82f * flicker),
                0.55f to accent.copy(alpha = 0.28f * flicker),
                1f to Color.Transparent,
                center = Offset(poleX, h * 0.36f),
                radius = m * 0.30f,
            ),
            topLeft = Offset(poleX - m * 0.30f, h * 0.28f),
            size = Size(m * 0.60f, m * 0.22f),
        )

        // Sparse dust motes glimmer only where they pass through the light.
        motes.forEachIndexed { i, (x, y, r) ->
            val drift = sin(time * 2f * PI.toFloat() + i * 1.7f)
            val px = w * (x + drift * 0.018f)
            val py = h * (y - (time * 0.14f + i * 0.13f) % 0.14f)
            val influence = (1f - kotlin.math.abs(px - poleX) / (m * 0.50f)).coerceIn(0f, 1f)
            val alpha = (0.12f + 0.30f * influence) * dim
            drawCircle(accent.copy(alpha = alpha), m * r * 1.35f, Offset(px, py))
        }
    }
}

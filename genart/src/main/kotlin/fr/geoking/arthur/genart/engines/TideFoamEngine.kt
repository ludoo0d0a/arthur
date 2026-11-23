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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import fr.geoking.arthur.genart.GenartQuality
import fr.geoking.arthur.genart.TonalPalette
import fr.geoking.arthur.genart.loopedFbm
import fr.geoking.arthur.genart.phase01
import fr.geoking.arthur.genart.qualityCount
import kotlin.math.PI
import kotlin.math.sin

/** A quiet, moonlit tide drawing lace-like foam along a dark shore. */
@Composable
internal fun TideFoamEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val detail = qualityCount(quality, low = 4, medium = 7, high = 11)
    val transition = rememberInfiniteTransition(label = "tidefoam")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((72000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "tidefoam_t",
    )
    val dim = if (isActive) 1f else 0.55f

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val minDim = minOf(w, h)
        val time = phase01(t)
        val sea = TonalPalette.brightness(Color(0xFF1A3040), brightness * dim)
        val deepSea = TonalPalette.brightness(Color(0xFF0E1C28), brightness * dim)
        val foam = TonalPalette.brightness(
            TonalPalette.mix(Color(0xFFE8F0F8), TonalPalette.pick(paletteColors, 0), 0.18f),
            brightness * dim,
        )

        // Deep dusk sky and shadowed rocks frame the water.
        drawRect(
            brush = Brush.verticalGradient(
                0f to Color(0xFF101820),
                0.48f to Color(0xFF192833),
                1f to deepSea,
            ),
        )
        val horizonGlow = Offset(w * 0.55f, h * 0.57f)
        drawCircle(
            brush = Brush.radialGradient(
                0f to TonalPalette.withAlpha(foam, 0.10f * dim),
                0.45f to TonalPalette.withAlpha(sea, 0.15f * dim),
                1f to Color.Transparent,
                center = horizonGlow,
                radius = (minDim * 0.72f).coerceAtLeast(1f),
            ),
            radius = (minDim * 0.72f).coerceAtLeast(1f),
            center = horizonGlow,
        )

        fun shoreY(x: Float, offset: Float = 0f): Float {
            val x01 = x / w
            val swell = sin(x01 * 2f * PI.toFloat() + time * 2f * PI.toFloat())
            val ripple = sin(x01 * 5f * PI.toFloat() - time * 2f * PI.toFloat())
            val warp = loopedFbm(time + x01 * 0.12f, radius = 1.15f, octaves = 2, seedOffset = 61)
            return h * (0.61f + 0.052f * swell + 0.014f * ripple + 0.022f * (warp - 0.5f)) + offset
        }

        fun wavePath(offset: Float, closeDown: Boolean): Path = Path().apply {
            val segments = (detail * 5).coerceAtLeast(24)
            for (i in 0..segments) {
                val x = w * i / segments
                val y = shoreY(x, offset)
                if (i == 0) moveTo(x, y) else lineTo(x, y)
            }
            if (closeDown) {
                lineTo(w, h)
                lineTo(0f, h)
                close()
            }
        }

        val shorePath = Path().apply {
            val segments = (detail * 5).coerceAtLeast(24)
            moveTo(0f, 0f)
            lineTo(w, 0f)
            for (i in segments downTo 0) {
                val x = w * i / segments
                lineTo(x, shoreY(x, -minDim * 0.01f))
            }
            close()
        }
        drawPath(
            path = shorePath,
            brush = Brush.verticalGradient(
                0f to Color(0xFF101820),
                0.75f to Color(0xFF1A2934),
                1f to Color(0xFF263844),
                startY = 0f,
                endY = h * 0.68f,
            ),
        )
        // Scattered dark pebbles keep the land edge readable without a hard silhouette.
        for (i in 0 until detail + 2) {
            val x = w * (0.08f + i * 0.84f / (detail + 1))
            val y = shoreY(x, -minDim * (0.035f + (i % 3) * 0.014f))
            val radius = minDim * (0.006f + (i % 3) * 0.002f)
            drawCircle(
                color = TonalPalette.withAlpha(Color(0xFF080F15), 0.55f * dim),
                radius = radius,
                center = Offset(x, y),
            )
        }

        // Moving tidal sheet.
        drawPath(
            path = wavePath(0f, closeDown = true),
            brush = Brush.verticalGradient(
                0f to TonalPalette.brightness(Color(0xFF24475A), brightness * dim),
                0.45f to sea,
                1f to deepSea,
                startY = h * 0.58f,
                endY = h,
            ),
        )

        // Several broken, soft lines echo the foam as it thins over wet sand.
        for (band in 1..3) {
            val offset = minDim * (0.016f + band * 0.025f)
            val path = wavePath(offset, closeDown = false)
            drawPath(
                path = path,
                color = TonalPalette.withAlpha(foam, (0.16f / band) * dim),
                style = Stroke(width = minDim * (0.012f / band)),
            )
            drawPath(
                path = path,
                color = TonalPalette.withAlpha(foam, (0.22f / band) * dim),
                style = Stroke(width = minDim * (0.0025f / band)),
            )
        }

        // The leading lace edge: short scallops sit just above the waterline.
        val lace = Path().apply {
            val count = (detail * 8).coerceAtLeast(32)
            for (i in 0 until count) {
                val x = w * i / (count - 1)
                val y = shoreY(x, -minDim * 0.006f)
                val scallop = sin(i * PI.toFloat() * 0.92f) * minDim * 0.005f
                if (i == 0) moveTo(x, y + scallop) else lineTo(x, y + scallop)
            }
        }
        drawPath(lace, TonalPalette.withAlpha(foam, 0.52f * dim), style = Stroke(minDim * 0.009f))
        drawPath(lace, TonalPalette.withAlpha(Color.White, 0.42f * dim), style = Stroke(minDim * 0.002f))

        // Long shallow ripples catch a little moonlight below the breaking wave.
        for (i in 0 until detail) {
            val yBase = h * (0.72f + i * 0.045f)
            val ripple = Path().apply {
                val segments = 24
                for (step in 0..segments) {
                    val x = w * step / segments
                    val x01 = x / w
                    val y = yBase + minDim * 0.008f * sin(x01 * 4f * PI.toFloat() + time * 2f * PI.toFloat() + i)
                    if (step == 0) moveTo(x, y) else lineTo(x, y)
                }
            }
            drawPath(
                path = ripple,
                color = TonalPalette.withAlpha(foam, (0.10f + 0.02f * (i % 3)) * dim),
                style = Stroke(width = minDim * 0.0018f),
            )
        }
    }
}

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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import fr.geoking.arthur.genart.GenartQuality
import fr.geoking.arthur.genart.TonalPalette
import fr.geoking.arthur.genart.loopedFbm
import fr.geoking.arthur.genart.phase01
import fr.geoking.arthur.genart.qualityCount
import kotlin.math.PI
import kotlin.math.sin

/** Soft Ambient POV US Drive-In Glow — a quiet movie night beneath the screen. */
@Composable
internal fun UsDriveInEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val flickerCount = qualityCount(quality, low = 3, medium = 5, high = 8)
    val transition = rememberInfiniteTransition(label = "usdrivein")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((68000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "usdrivein_t",
    )
    val dim = if (isActive) 1f else 0.55f

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val minDim = minOf(w, h)
        val time = phase01(t)
        val haze = loopedFbm(time, radius = 1.3f, octaves = 3, seedOffset = 17)
        val flicker = 0.96f + 0.04f * loopedFbm(time, radius = 2.1f, octaves = 2, seedOffset = 43)
        val skyTop = TonalPalette.brightness(Color(0xFF0E0A14), brightness * dim)
        val skyBottom = TonalPalette.brightness(Color(0xFF1A1424), brightness * dim)
        val amber = TonalPalette.brightness(
            TonalPalette.mix(Color(0xFFFFB040), TonalPalette.pick(paletteColors, 0), 0.24f),
            brightness * dim,
        )

        drawRect(brush = Brush.verticalGradient(listOf(skyTop, skyBottom, Color(0xFF100D17))))

        // Distant low hills and a narrow band of warm horizon haze.
        val horizonY = h * 0.61f
        val hills = Path().apply {
            moveTo(0f, horizonY + h * 0.015f)
            cubicTo(w * 0.19f, horizonY - h * 0.07f, w * 0.32f, horizonY + h * 0.03f, w * 0.48f, horizonY - h * 0.025f)
            cubicTo(w * 0.65f, horizonY - h * 0.09f, w * 0.82f, horizonY + h * 0.02f, w, horizonY - h * 0.04f)
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }
        drawPath(hills, Color(0xFF090A10))
        drawRect(
            brush = Brush.verticalGradient(
                0f to Color.Transparent,
                0.55f to TonalPalette.withAlpha(amber, 0.10f * dim * (0.8f + 0.2f * haze)),
                1f to Color.Transparent,
                startY = horizonY - h * 0.08f,
                endY = horizonY + h * 0.09f,
            ),
            topLeft = Offset(0f, horizonY - h * 0.08f),
            size = Size(w, h * 0.17f),
        )

        // Broad screen glow behind its dark metal frame.
        val screenCenter = Offset(w * 0.5f, h * 0.345f)
        val screenW = w * 0.72f
        val screenH = h * 0.31f
        drawCircle(
            brush = Brush.radialGradient(
                0f to TonalPalette.withAlpha(amber, 0.23f * flicker * dim),
                0.48f to TonalPalette.withAlpha(Color(0xFF9D5D4B), 0.09f * dim),
                1f to Color.Transparent,
                center = screenCenter,
                radius = minDim * 0.62f,
            ),
            radius = minDim * 0.62f,
            center = screenCenter,
        )

        val frameTop = screenCenter.y - screenH * 0.5f
        drawRoundRect(
            color = Color(0xFF08090E),
            topLeft = Offset(screenCenter.x - screenW * 0.5f - minDim * 0.012f, frameTop - minDim * 0.012f),
            size = Size(screenW + minDim * 0.024f, screenH + minDim * 0.024f),
            cornerRadius = CornerRadius(minDim * 0.006f),
        )
        drawRect(
            brush = Brush.verticalGradient(
                0f to Color(0xFF4C3835),
                0.33f to Color(0xFF9C6654),
                0.72f to Color(0xFF332E40),
                1f to Color(0xFF1E2535),
            ),
            topLeft = Offset(screenCenter.x - screenW * 0.5f, frameTop),
            size = Size(screenW, screenH),
        )

        // A soft, abstract projected image with drifting cloud-like bands.
        val imageGlowY = frameTop + screenH * (0.54f + 0.025f * (haze - 0.5f))
        drawOval(
            brush = Brush.radialGradient(
                0f to TonalPalette.withAlpha(Color(0xFFFFC681), 0.39f * flicker * dim),
                0.47f to TonalPalette.withAlpha(Color(0xFFE29A7A), 0.16f * dim),
                1f to Color.Transparent,
                center = Offset(screenCenter.x - screenW * 0.13f, imageGlowY),
                radius = screenW * 0.42f,
            ),
            topLeft = Offset(screenCenter.x - screenW * 0.48f, frameTop + screenH * 0.08f),
            size = Size(screenW * 0.96f, screenH * 0.85f),
        )
        for (i in 0 until flickerCount) {
            val n = loopedFbm(time, radius = 1.1f + i * 0.19f, octaves = 2, seedOffset = 81 + i * 13)
            val bandY = frameTop + screenH * (0.18f + i * 0.105f)
            val bandAlpha = (0.025f + 0.025f * n) * flicker * dim
            drawLine(
                color = TonalPalette.withAlpha(Color(0xFFFFE3BE), bandAlpha),
                start = Offset(screenCenter.x - screenW * 0.49f, bandY),
                end = Offset(screenCenter.x + screenW * 0.49f, bandY + screenH * 0.012f * sin(time * 2f * PI.toFloat() + i)),
                strokeWidth = minDim * 0.003f,
            )
        }
        // A low dark ridge on the projected picture suggests a film landscape.
        val movieRidge = Path().apply {
            moveTo(screenCenter.x - screenW * 0.49f, frameTop + screenH * 0.80f)
            cubicTo(screenCenter.x - screenW * 0.25f, frameTop + screenH * 0.69f, screenCenter.x - screenW * 0.16f, frameTop + screenH * 0.84f, screenCenter.x + screenW * 0.02f, frameTop + screenH * 0.76f)
            cubicTo(screenCenter.x + screenW * 0.2f, frameTop + screenH * 0.68f, screenCenter.x + screenW * 0.34f, frameTop + screenH * 0.82f, screenCenter.x + screenW * 0.49f, frameTop + screenH * 0.73f)
            lineTo(screenCenter.x + screenW * 0.49f, frameTop + screenH)
            lineTo(screenCenter.x - screenW * 0.49f, frameTop + screenH)
            close()
        }
        drawPath(movieRidge, Color(0xFF28263A))

        // Screen support and a low row of parked-car silhouettes.
        drawLine(Color(0xFF08090D), Offset(w * 0.5f, frameTop + screenH + minDim * 0.01f), Offset(w * 0.5f, horizonY + h * 0.06f), minDim * 0.014f)
        drawLine(Color(0xFF14131A), Offset(w * 0.18f, horizonY + h * 0.055f), Offset(w * 0.82f, horizonY + h * 0.055f), minDim * 0.007f)
        val carY = h * 0.82f
        val carXs = listOf(0.19f, 0.39f, 0.63f, 0.82f)
        carXs.forEachIndexed { index, fraction ->
            val carW = w * (0.15f + if (index % 2 == 0) 0.012f else 0f)
            val carH = h * (0.042f + if (index % 2 == 0) 0.006f else 0f)
            val x = w * fraction
            val car = Path().apply {
                moveTo(x - carW * 0.5f, carY)
                lineTo(x - carW * 0.43f, carY - carH * 0.42f)
                quadraticTo(x - carW * 0.28f, carY - carH * 0.53f, x - carW * 0.14f, carY - carH * 0.98f)
                lineTo(x + carW * 0.16f, carY - carH * 0.98f)
                quadraticTo(x + carW * 0.33f, carY - carH * 0.44f, x + carW * 0.45f, carY - carH * 0.35f)
                lineTo(x + carW * 0.5f, carY)
                close()
            }
            drawPath(car, Color(0xFF07080C))
            drawCircle(Color(0xFF050609), carH * 0.21f, Offset(x - carW * 0.29f, carY))
            drawCircle(Color(0xFF050609), carH * 0.21f, Offset(x + carW * 0.29f, carY))
        }
        drawRect(
            brush = Brush.verticalGradient(0f to Color.Transparent, 1f to Color(0xFF06070B)),
            topLeft = Offset(0f, h * 0.84f),
            size = Size(w, h * 0.16f),
        )
    }
}

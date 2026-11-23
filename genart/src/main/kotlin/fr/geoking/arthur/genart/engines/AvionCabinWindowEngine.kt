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
import androidx.compose.ui.graphics.drawscope.Stroke
import fr.geoking.arthur.genart.GenartQuality
import fr.geoking.arthur.genart.TonalPalette
import fr.geoking.arthur.genart.loopedFbm
import fr.geoking.arthur.genart.phase01
import fr.geoking.arthur.genart.qualityCount

/** Soft Ambient POV Avion Cabin Window — clouds drift gently beyond a dim cabin frame. */
@Composable
internal fun AvionCabinWindowEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val cloudCount = qualityCount(quality, low = 4, medium = 7, high = 11)
    val transition = rememberInfiniteTransition(label = "avioncabinwindow")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((68000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "avioncabinwindow_t",
    )
    val dim = if (isActive) 1f else 0.55f

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val minDim = minOf(w, h)
        val time = phase01(t)
        val cloudWarp = loopedFbm(time, radius = 1.4f, octaves = 3, seedOffset = 215)
        val drift = time * minDim * 0.10f
        val outer = TonalPalette.brightness(Color(0xFF080A0C), brightness * dim)
        val cabin = TonalPalette.brightness(Color(0xFF0A0C10), brightness * dim)
        val skyTop = TonalPalette.brightness(Color(0xFF0E1420), brightness * dim)
        val skyBottom = TonalPalette.brightness(Color(0xFF1A2438), brightness * dim)
        val blueGlow = TonalPalette.brightness(Color(0xFF80B0E0), brightness * dim)

        drawRect(brush = Brush.verticalGradient(listOf(Color(0xFF05070A), outer, Color(0xFF111820))))

        val windowW = w * 0.76f
        val windowH = h * 0.70f
        val left = (w - windowW) * 0.5f
        val top = h * 0.11f
        val corner = minDim * 0.105f

        // Soft cabin spill behind the window gasket.
        drawRoundRect(
            brush = Brush.radialGradient(
                0f to TonalPalette.withAlpha(blueGlow, 0.11f * dim),
                1f to Color.Transparent,
                center = Offset(w * 0.5f, top + windowH * 0.44f),
                radius = minDim * 0.62f,
            ),
            topLeft = Offset(left - minDim * 0.08f, top - minDim * 0.05f),
            size = Size(windowW + minDim * 0.16f, windowH + minDim * 0.10f),
            cornerRadius = CornerRadius(corner + minDim * 0.05f),
        )

        // Window glass and the deep sky beyond it.
        drawRoundRect(
            brush = Brush.verticalGradient(
                0f to skyTop,
                0.55f to TonalPalette.mix(skyTop, skyBottom, 0.58f),
                1f to skyBottom,
                startY = top,
                endY = top + windowH,
            ),
            topLeft = Offset(left, top),
            size = Size(windowW, windowH),
            cornerRadius = CornerRadius(corner),
        )

        val cloudY = top + windowH * (0.63f + 0.025f * (cloudWarp - 0.5f))
        drawRect(
            brush = Brush.verticalGradient(
                0f to Color.Transparent,
                0.44f to TonalPalette.withAlpha(blueGlow, 0.12f * dim),
                1f to Color.Transparent,
                startY = cloudY - windowH * 0.22f,
                endY = cloudY + windowH * 0.18f,
            ),
            topLeft = Offset(left + minDim * 0.02f, cloudY - windowH * 0.22f),
            size = Size(windowW - minDim * 0.04f, windowH * 0.40f),
        )

        // Layered cloud wisps slide slowly across the glass, with soft edges.
        repeat(cloudCount) { i ->
            val lane = i / cloudCount.toFloat()
            val y = top + windowH * (0.43f + lane * 0.42f)
            val n = loopedFbm(time, radius = 1.1f + lane, octaves = 2, seedOffset = 240 + i * 17)
            val x = left - windowW * 0.32f + ((drift * (0.65f + lane * 0.45f) + i * windowW * 0.34f) % (windowW * 1.64f))
            val bandW = windowW * (0.42f + 0.25f * n)
            val bandH = windowH * (0.065f + 0.045f * n)
            val cloudColor = TonalPalette.mix(Color(0xFF526783), Color(0xFFB5C9DF), 0.35f + 0.35f * n)
            val cloudPath = Path().apply {
                moveTo(x, y + bandH * 0.25f)
                cubicTo(x + bandW * 0.22f, y - bandH * 0.22f, x + bandW * 0.30f, y + bandH * 0.12f, x + bandW * 0.52f, y)
                cubicTo(x + bandW * 0.72f, y - bandH * 0.20f, x + bandW * 0.80f, y + bandH * 0.36f, x + bandW, y + bandH * 0.08f)
            }
            drawPath(
                path = cloudPath,
                color = TonalPalette.withAlpha(cloudColor, (0.10f + 0.10f * n) * dim),
                style = Stroke(width = bandH, cap = androidx.compose.ui.graphics.StrokeCap.Round),
            )
        }

        // A very soft dawnless high-altitude glow sits at the cloud line.
        drawCircle(
            brush = Brush.radialGradient(
                0f to TonalPalette.withAlpha(blueGlow, 0.16f * dim),
                1f to Color.Transparent,
                center = Offset(left + windowW * 0.54f, cloudY),
                radius = windowW * 0.56f,
            ),
            radius = windowW * 0.56f,
            center = Offset(left + windowW * 0.54f, cloudY),
        )

        // Window gasket, bevel and cool reflected rim.
        drawRoundRect(
            color = TonalPalette.withAlpha(outer, 0.96f * dim),
            topLeft = Offset(left - minDim * 0.023f, top - minDim * 0.023f),
            size = Size(windowW + minDim * 0.046f, windowH + minDim * 0.046f),
            cornerRadius = CornerRadius(corner + minDim * 0.022f),
            style = Stroke(width = minDim * 0.027f),
        )
        drawRoundRect(
            color = TonalPalette.withAlpha(Color(0xFF53647A), 0.25f * dim),
            topLeft = Offset(left + minDim * 0.008f, top + minDim * 0.008f),
            size = Size(windowW - minDim * 0.016f, windowH - minDim * 0.016f),
            cornerRadius = CornerRadius(corner - minDim * 0.008f),
            style = Stroke(width = minDim * 0.006f),
        )

        // Dim cabin wall and lower sill frame the view without blocking the clouds.
        drawRect(
            brush = Brush.verticalGradient(
                0f to Color.Transparent,
                0.20f to TonalPalette.withAlpha(cabin, 0.65f * dim),
                1f to cabin,
                startY = top + windowH - minDim * 0.015f,
                endY = h,
            ),
            topLeft = Offset(0f, top + windowH - minDim * 0.015f),
            size = Size(w, h - (top + windowH - minDim * 0.015f)),
        )
        drawRoundRect(
            color = TonalPalette.withAlpha(Color(0xFF151A22), 0.75f * dim),
            topLeft = Offset(left - minDim * 0.045f, top + windowH - minDim * 0.008f),
            size = Size(windowW + minDim * 0.09f, minDim * 0.042f),
            cornerRadius = CornerRadius(minDim * 0.012f),
        )
        drawLine(
            color = TonalPalette.withAlpha(blueGlow, 0.12f * dim),
            start = Offset(left + windowW * 0.08f, top + windowH + minDim * 0.004f),
            end = Offset(left + windowW * 0.92f, top + windowH + minDim * 0.004f),
            strokeWidth = minDim * 0.003f,
        )
    }
}

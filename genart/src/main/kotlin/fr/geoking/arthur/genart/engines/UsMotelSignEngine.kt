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
import androidx.compose.ui.graphics.drawscope.Stroke
import fr.geoking.arthur.genart.GenartQuality
import fr.geoking.arthur.genart.TonalPalette
import fr.geoking.arthur.genart.loopedFbm
import fr.geoking.arthur.genart.phase01
import fr.geoking.arthur.genart.qualityCount
import fr.geoking.arthur.genart.seededRange
import fr.geoking.arthur.genart.seededUnit
import kotlin.math.PI
import kotlin.math.sin

/** Quiet roadside motel sign glowing over a dark desert road. */
@Composable
internal fun UsMotelSignEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val moteCount = qualityCount(quality, low = 4, medium = 7, high = 11)
    val motes = List(moteCount) { i ->
        Offset(seededUnit(i * 17 + 3), seededRange(i * 29 + 7, 0.14f, 0.72f))
    }
    val transition = rememberInfiniteTransition(label = "usmotelsign")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((68000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "usmotelsign_t",
    )
    val dim = if (isActive) 1f else 0.55f

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val unit = minOf(w, h)
        val time = phase01(t)
        val wash = loopedFbm(time, radius = 1.35f, octaves = 3, seedOffset = 31)
        val flicker = 0.92f + 0.08f * loopedFbm(time, radius = 2.1f, octaves = 2, seedOffset = 73)
        val warm = TonalPalette.brightness(TonalPalette.mix(Color(0xFFFFE060), TonalPalette.pick(paletteColors, 0), 0.2f), brightness * dim)
        val red = TonalPalette.brightness(Color(0xFFFF5A4E), brightness * dim)
        val cyan = TonalPalette.brightness(Color(0xFF6EDDE8), brightness * dim)

        drawRect(brush = Brush.verticalGradient(listOf(Color(0xFF080A10), Color(0xFF111321), Color(0xFF17151A))))
        // Faint desert horizon and distant mesa silhouettes.
        val horizonY = h * 0.70f
        drawRect(
            brush = Brush.verticalGradient(
                0f to Color.Transparent,
                1f to TonalPalette.withAlpha(warm, 0.12f * flicker * dim),
                startY = horizonY - h * 0.12f,
                endY = horizonY + h * 0.12f,
            ),
        )
        drawOval(Color(0xFF11131A), Offset(-w * 0.12f, horizonY - h * 0.10f), Size(w * 0.56f, h * 0.19f))
        drawOval(Color(0xFF0D1017), Offset(w * 0.48f, horizonY - h * 0.08f), Size(w * 0.72f, h * 0.16f))
        drawRect(Color(0xFF090B10), Offset(0f, horizonY), Size(w, h - horizonY))

        // Road shoulder catches just a little of the sign's light.
        drawRect(
            brush = Brush.verticalGradient(
                0f to Color.Transparent,
                1f to TonalPalette.withAlpha(warm, 0.10f * flicker * dim),
                startY = h * 0.78f,
                endY = h,
            ),
        )
        drawLine(Color(0xFF353039), Offset(w * 0.43f, h), Offset(w * 0.48f, horizonY), unit * 0.008f)
        drawLine(Color(0xFF353039), Offset(w * 0.57f, h), Offset(w * 0.52f, horizonY), unit * 0.008f)

        // Pole and crossbar silhouette.
        val signCenter = Offset(w * 0.66f, h * 0.39f)
        val signWidth = unit * 0.43f
        val signHeight = unit * 0.31f
        drawLine(Color(0xFF17191F), Offset(signCenter.x, signCenter.y + signHeight * 0.45f), Offset(signCenter.x, h * 0.88f), unit * 0.025f)
        drawLine(Color(0xFF17191F), Offset(signCenter.x - signWidth * 0.24f, h * 0.62f), Offset(signCenter.x + signWidth * 0.24f, h * 0.62f), unit * 0.014f)

        // Diffuse neon bloom behind the sign, then its framed enamel face.
        drawCircle(
            brush = Brush.radialGradient(
                0f to TonalPalette.withAlpha(warm, 0.30f * flicker * dim),
                0.45f to TonalPalette.withAlpha(red, 0.12f * flicker * dim),
                1f to Color.Transparent,
                center = signCenter,
                radius = unit * 0.48f,
            ),
            radius = unit * 0.48f,
            center = signCenter,
        )
        val signTopLeft = Offset(signCenter.x - signWidth / 2f, signCenter.y - signHeight / 2f)
        val signSize = Size(signWidth, signHeight)
        drawRoundRect(Color(0xFF18171C), signTopLeft, signSize, CornerRadius(unit * 0.035f))
        drawRoundRect(
            color = TonalPalette.withAlpha(red, 0.65f * flicker * dim),
            topLeft = signTopLeft,
            size = signSize,
            cornerRadius = CornerRadius(unit * 0.035f),
            style = Stroke(width = unit * 0.009f),
        )
        val inset = unit * 0.025f
        drawRoundRect(
            color = TonalPalette.withAlpha(warm, 0.18f * dim),
            topLeft = signTopLeft + Offset(inset, inset),
            size = Size(signWidth - inset * 2f, signHeight - inset * 2f),
            cornerRadius = CornerRadius(unit * 0.022f),
        )

        // Graphic lettering: a bright header bar, smaller cyan rule, and room lights.
        val headerY = signCenter.y - signHeight * 0.19f
        drawLine(TonalPalette.withAlpha(warm, 0.92f * flicker * dim), Offset(signCenter.x - signWidth * 0.29f, headerY), Offset(signCenter.x + signWidth * 0.29f, headerY), unit * 0.020f)
        drawLine(TonalPalette.withAlpha(cyan, 0.80f * flicker * dim), Offset(signCenter.x - signWidth * 0.21f, signCenter.y - signHeight * 0.01f), Offset(signCenter.x + signWidth * 0.21f, signCenter.y - signHeight * 0.01f), unit * 0.009f)
        drawLine(TonalPalette.withAlpha(warm, 0.74f * flicker * dim), Offset(signCenter.x - signWidth * 0.24f, signCenter.y + signHeight * 0.13f), Offset(signCenter.x + signWidth * 0.24f, signCenter.y + signHeight * 0.13f), unit * 0.008f)
        for (i in -1..1) {
            val x = signCenter.x + signWidth * i * 0.20f
            drawCircle(TonalPalette.withAlpha(warm, 0.8f * flicker * dim), unit * 0.018f, Offset(x, signCenter.y + signHeight * 0.32f))
        }
        drawCircle(
            color = TonalPalette.withAlpha(cyan, 0.75f * flicker * dim),
            radius = unit * 0.022f,
            center = Offset(signCenter.x + signWidth * 0.39f, signCenter.y - signHeight * 0.30f),
        )

        // Sparse dust and stars, with only slight looped drift.
        motes.forEachIndexed { i, mote ->
            val drift = loopedFbm(time, radius = 1.4f, octaves = 2, seedOffset = i * 37 + 5)
            val x = (mote.x + (drift - 0.5f) * 0.025f) * w
            val y = (mote.y + 0.008f * sin(time * 2f * PI.toFloat() + i)) * h
            val radius = unit * seededRange(i * 41 + 11, 0.0015f, 0.004f)
            drawCircle(TonalPalette.withAlpha(if (i % 3 == 0) warm else Color(0xFF9AA8C0), 0.42f * dim), radius, Offset(x, y))
        }
    }
}

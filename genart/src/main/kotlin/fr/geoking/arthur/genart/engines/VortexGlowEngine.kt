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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import fr.geoking.arthur.genart.GenartQuality
import fr.geoking.arthur.genart.TonalPalette
import fr.geoking.arthur.genart.phase01
import fr.geoking.arthur.genart.qualityCount
import fr.geoking.arthur.genart.seededRange
import fr.geoking.arthur.genart.sin01
import kotlin.math.min

/** High-contrast rotating plasma vortex — cyan/magenta arms, white-hot core, soft energy rings. */
@Composable
internal fun VortexGlowEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val count = qualityCount(quality, low = 7, medium = 11, high = 16)
    val arms = remember(count) {
        List(count) { i ->
            VortexArm(
                angleOffsetDeg = (360f / count) * i,
                radiusScale = seededRange(i * 23 + 11, 0.4f, 0.98f),
                colorIdx = i,
                magentaBias = seededRange(i * 41 + 7, 0f, 1f),
            )
        }
    }
    val ringCount = qualityCount(quality, low = 3, medium = 4, high = 6)
    val rings = remember(ringCount) {
        List(ringCount) { i ->
            VortexRingSeed(
                radiusFrac = seededRange(i * 31 + 5, 0.22f, 0.72f),
                alpha = seededRange(i * 47 + 9, 0.18f, 0.4f),
                colorIdx = i + 1,
            )
        }
    }
    val transition = rememberInfiniteTransition(label = "vortex_glow")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((32000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "vortex_glow_t",
    )
    val pulseT by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((90000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "vortex_glow_pulse",
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val center = Offset(w / 2f, h / 2f)
        val maxRadius = min(w, h) * 0.72f
        drawRect(color = Color(0xFF020108))

        val time = phase01(t)
        val rotationDeg = time * 360f
        val breathe = sin01(phase01(pulseT) * 2f * kotlin.math.PI.toFloat())
        val dim = if (isActive) 1f else 0.55f
        val cyan = Color(0xFF3DFFF5)
        val magenta = Color(0xFFFF2EC8)
        val ice = Color(0xFFF0FBFF)

        arms.forEach { arm ->
            val pick = TonalPalette.pick(paletteColors, arm.colorIdx)
            val sciFi = TonalPalette.mix(if (arm.magentaBias > 0.5f) magenta else cyan, pick, 0.3f)
            val c = TonalPalette.brightness(sciFi, brightness)
            val armRadius = maxRadius * arm.radiusScale * (0.94f + 0.06f * breathe)

            rotate(rotationDeg + arm.angleOffsetDeg, pivot = center) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            TonalPalette.withAlpha(c, 0.55f * dim),
                            TonalPalette.withAlpha(c, 0.22f * dim),
                            Color.Transparent,
                        ),
                        center = Offset(center.x + armRadius * 0.42f, center.y),
                        radius = armRadius * 0.58f,
                    ),
                    radius = armRadius * 0.58f,
                    center = Offset(center.x + armRadius * 0.42f, center.y),
                )
            }
        }

        rings.forEach { ring ->
            val pick = TonalPalette.pick(paletteColors, ring.colorIdx)
            val tint = TonalPalette.brightness(TonalPalette.mix(cyan, pick, 0.4f), brightness)
            val r = maxRadius * ring.radiusFrac * (0.96f + 0.04f * breathe)
            drawCircle(
                color = TonalPalette.withAlpha(tint, ring.alpha * dim * (0.75f + 0.25f * breathe)),
                radius = r,
                center = center,
                style = Stroke(width = 1.8f + breathe * 0.8f, cap = StrokeCap.Round),
            )
        }

        // Hot core — white → cyan → magenta fringe
        val coreR = maxRadius * (0.28f + 0.04f * breathe)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    TonalPalette.withAlpha(ice, 0.75f * dim * brightness.coerceAtMost(1.35f)),
                    TonalPalette.withAlpha(cyan, 0.4f * dim),
                    TonalPalette.withAlpha(magenta, 0.18f * dim),
                    Color.Transparent,
                ),
                center = center,
                radius = coreR,
            ),
            radius = coreR,
            center = center,
        )

        // Soft dark center shadow for depth (portal feel)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.Black.copy(alpha = 0.55f * dim),
                    Color.Transparent,
                ),
                center = center,
                radius = maxRadius * 0.12f,
            ),
            radius = maxRadius * 0.12f,
            center = center,
        )
    }
}

private data class VortexArm(
    val angleOffsetDeg: Float,
    val radiusScale: Float,
    val colorIdx: Int,
    val magentaBias: Float,
)

private data class VortexRingSeed(
    val radiusFrac: Float,
    val alpha: Float,
    val colorIdx: Int,
)

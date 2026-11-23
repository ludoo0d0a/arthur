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
import fr.geoking.arthur.genart.GenartQuality
import fr.geoking.arthur.genart.TonalPalette
import fr.geoking.arthur.genart.lerp
import fr.geoking.arthur.genart.loopedFbm
import fr.geoking.arthur.genart.phase01
import fr.geoking.arthur.genart.positiveRadius
import fr.geoking.arthur.genart.qualityCount
import fr.geoking.arthur.genart.seededRange
import fr.geoking.arthur.genart.seededUnit
import kotlin.math.PI
import kotlin.math.sin

/** Night ride through a single warm headlamp beam; all movement loops smoothly. */
@Composable
internal fun MotoHeadlightEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val count = qualityCount(quality, low = 4, medium = 7, high = 11)
    val particles = List(count) { i ->
        Triple(seededUnit(i * 17 + 3), seededUnit(i * 29 + 7), seededRange(i * 41 + 11, 0.0015f, 0.0045f))
    }
    val transition = rememberInfiniteTransition(label = "motoheadlight")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((68000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "motoheadlight_t",
    )
    val dim = if (isActive) 1f else 0.55f

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val unit = minOf(w, h)
        val time = phase01(t)
        val sway = (loopedFbm(time, radius = 1.25f, octaves = 2, seedOffset = 11) - 0.5f) * w * 0.035f
        val pulse = 0.93f + 0.07f * loopedFbm(time, radius = 1.45f, octaves = 2, seedOffset = 29)
        val gold = TonalPalette.brightness(
            TonalPalette.mix(Color(0xFFFFF0C0), TonalPalette.pick(paletteColors, 0), 0.25f),
            brightness * dim,
        )

        drawRect(
            brush = Brush.verticalGradient(
                0f to TonalPalette.brightness(Color(0xFF080A0D), brightness * dim),
                0.58f to TonalPalette.brightness(Color(0xFF141820), brightness * dim),
                1f to Color(0xFF07090C),
            ),
        )

        val horizonY = h * 0.52f
        val beamCenter = Offset(w * 0.5f + sway, h * 0.79f)

        // Low horizon haze lets the single headlamp beam read through the darkness.
        drawOval(
            brush = Brush.radialGradient(
                0f to TonalPalette.withAlpha(gold, 0.10f * pulse * dim),
                0.55f to TonalPalette.withAlpha(Color(0xFF87909A), 0.035f * dim),
                1f to Color.Transparent,
                center = Offset(w * 0.5f, horizonY),
                radius = unit * 0.72f,
            ),
            topLeft = Offset(-w * 0.18f, horizonY - unit * 0.24f),
            size = Size(w * 1.36f, unit * 0.48f),
        )

        // Broad triangular cone is softened by overlaid gradients rather than a hard beam edge.
        val beam = Path().apply {
            moveTo(beamCenter.x - unit * 0.06f, beamCenter.y)
            lineTo(w * 0.18f + sway * 0.22f, horizonY)
            quadraticTo(w * 0.5f + sway, horizonY - unit * 0.06f, w * 0.82f + sway * 0.22f, horizonY)
            lineTo(beamCenter.x + unit * 0.06f, beamCenter.y)
            close()
        }
        drawPath(
            path = beam,
            brush = Brush.verticalGradient(
                0f to TonalPalette.withAlpha(gold, 0.015f * dim),
                0.55f to TonalPalette.withAlpha(gold, 0.055f * pulse * dim),
                1f to TonalPalette.withAlpha(gold, 0.16f * pulse * dim),
                startY = horizonY,
                endY = beamCenter.y,
            ),
        )

        // Road plane and pale center line emerge only inside the beam.
        val road = Path().apply {
            moveTo(w * 0.44f, h)
            lineTo(w * 0.47f, horizonY)
            quadraticTo(w * 0.5f + sway * 0.12f, horizonY - unit * 0.01f, w * 0.53f, horizonY)
            lineTo(w * 0.70f, h)
            close()
        }
        drawPath(
            path = road,
            brush = Brush.verticalGradient(
                0f to Color(0xFF11151A),
                1f to TonalPalette.withAlpha(Color(0xFF9A8D69), 0.15f * dim),
                startY = horizonY,
                endY = h,
            ),
        )
        for (i in 0..3) {
            val progress = (i / 3f + time) % 1f
            val y = lerp(horizonY + unit * 0.04f, h * 0.96f, progress * progress)
            val width = unit * (0.003f + progress * 0.012f)
            drawRoundRect(
                color = TonalPalette.withAlpha(gold, (0.16f + 0.20f * progress) * pulse * dim),
                topLeft = Offset(w * 0.5f + sway * 0.12f - width / 2f, y),
                size = Size(width, unit * (0.008f + progress * 0.045f)),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(width),
            )
        }

        // Haze flecks crossing the beam; particle positions wrap on the seamless loop.
        particles.forEachIndexed { i, (x0, y0, radius) ->
            val drift = (loopedFbm(time, radius = 1.1f + radius, octaves = 2, seedOffset = i * 53 + 7) - 0.5f)
            val x = x0 * w + drift * unit * 0.035f + sway * 0.2f
            val y = ((y0 - time * (0.08f + radius * 6f) + 1f) % 1f) * h
            val nearBeam = (1f - kotlin.math.abs(x - beamCenter.x) / (unit * 0.46f)).coerceIn(0f, 1f)
            if (nearBeam > 0f) {
                drawCircle(
                    brush = Brush.radialGradient(
                        0f to TonalPalette.withAlpha(gold, 0.28f * nearBeam * dim),
                        1f to Color.Transparent,
                        center = Offset(x, y),
                        radius = (unit * radius * (0.8f + 0.4f * sin(time * 2f * PI.toFloat() + i))).positiveRadius(),
                    ),
                    radius = (unit * radius * 3f).positiveRadius(),
                    center = Offset(x, y),
                )
            }
        }

        // Compact foreground fairing anchors the POV without competing with the beam.
        val fairing = Path().apply {
            moveTo(w * 0.28f, h)
            quadraticTo(w * 0.34f, h * 0.84f, beamCenter.x, h * 0.86f)
            quadraticTo(w * 0.66f, h * 0.84f, w * 0.72f, h)
            close()
        }
        drawPath(
            path = fairing,
            brush = Brush.verticalGradient(
                0f to Color(0xFF15191E),
                1f to Color(0xFF050609),
                startY = h * 0.84f,
                endY = h,
            ),
        )

        // The one warm lamp and its soft halo sit just above the lower fairing.
        drawCircle(
            brush = Brush.radialGradient(
                0f to TonalPalette.withAlpha(Color(0xFFFFF8DE), 0.95f * pulse * dim),
                0.22f to TonalPalette.withAlpha(gold, 0.76f * pulse * dim),
                0.62f to TonalPalette.withAlpha(gold, 0.17f * dim),
                1f to Color.Transparent,
                center = beamCenter,
                radius = unit * 0.075f,
            ),
            radius = unit * 0.075f,
            center = beamCenter,
        )
        drawOval(
            color = TonalPalette.withAlpha(gold, 0.48f * dim),
            topLeft = Offset(beamCenter.x - unit * 0.038f, beamCenter.y - unit * 0.006f),
            size = Size(unit * 0.076f, unit * 0.012f),
        )
    }
}

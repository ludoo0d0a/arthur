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
import fr.geoking.arthur.genart.phase01
import fr.geoking.arthur.genart.qualityCount
import fr.geoking.arthur.genart.seededRange
import fr.geoking.arthur.genart.seededUnit
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Slowly rotating cluster of soft-edged landmass blobs over a dark ocean, viewed from orbit. */
@Composable
internal fun ContinentsEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val count = qualityCount(quality, low = 2, medium = 3, high = 4)
    val lobeBudget = qualityCount(quality, low = 3, medium = 4, high = 6)
    val continents = remember(count, lobeBudget) {
        List(count) { i ->
            val lobeCount = (lobeBudget - 1 + (seededUnit(i * 131 + 5) * 2f).toInt()).coerceIn(3, 8)
            ContinentSeed(
                centerAngle = seededRange(i * 17 + 3, 0f, 2f * PI.toFloat()),
                centerRadiusFrac = seededRange(i * 29 + 7, 0.16f, 0.42f),
                scale = seededRange(i * 41 + 11, 0.55f, 1.05f),
                colorIndex = i,
                lobes = List(lobeCount) { l ->
                    LobeSeed(
                        dx = seededRange(i * 200 + l * 13 + 5, -0.5f, 0.5f),
                        dy = seededRange(i * 200 + l * 17 + 7, -0.5f, 0.5f),
                        radiusFrac = seededRange(i * 200 + l * 19 + 11, 0.24f, 0.5f),
                    )
                },
            )
        }
    }
    val transition = rememberInfiniteTransition(label = "continents")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((120000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "continents_t",
    )
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val minDim = minOf(w, h)
        val cx = w * 0.5f
        val cy = h * 0.5f
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF0A1830), Color(0xFF030A16)),
                center = Offset(cx, cy),
                radius = maxOf(w, h) * 0.8f,
            ),
        )
        val driftScale = if (isActive) 1f else 0.35f
        val dim = if (isActive) 1f else 0.6f
        val time = phase01(t) * driftScale
        val rotation = time * 2f * PI.toFloat()
        continents.forEach { continent ->
            val angle = continent.centerAngle + rotation
            val ox = cx + cos(angle) * continent.centerRadiusFrac * minDim
            val oy = cy + sin(angle) * continent.centerRadiusFrac * minDim
            val land = TonalPalette.brightness(
                TonalPalette.pick(paletteColors, continent.colorIndex),
                brightness * 0.75f,
            )
            continent.lobes.forEach { lobe ->
                val lx = ox + lobe.dx * continent.scale * minDim * 0.4f
                val ly = oy + lobe.dy * continent.scale * minDim * 0.4f
                val radius = lobe.radiusFrac * continent.scale * minDim * 0.32f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            TonalPalette.withAlpha(land, 0.85f * dim),
                            TonalPalette.withAlpha(land, 0.5f * dim),
                            Color.Transparent,
                        ),
                        center = Offset(lx, ly),
                        radius = radius,
                    ),
                    radius = radius,
                    center = Offset(lx, ly),
                )
            }
        }
    }
}

private data class ContinentSeed(
    val centerAngle: Float,
    val centerRadiusFrac: Float,
    val scale: Float,
    val colorIndex: Int,
    val lobes: List<LobeSeed>,
)

private data class LobeSeed(
    val dx: Float,
    val dy: Float,
    val radiusFrac: Float,
)

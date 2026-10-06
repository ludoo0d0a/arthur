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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import fr.geoking.arthur.genart.GenartQuality
import fr.geoking.arthur.genart.TonalPalette
import fr.geoking.arthur.genart.loopedFbm
import fr.geoking.arthur.genart.phase01
import fr.geoking.arthur.genart.qualityCount
import fr.geoking.arthur.genart.sin01
import kotlin.math.PI

/**
 * Soft procedural height-band silhouettes that slowly terraform — ridges rise/settle,
 * valleys deepen, palette rock → grass → snow over a multi-minute cycle.
 * Analytic / low-frequency fbm only (no hydraulic erosion).
 */
@Composable
internal fun TerrainMakerEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val layerCount = qualityCount(quality, low = 3, medium = 4, high = 5)
    val sampleCount = qualityCount(quality, low = 28, medium = 40, high = 56)
    val layers = remember(layerCount) {
        List(layerCount) { i ->
            val distanceT = 1f - i / (layerCount - 1).coerceAtLeast(1).toFloat()
            TerrainMakerLayerSeed(
                distanceT = distanceT,
                baselineFrac = 0.72f - distanceT * 0.28f,
                amplitudeFrac = 0.08f + distanceT * 0.22f,
                noiseScale = 0.7f + distanceT * 1.4f,
                seedOffset = i * 211 + 17,
                colorIndex = i,
            )
        }
    }

    val transition = rememberInfiniteTransition(label = "terrainmaker")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((180000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "terrainmaker_t",
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val dim = if (isActive) 1f else 0.55f
        val cycle = phase01(t)
        val biome = cycle * 3f
        val rock = Color(0xFF6B5A4A)
        val grass = Color(0xFF4A7A4E)
        val snow = Color(0xFFE8EEF4)
        val biomeColor = when {
            biome < 1f -> TonalPalette.mix(rock, grass, sin01(biome * PI.toFloat() / 2f))
            biome < 2f -> TonalPalette.mix(grass, snow, sin01((biome - 1f) * PI.toFloat() / 2f))
            else -> TonalPalette.mix(snow, rock, sin01((biome - 2f) * PI.toFloat() / 2f))
        }
        val skyTop = TonalPalette.mix(Color(0xFF7BA3C9), Color(0xFFC9D6E8), sin01(cycle * 2f * PI.toFloat()) * 0.5f)
        val skyBot = TonalPalette.mix(Color(0xFFB8C9A8), biomeColor, 0.35f)

        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    TonalPalette.brightness(skyTop, brightness * dim),
                    TonalPalette.brightness(skyBot, brightness * 0.85f * dim),
                ),
            ),
        )

        val time = cycle * 2f * PI.toFloat()
        layers.forEach { layer ->
            val path = Path()
            path.moveTo(0f, h)
            for (s in 0..sampleCount) {
                val xFrac = s / sampleCount.toFloat()
                val x = xFrac * w
                val n1 = loopedFbm(
                    t = time * 0.15f + xFrac * layer.noiseScale,
                    radius = 1.2f,
                    octaves = 2,
                    seedOffset = layer.seedOffset,
                )
                val n2 = loopedFbm(
                    t = time * 0.08f + xFrac * layer.noiseScale * 0.55f + 1.7f,
                    radius = 1.4f,
                    octaves = 2,
                    seedOffset = layer.seedOffset + 97,
                )
                val rise = 0.55f + 0.45f * n1
                val valley = 0.75f + 0.25f * n2
                val height = layer.amplitudeFrac * rise * valley
                val y = h * (layer.baselineFrac - height)
                if (s == 0) path.lineTo(0f, y) else path.lineTo(x, y)
            }
            path.lineTo(w, h)
            path.close()

            val haze = Color(0xFF8AA0B8)
            val base = TonalPalette.mix(
                TonalPalette.pick(paletteColors, layer.colorIndex),
                biomeColor,
                0.55f,
            )
            val tinted = TonalPalette.mix(base, haze, (1f - layer.distanceT) * 0.45f)
            val alpha = (0.35f + layer.distanceT * 0.55f) * dim
            val color = TonalPalette.withAlpha(
                TonalPalette.brightness(tinted, brightness * (0.45f + layer.distanceT * 0.5f)),
                alpha,
            )
            drawPath(path = path, color = color, style = Fill)
        }
    }
}

private data class TerrainMakerLayerSeed(
    val distanceT: Float,
    val baselineFrac: Float,
    val amplitudeFrac: Float,
    val noiseScale: Float,
    val seedOffset: Int,
    val colorIndex: Int,
)

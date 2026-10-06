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
import androidx.compose.ui.graphics.drawscope.Fill
import fr.geoking.arthur.genart.GenartQuality
import fr.geoking.arthur.genart.TonalPalette
import fr.geoking.arthur.genart.loopedFbm
import fr.geoking.arthur.genart.phase01
import fr.geoking.arthur.genart.qualityCount
import fr.geoking.arthur.genart.seededRange
import fr.geoking.arthur.genart.seededUnit
import kotlin.math.PI
import kotlin.math.sin

/**
 * Several distinct flame clusters (campfires / braziers) with soft stacked teardrops
 * and ember sparks. Independent breathe phases — cozier than single wildfire [FireEngine].
 */
@Composable
internal fun MultiFlamesEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val clusterCount = qualityCount(quality, low = 2, medium = 3, high = 4)
    val tonguesPerCluster = qualityCount(quality, low = 2, medium = 3, high = 4)
    val embersPerCluster = qualityCount(quality, low = 4, medium = 7, high = 10)

    val clusters = remember(clusterCount, tonguesPerCluster, embersPerCluster) {
        List(clusterCount) { c ->
            val baseX = seededRange(c * 17 + 3, 0.15f, 0.85f)
            val baseY = seededRange(c * 29 + 7, 0.62f, 0.88f)
            val scale = seededRange(c * 41 + 11, 0.65f, 1.15f)
            val breathPhase = seededUnit(c * 53 + 13)
            val tongues = List(tonguesPerCluster) { i ->
                val seed = c * 1000 + i
                MultiFlamesTongueSeed(
                    offsetX = seededRange(seed * 17 + 3, -0.04f, 0.04f),
                    widthFrac = seededRange(seed * 29 + 7, 0.025f, 0.055f) * scale,
                    heightFrac = seededRange(seed * 41 + 11, 0.12f, 0.28f) * scale,
                    swayAmp = seededRange(seed * 53 + 13, 0.008f, 0.02f),
                    swayFreq = seededRange(seed * 67 + 19, 0.6f, 1.4f),
                    flickerFreq = seededRange(seed * 79 + 23, 0.8f, 1.8f),
                    flickerPhase = seededRange(seed * 89 + 29, 0f, 2f * PI.toFloat()),
                    colorIndex = c + i,
                )
            }
            val embers = List(embersPerCluster) { i ->
                val seed = c * 2000 + i
                MultiFlamesEmberSeed(
                    startX = seededRange(seed * 17 + 3, -0.06f, 0.06f),
                    phaseOffset = seededUnit(seed * 29 + 7),
                    riseSpeed = seededRange(seed * 41 + 11, 0.4f, 1.0f),
                    jitterAmp = seededRange(seed * 53 + 13, 0.008f, 0.025f),
                    jitterFreq = seededRange(seed * 67 + 19, 0.5f, 1.8f),
                    radius = seededRange(seed * 79 + 23, 1.5f, 4.5f),
                    colorIndex = c + i,
                )
            }
            MultiFlamesClusterSeed(
                baseX = baseX,
                baseY = baseY,
                scale = scale,
                breathPhase = breathPhase,
                tongues = tongues,
                embers = embers,
            )
        }
    }

    val transition = rememberInfiniteTransition(label = "multiflames")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((28000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "multiflames_t",
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val minDim = minOf(w, h)
        val dim = if (isActive) 1f else 0.55f
        val time = phase01(t) * 2f * PI.toFloat()

        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF060308), Color(0xFF120805), Color(0xFF1A0C06)),
            ),
        )

        clusters.forEach { cluster ->
            val cx = cluster.baseX * w
            val cy = cluster.baseY * h
            val breath = 0.82f + 0.18f * loopedFbm(
                t = time * 0.25f + cluster.breathPhase * 2f * PI.toFloat(),
                radius = 1.0f,
                octaves = 2,
                seedOffset = (cluster.breathPhase * 1000f).toInt(),
            )

            val glowR = minDim * 0.12f * cluster.scale
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        TonalPalette.withAlpha(Color(0xFFFF8844), 0.35f * dim * breath),
                        TonalPalette.withAlpha(Color(0xFFFF6622), 0.12f * dim),
                        Color.Transparent,
                    ),
                    center = Offset(cx, cy),
                    radius = glowR * 2.2f,
                ),
                radius = glowR * 2.2f,
                center = Offset(cx, cy),
            )
            drawOval(
                color = TonalPalette.withAlpha(Color(0xFF2A1810), 0.85f * dim),
                topLeft = Offset(cx - glowR * 0.7f, cy - glowR * 0.12f),
                size = Size(glowR * 1.4f, glowR * 0.28f),
            )

            cluster.tongues.forEach { tongue ->
                val sway = sin(time * tongue.swayFreq + tongue.flickerPhase) * tongue.swayAmp
                val flicker = (0.75f + 0.25f * sin(time * tongue.flickerFreq + tongue.flickerPhase))
                    .coerceIn(0.5f, 1f)
                val baseX = cx + tongue.offsetX * w + sway * w
                val height = tongue.heightFrac * breath * minDim
                val halfWidth = tongue.widthFrac * minDim * flicker
                val tipY = cy - height
                val baseY = cy
                val tint = TonalPalette.pick(paletteColors, tongue.colorIndex)
                val flame = TonalPalette.mix(tint, Color(0xFFFFAA44), 0.35f)
                val bright = TonalPalette.brightness(flame, brightness * 1.15f * dim)
                val outer = TonalPalette.brightness(flame, brightness * 0.55f * dim)
                val path = Path().apply {
                    moveTo(baseX - halfWidth, baseY)
                    cubicTo(
                        baseX - halfWidth * 1.25f, tipY + height * 0.35f,
                        baseX + halfWidth * 1.25f, tipY + height * 0.35f,
                        baseX + halfWidth * 0.15f, tipY,
                    )
                    cubicTo(
                        baseX + halfWidth * 0.9f, tipY + height * 0.25f,
                        baseX - halfWidth * 0.9f, tipY + height * 0.25f,
                        baseX - halfWidth, baseY,
                    )
                    close()
                }
                drawPath(
                    path = path,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            TonalPalette.withAlpha(outer, 0f),
                            TonalPalette.withAlpha(outer, 0.55f),
                            TonalPalette.withAlpha(bright, 0.9f),
                            TonalPalette.withAlpha(bright, 0.15f),
                        ),
                        startY = tipY,
                        endY = baseY,
                    ),
                    style = Fill,
                )
            }

            cluster.embers.forEach { ember ->
                val life = phase01(t * ember.riseSpeed + ember.phaseOffset + cluster.breathPhase)
                val rise = life * minDim * 0.22f * cluster.scale
                val jitter = ember.jitterAmp * sin(time * ember.jitterFreq + ember.phaseOffset * 8f)
                val ex = cx + ember.startX * w + jitter * w
                val ey = cy - rise
                val fade = (life / 0.12f).coerceIn(0f, 1f) * ((1f - life) / 0.25f).coerceIn(0f, 1f)
                val tint = TonalPalette.mix(
                    Color(0xFFFFCC66),
                    TonalPalette.pick(paletteColors, ember.colorIndex),
                    0.3f,
                )
                val alpha = 0.55f * fade * dim * breath
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            TonalPalette.withAlpha(tint, alpha),
                            Color.Transparent,
                        ),
                        center = Offset(ex, ey),
                        radius = ember.radius * 2.2f,
                    ),
                    radius = ember.radius * 2.2f,
                    center = Offset(ex, ey),
                )
            }
        }
    }
}

private data class MultiFlamesClusterSeed(
    val baseX: Float,
    val baseY: Float,
    val scale: Float,
    val breathPhase: Float,
    val tongues: List<MultiFlamesTongueSeed>,
    val embers: List<MultiFlamesEmberSeed>,
)

private data class MultiFlamesTongueSeed(
    val offsetX: Float,
    val widthFrac: Float,
    val heightFrac: Float,
    val swayAmp: Float,
    val swayFreq: Float,
    val flickerFreq: Float,
    val flickerPhase: Float,
    val colorIndex: Int,
)

private data class MultiFlamesEmberSeed(
    val startX: Float,
    val phaseOffset: Float,
    val riseSpeed: Float,
    val jitterAmp: Float,
    val jitterFreq: Float,
    val radius: Float,
    val colorIndex: Int,
)

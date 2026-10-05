package fr.geoking.arthur.genart.engines

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import fr.geoking.arthur.genart.GenartQuality
import fr.geoking.arthur.genart.TonalPalette
import fr.geoking.arthur.genart.phase01
import fr.geoking.arthur.genart.qualityCount
import fr.geoking.arthur.genart.seededRange
import fr.geoking.arthur.genart.seededUnit
import kotlin.math.PI
import kotlin.math.sin

/**
 * Soft reflections drifting across a vertical glass pane — blurred city/light blobs
 * behind the glass, specular streaks and glints sliding slowly on the surface.
 */
@Composable
internal fun PaneReflectionsEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val blobCount = qualityCount(quality, low = 4, medium = 6, high = 9)
    val glintCount = qualityCount(quality, low = 8, medium = 14, high = 22)
    val streakCount = qualityCount(quality, low = 3, medium = 5, high = 7)
    val blobs = remember(blobCount) {
        List(blobCount) { i ->
            PaneBlobSeed(
                x0 = seededUnit(i * 17 + 3),
                y0 = seededRange(i * 29 + 7, 0.15f, 0.85f),
                radiusFrac = seededRange(i * 41 + 11, 0.12f, 0.28f),
                drift = seededRange(i * 53 + 13, 0.02f, 0.06f),
                phase = seededRange(i * 67 + 19, 0f, 2f * PI.toFloat()),
                alpha = seededRange(i * 79 + 23, 0.18f, 0.4f),
                colorIndex = i,
            )
        }
    }
    val glints = remember(glintCount) {
        List(glintCount) { j ->
            PaneGlintSeed(
                x0 = seededUnit(j * 19 + 5),
                y0 = seededUnit(j * 31 + 11),
                sizeFrac = seededRange(j * 43 + 13, 0.004f, 0.018f),
                driftFreq = seededRange(j * 59 + 17, 0.2f, 0.55f),
                driftAmp = seededRange(j * 71 + 19, 0.01f, 0.04f),
                twinkleFreq = seededRange(j * 83 + 23, 0.4f, 1.4f),
                twinklePhase = seededRange(j * 97 + 29, 0f, 2f * PI.toFloat()),
                colorIndex = j,
            )
        }
    }
    val streaks = remember(streakCount) {
        List(streakCount) { k ->
            PaneStreakSeed(
                x0 = seededRange(k * 23 + 5, 0.1f, 0.9f),
                y0 = seededRange(k * 37 + 11, 0.05f, 0.35f),
                lengthFrac = seededRange(k * 47 + 13, 0.25f, 0.7f),
                thickness = seededRange(k * 61 + 17, 0.8f, 2.4f),
                slideFreq = seededRange(k * 73 + 19, 0.08f, 0.22f),
                slidePhase = seededRange(k * 89 + 23, 0f, 2f * PI.toFloat()),
                alpha = seededRange(k * 101 + 29, 0.06f, 0.16f),
            )
        }
    }
    val transition = rememberInfiniteTransition(label = "pane_reflections")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((42000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "pane_reflections_t",
    )

    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize().blur(22.dp)) {
            val w = size.width
            val h = size.height
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF101820), Color(0xFF060A10)),
                ),
            )
            val time = phase01(t)
            val dim = if (isActive) 1f else 0.5f
            blobs.forEach { blob ->
                val cx = phase01(blob.x0 + blob.drift * sin(time * 2f * PI.toFloat() + blob.phase)) * w
                val cy = blob.y0 * h
                val tint = TonalPalette.brightness(
                    TonalPalette.pick(paletteColors, blob.colorIndex),
                    brightness * 0.9f,
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            TonalPalette.withAlpha(tint, blob.alpha * dim),
                            Color.Transparent,
                        ),
                        center = Offset(cx, cy),
                        radius = blob.radiusFrac * w,
                    ),
                    radius = blob.radiusFrac * w,
                    center = Offset(cx, cy),
                )
            }
        }
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val minDim = minOf(w, h)
            val time = phase01(t) * 2f * PI.toFloat()
            val dim = if (isActive) 1f else 0.6f

            // Soft glass wash
            drawRect(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.04f * dim),
                        Color.Transparent,
                        Color.White.copy(alpha = 0.06f * dim),
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(w, h),
                ),
            )

            streaks.forEach { streak ->
                val slide = sin(time * streak.slideFreq + streak.slidePhase) * 0.08f
                val x = (streak.x0 + slide) * w
                val y0 = streak.y0 * h
                val y1 = y0 + streak.lengthFrac * h
                drawLine(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.White.copy(alpha = streak.alpha * dim),
                            Color.Transparent,
                        ),
                        startY = y0,
                        endY = y1,
                    ),
                    start = Offset(x, y0),
                    end = Offset(x, y1),
                    strokeWidth = streak.thickness,
                    cap = StrokeCap.Round,
                )
            }

            glints.forEach { glint ->
                val drift = glint.driftAmp * sin(time * glint.driftFreq + glint.twinklePhase)
                val x = phase01(glint.x0 + drift) * w
                val y = phase01(glint.y0 + drift * 0.4f) * h
                val twinkle = 0.35f + 0.65f * (0.5f + 0.5f * sin(time * glint.twinkleFreq + glint.twinklePhase))
                val tint = TonalPalette.brightness(
                    TonalPalette.mix(
                        Color.White,
                        TonalPalette.pick(paletteColors, glint.colorIndex),
                        0.35f,
                    ),
                    brightness,
                )
                val r = glint.sizeFrac * minDim * (0.7f + 0.5f * twinkle)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            TonalPalette.withAlpha(Color.White, 0.85f * twinkle * dim),
                            TonalPalette.withAlpha(tint, 0.35f * twinkle * dim),
                            Color.Transparent,
                        ),
                        center = Offset(x, y),
                        radius = r * 2.2f,
                    ),
                    radius = r * 2.2f,
                    center = Offset(x, y),
                )
                drawCircle(
                    color = TonalPalette.withAlpha(Color.White, 0.7f * twinkle * dim),
                    radius = r * 0.35f,
                    center = Offset(x, y),
                )
            }
        }
    }
}

private data class PaneBlobSeed(
    val x0: Float,
    val y0: Float,
    val radiusFrac: Float,
    val drift: Float,
    val phase: Float,
    val alpha: Float,
    val colorIndex: Int,
)

private data class PaneGlintSeed(
    val x0: Float,
    val y0: Float,
    val sizeFrac: Float,
    val driftFreq: Float,
    val driftAmp: Float,
    val twinkleFreq: Float,
    val twinklePhase: Float,
    val colorIndex: Int,
)

private data class PaneStreakSeed(
    val x0: Float,
    val y0: Float,
    val lengthFrac: Float,
    val thickness: Float,
    val slideFreq: Float,
    val slidePhase: Float,
    val alpha: Float,
)

package fr.geoking.arthur.fractal

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
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer

/** Composition zoom so strokes fill the frame without sparse letterboxing (near-4K). */
private const val FillScale = 1.13f

/**
 * Live Custom Fractal loop — sparse luminous Bezier strokes on AMOLED black.
 * Pauses visual progression when [isActive] is false (keeps last phase; low cost).
 */
@Composable
fun CustomFractalEffectCanvas(
    params: CustomFractalParams,
    isActive: Boolean,
    modifier: Modifier = Modifier,
    quality: FractalQuality = FractalQuality.Medium,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "custom_fractal")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 48_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "custom_phase",
    )
    var lastActivePhase by remember { mutableFloatStateOf(0f) }
    SideEffect {
        if (isActive) lastActivePhase = phase
    }
    val t = if (isActive) phase else lastActivePhase

    val engineQuality = remember(quality) {
        when (quality) {
            FractalQuality.Low -> CustomFractalQuality.Low
            FractalQuality.Medium -> CustomFractalQuality.Medium
            FractalQuality.High -> CustomFractalQuality.High
        }
    }
    val palette = remember(params.colorSeed) {
        CustomFractalEngine.paletteArgb(params.colorSeed).map { Color(it) }
    }
    val topology = remember(params, engineQuality) {
        CustomFractalEngine.topology(params, engineQuality)
    }
    val path = remember { Path() }
    val contrastWash = if (isActive) 0.10f else 0.04f
    val vignetteAlpha = if (isActive) 0.28f else 0.18f

    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
                scaleX = FillScale
                scaleY = FillScale
            },
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(Color.Black)
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        palette[0].copy(alpha = 0.14f),
                        palette.getOrElse(3) { palette[0] }.copy(alpha = 0.06f),
                        Color.Transparent,
                    ),
                    center = Offset(size.width * 0.5f, size.height * 0.45f),
                    radius = maxOf(size.width, size.height) * 0.7f,
                ),
            )

            val strokes = CustomFractalEngine.strokesAt(topology, t)
            val w = size.width
            val h = size.height
            val minDim = minOf(w, h)
            val baseStroke = (minDim * 0.0055f).coerceIn(1.6f, 14f)
            val alphaMul = if (isActive) 1f else 0.75f

            for (stroke in strokes) {
                if (stroke.cubics.isEmpty()) continue
                val c0 = palette[stroke.colorIndex % palette.size]
                val c1 = palette[(stroke.colorIndex + 2) % palette.size]
                val alpha = (stroke.alpha * alphaMul).coerceIn(0.12f, 0.98f)
                val start = stroke.start
                val end = stroke.end
                val brush = Brush.linearGradient(
                    colors = listOf(
                        c0.copy(alpha = alpha),
                        c1.copy(alpha = (alpha * 0.92f).coerceIn(0.08f, 0.98f)),
                        c0.copy(alpha = (alpha * 0.75f).coerceIn(0.08f, 0.98f)),
                    ),
                    start = Offset(start.x * w, start.y * h),
                    end = Offset(end.x * w, end.y * h),
                )
                path.reset()
                val first = stroke.cubics.first()
                path.moveTo(first.p0.x * w, first.p0.y * h)
                for (cubic in stroke.cubics) {
                    path.cubicTo(
                        cubic.c1.x * w,
                        cubic.c1.y * h,
                        cubic.c2.x * w,
                        cubic.c2.y * h,
                        cubic.p3.x * w,
                        cubic.p3.y * h,
                    )
                }
                val coreWidth = baseStroke * stroke.strokeScale
                // Soft underglow then sharp core (luminous neon without BlurMaskFilter).
                drawPath(
                    path = path,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            c0.copy(alpha = alpha * 0.28f),
                            c1.copy(alpha = alpha * 0.22f),
                            Color.Transparent,
                        ),
                        start = Offset(start.x * w, start.y * h),
                        end = Offset(end.x * w, end.y * h),
                    ),
                    style = Stroke(
                        width = coreWidth * 2.8f,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round,
                    ),
                )
                drawPath(
                    path = path,
                    brush = brush,
                    style = Stroke(
                        width = coreWidth,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round,
                    ),
                )
            }

            // Soft anchor glow so authored taps remain readable in preview
            params.points.forEach { p ->
                val center = Offset(p.x * w, p.y * h)
                val glowR = baseStroke * 3.4f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            palette.first().copy(alpha = 0.45f),
                            palette.getOrElse(2) { palette.first() }.copy(alpha = 0.14f),
                            Color.Transparent,
                        ),
                        center = center,
                        radius = glowR,
                    ),
                    radius = glowR,
                    center = center,
                )
                drawCircle(
                    color = palette.first().copy(alpha = 0.35f),
                    radius = baseStroke * 2.0f,
                    center = center,
                    style = Stroke(width = baseStroke * 0.5f, cap = StrokeCap.Round, join = StrokeJoin.Round),
                )
            }

            // Near-4K polish: soft center lift + edge vignette
            val cx = size.width * 0.5f
            val cy = size.height * 0.48f
            val polishR = minDim * 0.55f
            if (minDim > 0f) {
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = contrastWash),
                            Color.Transparent,
                        ),
                        center = Offset(cx, cy),
                        radius = polishR,
                    ),
                )
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = vignetteAlpha),
                        ),
                        center = Offset(cx, cy),
                        radius = minDim * 0.78f,
                    ),
                )
            }
        }
    }
}

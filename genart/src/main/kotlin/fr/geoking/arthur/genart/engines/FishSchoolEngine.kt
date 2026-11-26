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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import fr.geoking.arthur.genart.GenartQuality
import fr.geoking.arthur.genart.TonalPalette
import fr.geoking.arthur.genart.phase01
import fr.geoking.arthur.genart.qualityCount
import fr.geoking.arthur.genart.seededRange
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/** Calm underwater school — teardrop silhouettes clustered as a true school. */
@Composable
internal fun FishSchoolEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val count = qualityCount(quality, low = 20, medium = 32, high = 48)
    val fish = remember(count) {
        List(count) { i ->
            // Pack angleOffsets into a few arcs so the group reads as a school, not orbits.
            val arc = (i % 4) * (PI.toFloat() * 0.5f)
            val arcSpread = seededRange(i * 29 + 7, -0.35f, 0.35f)
            FishSeed(
                orbitRadius = seededRange(i * 17 + 3, 0.06f, 0.14f),
                angleOffset = arc + arcSpread,
                angularSpeed = seededRange(i * 41 + 11, 0.55f, 0.85f),
                scale = if (i < 3) {
                    seededRange(i * 53 + 13, 1.05f, 1.35f)
                } else {
                    seededRange(i * 53 + 13, 0.55f, 1.05f)
                },
                verticalTilt = seededRange(i * 67 + 19, 0.45f, 0.75f),
                colorIndex = i,
                leadBoost = if (i < 3) 1.15f else 1f,
            )
        }
    }
    val transition = rememberInfiniteTransition(label = "fish_school")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((28000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "fish_school_t",
    )
    val fishPath = remember { Path() }
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF061525), Color(0xFF020810)),
            ),
        )
        val time = phase01(t) * 2f * PI.toFloat()
        val schoolX = (0.5f + 0.28f * cos(time * 0.45f)) * w
        val schoolY = (0.5f + 0.18f * sin(time * 0.6f)) * h
        // Shared school heading from centre velocity.
        val schoolHeading = atan2(
            0.18f * 0.6f * cos(time * 0.6f),
            -0.28f * 0.45f * sin(time * 0.45f),
        ) * 180f / PI.toFloat()
        val minDim = minOf(w, h)
        val dim = if (isActive) 1f else 0.65f

        fish.forEach { f ->
            val angle = f.angleOffset + time * f.angularSpeed * 0.55f
            val fx = schoolX + cos(angle) * f.orbitRadius * minDim
            val fy = schoolY + sin(angle) * f.orbitRadius * minDim * f.verticalTilt
            val localHeading = atan2(
                cos(angle + 0.12f) - cos(angle),
                -(sin(angle + 0.12f) - sin(angle)),
            ) * 180f / PI.toFloat()
            // Blend shared school heading with slight local orbit heading.
            val heading = schoolHeading * 0.72f + localHeading * 0.28f
            val bodyLen = f.scale * f.leadBoost * minDim * 0.028f
            val bodyH = bodyLen * 0.38f
            val base = TonalPalette.pick(paletteColors, f.colorIndex)
            val color = TonalPalette.brightness(base, brightness)
            val alpha = (0.50f + 0.38f * f.scale.coerceIn(0.4f, 1.3f)) * dim * f.leadBoost.coerceAtMost(1.1f)
            rotate(degrees = heading, pivot = Offset(fx, fy)) {
                drawFishSilhouette(fishPath, fx, fy, bodyLen, bodyH, color, alpha)
            }
        }
    }
}

private fun DrawScope.drawFishSilhouette(
    path: Path,
    fx: Float,
    fy: Float,
    bodyLen: Float,
    bodyH: Float,
    color: Color,
    alpha: Float,
) {
    path.reset()
    // Teardrop body — nose to the right (heading +X).
    path.moveTo(fx + bodyLen, fy)
    path.cubicTo(
        fx + bodyLen * 0.7f, fy - bodyH,
        fx - bodyLen * 0.3f, fy - bodyH * 0.8f,
        fx - bodyLen * 0.55f, fy,
    )
    path.cubicTo(
        fx - bodyLen * 0.3f, fy + bodyH * 0.8f,
        fx + bodyLen * 0.7f, fy + bodyH,
        fx + bodyLen, fy,
    )
    path.close()
    drawPath(path, TonalPalette.withAlpha(color, alpha))

    // Dorsal fin.
    path.reset()
    path.moveTo(fx + bodyLen * 0.05f, fy - bodyH * 0.55f)
    path.lineTo(fx - bodyLen * 0.15f, fy - bodyH * 1.15f)
    path.lineTo(fx - bodyLen * 0.25f, fy - bodyH * 0.45f)
    path.close()
    drawPath(path, TonalPalette.withAlpha(color, alpha * 0.85f))

    // Forked tail.
    path.reset()
    path.moveTo(fx - bodyLen * 0.55f, fy)
    path.lineTo(fx - bodyLen * 0.95f, fy - bodyH * 0.75f)
    path.lineTo(fx - bodyLen * 0.72f, fy)
    path.lineTo(fx - bodyLen * 0.95f, fy + bodyH * 0.75f)
    path.close()
    drawPath(path, TonalPalette.withAlpha(color, alpha * 0.9f))

    // Eye.
    drawCircle(
        color = TonalPalette.withAlpha(Color.White, alpha * 0.75f),
        radius = bodyH * 0.14f,
        center = Offset(fx + bodyLen * 0.55f, fy - bodyH * 0.12f),
    )
    drawCircle(
        color = TonalPalette.withAlpha(Color.Black, alpha * 0.55f),
        radius = bodyH * 0.07f,
        center = Offset(fx + bodyLen * 0.58f, fy - bodyH * 0.12f),
    )
}

private data class FishSeed(
    val orbitRadius: Float,
    val angleOffset: Float,
    val angularSpeed: Float,
    val scale: Float,
    val verticalTilt: Float,
    val colorIndex: Int,
    val leadBoost: Float,
)

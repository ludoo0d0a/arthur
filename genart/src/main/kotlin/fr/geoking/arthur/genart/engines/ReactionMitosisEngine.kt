package fr.geoking.arthur.genart.engines

import android.graphics.RuntimeShader
import android.os.Build
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import fr.geoking.arthur.genart.GenartQuality
import fr.geoking.arthur.genart.TonalPalette
import fr.geoking.arthur.genart.loopedFbm
import fr.geoking.arthur.genart.phase01
import fr.geoking.arthur.genart.qualityCount
import fr.geoking.arthur.genart.seededRange
import fr.geoking.arthur.genart.seededUnit
import kotlin.math.PI
import kotlin.math.sin

private data class ReactionMitosisBlob(
    val x0: Float,
    val y0: Float,
    val radius: Float,
    val phaseOffset: Float,
    val colorIndex: Int,
)

@Composable
internal fun ReactionMitosisEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val count = qualityCount(quality, low = 5, medium = 8, high = 12)
    val blobs = remember(count) {
        List(count) { i ->
            ReactionMitosisBlob(
                x0 = seededUnit(i * 19 + 5),
                y0 = seededUnit(i * 31 + 9),
                radius = seededRange(i * 43 + 13, 0.05f, 0.16f),
                phaseOffset = seededRange(i * 53 + 17, 0f, 2f * PI.toFloat()),
                colorIndex = i,
            )
        }
    }
    val transition = rememberInfiniteTransition(label = "reactionmitosis")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((80000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "reactionmitosis_t",
    )
    val dim = if (isActive) 1f else 0.55f
    val c0 = TonalPalette.brightness(paletteColors.getOrElse(0) { Color(0xFFFF80C8) }, brightness * dim)
    val c1 = TonalPalette.brightness(paletteColors.getOrElse(1) { Color(0xFF80E0FF) }, brightness * dim)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val shader = remember { RuntimeShader(REACTION_SOFT_AGSL) }
        val brush = remember(shader) { ShaderBrush(shader) }
        Canvas(modifier = modifier) {
            shader.setFloatUniform("iResolution", size.width, size.height)
            shader.setFloatUniform("iTime", phase01(t) * 6.2831853f)
            shader.setFloatUniform("iEnergy", dim)
            shader.setFloatUniform("iColor0", c0.red, c0.green, c0.blue)
            shader.setFloatUniform("iColor1", c1.red, c1.green, c1.blue)
            drawRect(brush = brush)
        }
    } else {
        Canvas(modifier = modifier) {
            val w = size.width
            val h = size.height
            val time = phase01(t)
            drawRect(color = TonalPalette.brightness(Color(0xFF08060C), brightness * dim))
            blobs.forEach { blob ->
                val n = loopedFbm(time, radius = 1.25f, seedOffset = blob.colorIndex * 23)
                val x = (blob.x0 + 0.05f * (n * 2f - 1f) + 1f) % 1f * w
                val y = (blob.y0 + 0.04f * sin(time * 2f * PI.toFloat() + blob.phaseOffset) + 1f) % 1f * h
                val r = minOf(w, h) * blob.radius * (0.85f + 0.25f * n)
                val col = if (blob.colorIndex % 2 == 0) c0 else c1
                drawCircle(color = TonalPalette.withAlpha(col, 0.28f * dim), radius = r, center = Offset(x, y))
                drawCircle(color = TonalPalette.withAlpha(col, 0.45f * dim), radius = r * 0.45f, center = Offset(x, y))
            }
        }
    }
}

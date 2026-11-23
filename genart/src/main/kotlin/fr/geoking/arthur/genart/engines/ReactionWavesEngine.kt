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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.drawscope.Stroke
import fr.geoking.arthur.genart.GenartQuality
import fr.geoking.arthur.genart.TonalPalette
import fr.geoking.arthur.genart.loopedFbm
import fr.geoking.arthur.genart.phase01
import fr.geoking.arthur.genart.qualityCount
import fr.geoking.arthur.genart.seededRange
import kotlin.math.PI
import kotlin.math.sin

private data class ReactionWave(
    val yFraction: Float,
    val amplitude: Float,
    val frequency: Float,
    val phaseOffset: Float,
    val colorIndex: Int,
)

/** Soft, continuous reaction-diffusion bands that drift without a visible loop seam. */
@Composable
internal fun ReactionWavesEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val count = qualityCount(quality, low = 5, medium = 8, high = 12)
    val waves = remember(count) {
        List(count) { i ->
            ReactionWave(
                yFraction = (i + 0.5f) / count,
                amplitude = seededRange(i * 43 + 13, 0.012f, 0.035f),
                frequency = seededRange(i * 53 + 17, 1.6f, 3.4f),
                phaseOffset = seededRange(i * 67 + 19, 0f, 2f * PI.toFloat()),
                colorIndex = i,
            )
        }
    }
    val transition = rememberInfiniteTransition(label = "reactionwaves")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((80000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "reactionwaves_t",
    )
    val dim = if (isActive) 1f else 0.55f
    val c0 = TonalPalette.brightness(paletteColors.getOrElse(0) { Color(0xFF58C7B2) }, brightness * dim)
    val c1 = TonalPalette.brightness(paletteColors.getOrElse(1) { Color(0xFF609FE8) }, brightness * dim)
    val c2 = TonalPalette.brightness(Color(0xFFB5D96A), brightness * dim)

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val shader = remember { RuntimeShader(REACTION_WAVES_AGSL) }
        val brush = remember(shader) { ShaderBrush(shader) }
        Canvas(modifier = modifier) {
            shader.setFloatUniform("iResolution", size.width, size.height)
            shader.setFloatUniform("iTime", phase01(t) * 6.2831853f)
            shader.setFloatUniform("iEnergy", dim)
            shader.setFloatUniform("iColor0", c0.red, c0.green, c0.blue)
            shader.setFloatUniform("iColor1", c1.red, c1.green, c1.blue)
            shader.setFloatUniform("iColor2", c2.red, c2.green, c2.blue)
            drawRect(brush = brush)
        }
    } else {
        Canvas(modifier = modifier) {
            val w = size.width
            val h = size.height
            val time = phase01(t)
            val warp = loopedFbm(time, radius = 1.2f, octaves = 3, seedOffset = 251)
            val background = TonalPalette.brightness(Color(0xFF080B10), brightness * dim)
            drawRect(
                brush = Brush.verticalGradient(
                    listOf(background, TonalPalette.brightness(Color(0xFF0C1519), brightness * dim), background),
                ),
            )

            waves.forEach { wave ->
                val centerY = h * (wave.yFraction + wave.amplitude * sin(time * 2f * PI.toFloat() + wave.phaseOffset))
                val waveHeight = h * (0.055f + 0.018f * warp)
                val color = when (wave.colorIndex % 3) {
                    0 -> c0
                    1 -> c1
                    else -> c2
                }
                val path = androidx.compose.ui.graphics.Path().apply {
                    val steps = 32
                    for (step in 0..steps) {
                        val x = w * step / steps
                        val nx = step.toFloat() / steps
                        val localWarp = loopedFbm(time, radius = 1.1f + wave.frequency * 0.08f, octaves = 2, seedOffset = wave.colorIndex * 31 + step)
                        val y = centerY + sin(nx * wave.frequency * 2f * PI.toFloat() + time * 2f * PI.toFloat() + wave.phaseOffset) * waveHeight * (0.7f + localWarp * 0.5f)
                        if (step == 0) moveTo(x, y) else lineTo(x, y)
                    }
                }
                drawPath(path, TonalPalette.withAlpha(color, 0.12f * dim), style = Stroke(width = h * 0.045f))
                drawPath(path, TonalPalette.withAlpha(color, 0.22f * dim), style = Stroke(width = h * 0.014f))
                drawPath(path, TonalPalette.withAlpha(color, 0.54f * dim), style = Stroke(width = h * 0.0025f))
            }
        }
    }
}

private const val REACTION_WAVES_AGSL = """
uniform float2 iResolution;
uniform float iTime;
uniform float iEnergy;
uniform float3 iColor0;
uniform float3 iColor1;
uniform float3 iColor2;

half4 main(float2 fragCoord) {
    float2 uv = fragCoord / iResolution;
    float2 p = uv * 2.0 - 1.0;
    p.x *= iResolution.x / iResolution.y;
    float t = iTime;

    float warpA = sin(p.x * 3.0 + sin(p.y * 2.0 + t * 0.55) * 1.4 + t * 0.3);
    float warpB = cos(p.x * 1.8 - p.y * 2.4 + t * 0.42);
    float field = p.y * 5.6 + warpA * 0.52 + warpB * 0.24;
    float reaction = 0.5 + 0.5 * sin(field * 3.14159265 + sin(p.x * 4.0 + t * 0.5) * 0.8);
    float bands = smoothstep(0.44, 0.68, reaction);
    float ridges = 1.0 - smoothstep(0.0, 0.18, abs(reaction - 0.56));
    float pockets = smoothstep(0.58, 0.78, 0.5 + 0.5 * sin(field * 1.35 - t * 0.24 + warpB));
    float energy = clamp(iEnergy, 0.0, 1.2);
    float3 dark = float3(0.018, 0.026, 0.036);
    float3 tealBlue = mix(iColor0, iColor1, clamp(0.35 + 0.35 * warpB, 0.0, 1.0));
    float3 color = dark;
    color = mix(color, tealBlue, bands * 0.52 * energy);
    color = mix(color, iColor2, ridges * pockets * 0.42 * energy);
    color += tealBlue * ridges * 0.12 * energy;
    float vignette = smoothstep(1.55, 0.18, length(p * float2(0.72, 0.86)));
    color *= mix(0.54, 1.0, vignette);
    return half4(color, 1.0);
}
"""

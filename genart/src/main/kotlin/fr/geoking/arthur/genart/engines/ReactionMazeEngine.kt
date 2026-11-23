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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import fr.geoking.arthur.genart.GenartQuality
import fr.geoking.arthur.genart.TonalPalette
import fr.geoking.arthur.genart.loopedFbm
import fr.geoking.arthur.genart.phase01
import fr.geoking.arthur.genart.qualityCount
import fr.geoking.arthur.genart.seededRange
import kotlin.math.PI
import kotlin.math.sin

@Composable
internal fun ReactionMazeEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val count = qualityCount(quality, low = 5, medium = 8, high = 12)
    val transition = rememberInfiniteTransition(label = "reactionmaze")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((80000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "reactionmaze_t",
    )
    val dim = if (isActive) 1f else 0.55f
    val rose = TonalPalette.brightness(paletteColors.getOrElse(0) { Color(0xFFFF80C8) }, brightness * dim)
    val cyan = TonalPalette.brightness(paletteColors.getOrElse(1) { Color(0xFF80E0FF) }, brightness * dim)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val shader = remember { RuntimeShader(REACTION_MAZE_AGSL) }
        val brush = remember(shader) { ShaderBrush(shader) }
        Canvas(modifier = modifier) {
            shader.setFloatUniform("iResolution", size.width, size.height)
            shader.setFloatUniform("iTime", phase01(t) * 6.2831853f)
            shader.setFloatUniform("iEnergy", dim)
            shader.setFloatUniform("iColor0", rose.red, rose.green, rose.blue)
            shader.setFloatUniform("iColor1", cyan.red, cyan.green, cyan.blue)
            drawRect(brush = brush)
        }
    } else {
        Canvas(modifier = modifier) {
            val w = size.width
            val h = size.height
            val minDim = minOf(w, h)
            val time = phase01(t)
            drawRect(color = TonalPalette.brightness(Color(0xFF08060C), brightness * dim))
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(TonalPalette.withAlpha(cyan, 0.13f * dim), Color.Transparent),
                    center = Offset(w * 0.5f, h * 0.5f),
                    radius = minDim * 0.75f,
                ),
            )
            repeat(count) { lineIndex ->
                val row = lineIndex / 2
                val col = lineIndex % 2
                val band = (row + 1f) / (count / 2f + 1f)
                val sway = seededRange(lineIndex * 29 + 7, 0.05f, 0.12f)
                val phase = seededRange(lineIndex * 47 + 3, 0f, 2f * PI.toFloat())
                val y = h * band
                val path = Path().apply {
                    moveTo(-w * 0.06f, y)
                    for (segment in 0..6) {
                        val x0 = w * (segment / 6f - 0.06f)
                        val x1 = w * ((segment + 1f) / 6f - 0.06f)
                        val midX = (x0 + x1) * 0.5f
                        val wave = sin(time * 2f * PI.toFloat() + phase + segment * 0.9f) * h * sway * 0.22f
                        val offset = if (col == 0) -1f else 1f
                        quadraticTo(midX, y + wave + offset * minDim * 0.035f, x1, y + wave * 0.65f)
                    }
                }
                val color = if ((lineIndex + row) % 3 == 0) rose else cyan
                drawPath(
                    path = path,
                    color = TonalPalette.withAlpha(color, (0.12f + 0.05f * loopedFbm(time, radius = 1.2f, seedOffset = lineIndex * 13)) * dim),
                    style = Stroke(width = minDim * 0.022f, cap = StrokeCap.Round),
                )
                drawPath(
                    path = path,
                    color = TonalPalette.withAlpha(color, 0.28f * dim),
                    style = Stroke(width = minDim * 0.003f, cap = StrokeCap.Round),
                )
            }
        }
    }
}

private const val REACTION_MAZE_AGSL = """
uniform float2 iResolution;
uniform float iTime;
uniform float iEnergy;
uniform float3 iColor0;
uniform float3 iColor1;

float hash21(float2 p) {
    p = fract(p * float2(123.34, 456.21));
    p += dot(p, p + 45.32);
    return fract(p.x * p.y);
}

float valueNoise(float2 p) {
    float2 i = floor(p);
    float2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash21(i), hash21(i + float2(1.0, 0.0)), f.x),
               mix(hash21(i + float2(0.0, 1.0)), hash21(i + float2(1.0, 1.0)), f.x), f.y);
}

half4 main(float2 fragCoord) {
    float2 uv = fragCoord / iResolution;
    float2 p = (uv - 0.5) * float2(iResolution.x / iResolution.y, 1.0) * 7.0;
    float t = iTime;
    float n0 = valueNoise(p * 0.42 + float2(cos(t), sin(t)) * 0.22);
    float n1 = valueNoise(p * 0.68 + float2(sin(t), cos(t)) * 0.19);
    float2 q = p + (float2(n0, n1) - 0.5) * 0.82;
    float maze = sin(q.x * 2.15 + sin(q.y * 1.25 + t * 0.2) * 0.9)
               + cos(q.y * 2.05 + cos(q.x * 1.15 - t * 0.2) * 0.8);
    float cell = abs(sin(maze * 2.15 + valueNoise(q * 0.32) * 1.2));
    float line = 1.0 - smoothstep(0.025, 0.115, cell);
    float glow = 1.0 - smoothstep(0.08, 0.42, cell);
    float accent = smoothstep(0.45, 0.9, valueNoise(q * 0.55 + 8.1));
    float3 pigment = mix(iColor1, iColor0, accent);
    float vignette = smoothstep(1.05, 0.18, length((uv - 0.5) * float2(iResolution.x / iResolution.y, 1.0)));
    float3 base = float3(0.031, 0.024, 0.047) * (0.82 + 0.24 * n0);
    float3 col = base + pigment * (line * 0.40 + glow * 0.12) * iEnergy;
    col += iColor0 * glow * 0.11 * accent * iEnergy;
    col *= 0.78 + 0.22 * vignette;
    return half4(col, 1.0);
}
"""

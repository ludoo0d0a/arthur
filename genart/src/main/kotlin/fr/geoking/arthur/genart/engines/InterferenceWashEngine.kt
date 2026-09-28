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
import fr.geoking.arthur.genart.GenartQuality
import fr.geoking.arthur.genart.TonalPalette
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Procedural AGSL background: layered sine-wave interference.
 *
 * Inspired by the ProAndroidDev “animated background” AGSL use-case —
 * zero payload, infinite resolution, mood via color uniforms.
 * Falls back to a Compose Canvas approximation below API 33.
 */
@Composable
internal fun InterferenceWashEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "interference_wash")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween((90_000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "interference_t",
    )
    val c0 = TonalPalette.brightness(paletteColors.getOrElse(0) { Color(0xFF0EA5E9) }, brightness)
    val c1 = TonalPalette.brightness(paletteColors.getOrElse(1) { Color(0xFFA78BFA) }, brightness)
    val c2 = TonalPalette.brightness(paletteColors.getOrElse(2) { Color(0xFFF472B6) }, brightness)
    val energy = if (isActive) 1f else 0.55f
    val bands = when (quality) {
        GenartQuality.Low -> 3f
        GenartQuality.Medium -> 4f
        GenartQuality.High -> 5f
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val shader = remember { RuntimeShader(INTERFERENCE_AGSL) }
        val brush = remember(shader) { ShaderBrush(shader) }
        Canvas(modifier = modifier) {
            shader.setFloatUniform("iResolution", size.width, size.height)
            shader.setFloatUniform("iTime", t)
            shader.setFloatUniform("iEnergy", energy)
            shader.setFloatUniform("iBands", bands)
            shader.setFloatUniform("iColor0", c0.red, c0.green, c0.blue)
            shader.setFloatUniform("iColor1", c1.red, c1.green, c1.blue)
            shader.setFloatUniform("iColor2", c2.red, c2.green, c2.blue)
            drawRect(brush = brush)
        }
    } else {
        InterferenceWashCpu(
            t = t,
            energy = energy,
            bands = bands.toInt(),
            c0 = c0,
            c1 = c1,
            c2 = c2,
            modifier = modifier,
        )
    }
}

@Composable
private fun InterferenceWashCpu(
    t: Float,
    energy: Float,
    bands: Int,
    c0: Color,
    c1: Color,
    c2: Color,
    modifier: Modifier,
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(c0.copy(alpha = 0.35f), Color(0xFF020617)),
                center = Offset(w * 0.5f, h * 0.45f),
                radius = maxOf(w, h) * 0.85f,
            )
        )
        val cols = 48
        val rows = 64
        val cellW = w / cols
        val cellH = h / rows
        for (iy in 0 until rows) {
            for (ix in 0 until cols) {
                val u = ix / cols.toFloat()
                val v = iy / rows.toFloat()
                var field = 0f
                for (k in 0 until bands) {
                    val fx = 2.2f + k * 1.15f
                    val fy = 1.7f + k * 0.9f
                    val phase = t * (0.35f + k * 0.08f) + k * 1.7f
                    field += sin((u * fx + v * fy) * 2f * PI.toFloat() + phase).toFloat()
                    field += cos((u * fy - v * fx) * 2f * PI.toFloat() - phase * 0.7f).toFloat() * 0.65f
                }
                val n = ((field / (bands * 1.65f)) * 0.5f + 0.5f).coerceIn(0f, 1f)
                val mid = if (n < 0.5f) {
                    lerpColor(c0, c1, n * 2f)
                } else {
                    lerpColor(c1, c2, (n - 0.5f) * 2f)
                }
                drawRect(
                    color = mid.copy(alpha = (0.18f + 0.55f * n) * energy),
                    topLeft = Offset(ix * cellW, iy * cellH),
                    size = androidx.compose.ui.geometry.Size(cellW + 1f, cellH + 1f),
                )
            }
        }
    }
}

private fun lerpColor(a: Color, b: Color, t: Float): Color {
    val u = t.coerceIn(0f, 1f)
    return Color(
        red = a.red + (b.red - a.red) * u,
        green = a.green + (b.green - a.green) * u,
        blue = a.blue + (b.blue - a.blue) * u,
        alpha = 1f,
    )
}

internal const val INTERFERENCE_AGSL = """
    uniform float2 iResolution;
    uniform float iTime;
    uniform float iEnergy;
    uniform float iBands;
    uniform float3 iColor0;
    uniform float3 iColor1;
    uniform float3 iColor2;

    half4 main(float2 fragCoord) {
        float2 uv = fragCoord / iResolution;
        float field = 0.0;
        float bands = clamp(iBands, 2.0, 6.0);
        for (int k = 0; k < 6; k++) {
            if (float(k) >= bands) break;
            float fk = float(k);
            float fx = 2.2 + fk * 1.15;
            float fy = 1.7 + fk * 0.9;
            float phase = iTime * (0.35 + fk * 0.08) + fk * 1.7;
            field += sin((uv.x * fx + uv.y * fy) * 6.2831853 + phase);
            field += cos((uv.x * fy - uv.y * fx) * 6.2831853 - phase * 0.7) * 0.65;
        }
        float n = clamp(field / (bands * 1.65) * 0.5 + 0.5, 0.0, 1.0);
        float3 mid = n < 0.5
            ? mix(iColor0, iColor1, n * 2.0)
            : mix(iColor1, iColor2, (n - 0.5) * 2.0);
        float vignette = smoothstep(1.15, 0.25, length(uv - float2(0.5, 0.48)));
        float3 deep = float3(0.008, 0.024, 0.086);
        float3 col = mix(deep, mid, (0.35 + 0.65 * n) * iEnergy * vignette);
        return half4(half3(col), 1.0);
    }
"""

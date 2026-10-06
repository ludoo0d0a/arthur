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
import fr.geoking.arthur.genart.phase01
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Slow Newton-ring / soap-film iridescence (AGSL + Canvas chromatic rings). */
@Composable
internal fun SoapFilmEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "soap_film")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween((80000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "soap_film_t",
    )
    val c0 = TonalPalette.brightness(paletteColors.getOrElse(0) { Color(0xFF67E8F9) }, brightness)
    val c1 = TonalPalette.brightness(paletteColors.getOrElse(1) { Color(0xFFC084FC) }, brightness)
    val c2 = TonalPalette.brightness(paletteColors.getOrElse(2) { Color(0xFFFDA4AF) }, brightness)
    val energy = if (isActive) 1f else 0.55f
    val rings = when (quality) {
        GenartQuality.Low -> 8f
        GenartQuality.Medium -> 12f
        GenartQuality.High -> 16f
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val shader = remember { RuntimeShader(SOAP_FILM_AGSL) }
        val brush = remember(shader) { ShaderBrush(shader) }
        Canvas(modifier = modifier) {
            shader.setFloatUniform("iResolution", size.width, size.height)
            shader.setFloatUniform("iTime", t)
            shader.setFloatUniform("iEnergy", energy)
            shader.setFloatUniform("iRings", rings)
            shader.setFloatUniform("iColor0", c0.red, c0.green, c0.blue)
            shader.setFloatUniform("iColor1", c1.red, c1.green, c1.blue)
            shader.setFloatUniform("iColor2", c2.red, c2.green, c2.blue)
            drawRect(brush = brush)
        }
    } else {
        SoapFilmCpu(
            t = phase01(t / 1000f),
            energy = energy,
            ringCount = rings.toInt(),
            c0 = c0,
            c1 = c1,
            c2 = c2,
            quality = quality,
            modifier = modifier,
        )
    }
}

@Composable
private fun SoapFilmCpu(
    t: Float,
    energy: Float,
    ringCount: Int,
    c0: Color,
    c1: Color,
    c2: Color,
    quality: GenartQuality,
    modifier: Modifier,
) {
    val strokeMul = when (quality) {
        GenartQuality.Low -> 0.8f
        GenartQuality.Medium -> 1f
        GenartQuality.High -> 1.2f
    }
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val minDim = minOf(w, h)
        val cx = w * 0.5f
        val cy = h * 0.48f
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF101828), Color(0xFF060810), Color(0xFF020408)),
                center = Offset(cx, cy),
                radius = minDim * 0.95f,
            ),
        )
        val time = t * 2f * PI.toFloat()
        val colors = listOf(c0, c1, c2)
        for (i in 0 until ringCount) {
            val frac = (i + 1f) / (ringCount + 1f)
            val radius = minDim * (0.08f + frac * 0.42f)
            val phase = time * 0.4f + i * 0.45f
            val mixT = (0.5f + 0.5f * sin(phase)).coerceIn(0f, 1f)
            val tint = if (mixT < 0.5f) {
                TonalPalette.mix(colors[0], colors[1], mixT * 2f)
            } else {
                TonalPalette.mix(colors[1], colors[2], (mixT - 0.5f) * 2f)
            }
            val alpha = (0.12f + 0.2f * (0.5f + 0.5f * cos(phase * 0.7f))) * energy
            drawCircle(
                color = TonalPalette.withAlpha(tint, alpha),
                radius = radius,
                center = Offset(cx + 0.01f * minDim * sin(phase * 0.3f), cy),
                style = Stroke(width = minDim * 0.012f * strokeMul),
            )
            // Chromatic offset ghost rings
            drawCircle(
                color = TonalPalette.withAlpha(colors[i % 3], alpha * 0.45f),
                radius = radius + minDim * 0.006f,
                center = Offset(cx, cy),
                style = Stroke(width = minDim * 0.006f),
            )
        }
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.White.copy(alpha = 0.12f * energy), Color.Transparent),
                center = Offset(cx - minDim * 0.08f, cy - minDim * 0.1f),
                radius = minDim * 0.2f,
            ),
            radius = minDim * 0.2f,
            center = Offset(cx - minDim * 0.08f, cy - minDim * 0.1f),
        )
    }
}

internal const val SOAP_FILM_AGSL = """
    uniform float2 iResolution;
    uniform float iTime;
    uniform float iEnergy;
    uniform float iRings;
    uniform float3 iColor0;
    uniform float3 iColor1;
    uniform float3 iColor2;

    half4 main(float2 fragCoord) {
        float2 uv = fragCoord / iResolution;
        float2 p = (fragCoord - 0.5 * iResolution) / iResolution.y;
        float r = length(p);
        float t = iTime * 0.035;
        float rings = clamp(iRings, 6.0, 18.0);
        float wave = sin(r * rings * 6.2831853 - t * 2.0) * 0.5 + 0.5;
        float wave2 = sin(r * rings * 4.1 - t * 1.3 + 1.7) * 0.5 + 0.5;
        float n = clamp(0.55 * wave + 0.45 * wave2, 0.0, 1.0);
        float3 mid = n < 0.5
            ? mix(iColor0, iColor1, n * 2.0)
            : mix(iColor1, iColor2, (n - 0.5) * 2.0);
        float film = smoothstep(0.72, 0.08, r);
        float3 deep = float3(0.02, 0.03, 0.06);
        float highlight = pow(smoothstep(0.22, 0.0, length(p - float2(-0.12, -0.15))), 2.0) * 0.35;
        float3 col = mix(deep, mid, (0.25 + 0.75 * n) * film * iEnergy);
        col += float3(highlight) * iEnergy;
        float vignette = smoothstep(1.1, 0.35, length(uv - float2(0.5, 0.48)));
        col *= vignette;
        return half4(half3(col), 1.0);
    }
"""

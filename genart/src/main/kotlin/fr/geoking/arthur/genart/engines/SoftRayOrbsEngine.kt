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
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.unit.dp
import fr.geoking.arthur.genart.GenartQuality
import fr.geoking.arthur.genart.TonalPalette
import fr.geoking.arthur.genart.phase01
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Soft raymarch orbs (AGSL API 33+) with Canvas terminator-disc fallback.
 */
@Composable
internal fun SoftRayOrbsEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "soft_ray_orbs")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween((70000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "soft_ray_orbs_t",
    )
    val c0 = TonalPalette.brightness(paletteColors.getOrElse(0) { Color(0xFF7DD3FC) }, brightness)
    val c1 = TonalPalette.brightness(paletteColors.getOrElse(1) { Color(0xFFC4B5FD) }, brightness)
    val c2 = TonalPalette.brightness(paletteColors.getOrElse(2) { Color(0xFFF9A8D4) }, brightness)
    val energy = if (isActive) 1f else 0.55f
    val orbCount = when (quality) {
        GenartQuality.Low -> 2f
        GenartQuality.Medium -> 3f
        GenartQuality.High -> 3f
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val shader = remember { RuntimeShader(SOFT_RAY_ORBS_AGSL) }
        val brush = remember(shader) { ShaderBrush(shader) }
        Canvas(modifier = modifier) {
            shader.setFloatUniform("iResolution", size.width, size.height)
            shader.setFloatUniform("iTime", t)
            shader.setFloatUniform("iEnergy", energy)
            shader.setFloatUniform("iOrbs", orbCount)
            shader.setFloatUniform("iColor0", c0.red, c0.green, c0.blue)
            shader.setFloatUniform("iColor1", c1.red, c1.green, c1.blue)
            shader.setFloatUniform("iColor2", c2.red, c2.green, c2.blue)
            drawRect(brush = brush)
        }
    } else {
        SoftRayOrbsCpu(
            t = phase01(t / 1000f),
            energy = energy,
            orbCount = orbCount.toInt(),
            c0 = c0,
            c1 = c1,
            c2 = c2,
            quality = quality,
            modifier = modifier,
        )
    }
}

@Composable
private fun SoftRayOrbsCpu(
    t: Float,
    energy: Float,
    orbCount: Int,
    c0: Color,
    c1: Color,
    c2: Color,
    quality: GenartQuality,
    modifier: Modifier,
) {
    val glowMul = when (quality) {
        GenartQuality.Low -> 0.8f
        GenartQuality.Medium -> 1f
        GenartQuality.High -> 1.15f
    }
    val colors = listOf(c0, c1, c2)
    Box(modifier = modifier) {
        Canvas(Modifier.fillMaxSize()) {
            drawRect(color = Color(0xFF050814))
        }
        Canvas(Modifier.fillMaxSize().blur(10.dp)) {
            val w = size.width
            val h = size.height
            val minDim = minOf(w, h)
            val angle = t * 2f * PI.toFloat()
            for (i in 0 until orbCount.coerceAtLeast(2)) {
                val a = angle * (0.35f + i * 0.12f) + i * 2.1f
                val orbit = minDim * (0.18f + i * 0.05f)
                val cx = w * 0.5f + cos(a) * orbit
                val cy = h * 0.48f + sin(a * 0.9f) * orbit * 0.55f
                val r = minDim * (0.14f - i * 0.02f) * glowMul
                val tint = colors[i % colors.size]
                drawCircle(
                    brush = Brush.radialGradient(
                        0f to TonalPalette.withAlpha(Color.White, 0.55f * energy),
                        0.35f to TonalPalette.withAlpha(tint, 0.45f * energy),
                        0.75f to TonalPalette.withAlpha(tint, 0.12f * energy),
                        1f to Color.Transparent,
                        center = Offset(cx - r * 0.25f, cy - r * 0.3f),
                        radius = r * 1.2f,
                    ),
                    radius = r * 1.2f,
                    center = Offset(cx, cy),
                )
            }
        }
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val minDim = minOf(w, h)
            val angle = t * 2f * PI.toFloat()
            for (i in 0 until orbCount.coerceAtLeast(2)) {
                val a = angle * (0.35f + i * 0.12f) + i * 2.1f
                val orbit = minDim * (0.18f + i * 0.05f)
                val cx = w * 0.5f + cos(a) * orbit
                val cy = h * 0.48f + sin(a * 0.9f) * orbit * 0.55f
                val r = minDim * (0.12f - i * 0.018f)
                val tint = colors[i % colors.size]
                drawCircle(
                    brush = Brush.radialGradient(
                        0f to TonalPalette.withAlpha(Color.White, 0.7f * energy),
                        0.4f to TonalPalette.withAlpha(tint, 0.55f * energy),
                        0.85f to TonalPalette.withAlpha(Color(0xFF101828), 0.85f * energy),
                        1f to TonalPalette.withAlpha(Color(0xFF050814), energy),
                        center = Offset(cx - r * 0.3f, cy - r * 0.35f),
                        radius = r,
                    ),
                    radius = r,
                    center = Offset(cx, cy),
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.White.copy(alpha = 0.65f * energy), Color.Transparent),
                        center = Offset(cx - r * 0.35f, cy - r * 0.4f),
                        radius = r * 0.22f,
                    ),
                    radius = r * 0.22f,
                    center = Offset(cx - r * 0.35f, cy - r * 0.4f),
                )
            }
        }
    }
}

internal const val SOFT_RAY_ORBS_AGSL = """
    uniform float2 iResolution;
    uniform float iTime;
    uniform float iEnergy;
    uniform float iOrbs;
    uniform float3 iColor0;
    uniform float3 iColor1;
    uniform float3 iColor2;

    float sdSphere(float3 p, float r) { return length(p) - r; }

    float mapScene(float3 p, float t) {
        float d = 1e5;
        float n = clamp(iOrbs, 2.0, 3.0);
        for (int i = 0; i < 3; i++) {
            if (float(i) >= n) break;
            float fi = float(i);
            float a = t * (0.35 + fi * 0.12) + fi * 2.1;
            float3 c = float3(cos(a) * (0.55 + fi * 0.12), sin(a * 0.9) * 0.32, sin(a * 0.7) * 0.2);
            d = min(d, sdSphere(p - c, 0.32 - fi * 0.04));
        }
        return d;
    }

    half4 main(float2 fragCoord) {
        float2 uv = (fragCoord - 0.5 * iResolution) / iResolution.y;
        float t = iTime * 0.04;
        float3 ro = float3(0.0, 0.0, 2.6);
        float3 rd = normalize(float3(uv, -1.4));
        float dist = 0.0;
        float hit = 0.0;
        float3 p = ro;
        for (int i = 0; i < 48; i++) {
            float d = mapScene(p, t);
            if (d < 0.002) { hit = 1.0; break; }
            dist += d;
            p += rd * d;
            if (dist > 8.0) break;
        }
        float3 deep = float3(0.02, 0.03, 0.08);
        float3 col = deep;
        if (hit > 0.5) {
            float e = 0.004;
            float3 nrm = normalize(float3(
                mapScene(p + float3(e,0,0), t) - mapScene(p - float3(e,0,0), t),
                mapScene(p + float3(0,e,0), t) - mapScene(p - float3(0,e,0), t),
                mapScene(p + float3(0,0,e), t) - mapScene(p - float3(0,0,e), t)
            ));
            float3 ld = normalize(float3(-0.4, 0.7, 0.5));
            float diff = clamp(dot(nrm, ld), 0.0, 1.0);
            float rim = pow(1.0 - clamp(dot(nrm, -rd), 0.0, 1.0), 2.2);
            float spec = pow(clamp(dot(reflect(-ld, nrm), -rd), 0.0, 1.0), 24.0);
            float3 tint = mix(iColor0, mix(iColor1, iColor2, 0.5 + 0.5 * nrm.x), 0.5 + 0.5 * nrm.y);
            col = tint * (0.18 + 0.55 * diff) + float3(1.0) * spec * 0.55 + tint * rim * 0.45;
        }
        float fog = 1.0 - exp(-dist * 0.18);
        col = mix(col, deep, fog * 0.65);
        col *= iEnergy;
        return half4(half3(col), 1.0);
    }
"""

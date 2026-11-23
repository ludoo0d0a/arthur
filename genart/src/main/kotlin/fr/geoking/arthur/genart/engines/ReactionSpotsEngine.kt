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
import fr.geoking.arthur.genart.loopedFbm
import fr.geoking.arthur.genart.phase01
import fr.geoking.arthur.genart.qualityCount
import fr.geoking.arthur.genart.seededRange
import fr.geoking.arthur.genart.seededUnit
import kotlin.math.PI
import kotlin.math.sin

private data class ReactionSpotsBlob(
    val x0: Float,
    val y0: Float,
    val radius: Float,
    val phaseOffset: Float,
    val colorIndex: Int,
)

@Composable
internal fun ReactionSpotsEngine(
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
            ReactionSpotsBlob(
                x0 = seededUnit(i * 19 + 5),
                y0 = seededUnit(i * 31 + 9),
                radius = seededRange(i * 43 + 13, 0.05f, 0.16f),
                phaseOffset = seededRange(i * 53 + 17, 0f, 2f * PI.toFloat()),
                colorIndex = i,
            )
        }
    }
    val transition = rememberInfiniteTransition(label = "reactionspots")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((80000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "reactionspots_t",
    )
    val dim = if (isActive) 1f else 0.55f
    val c0 = TonalPalette.brightness(paletteColors.getOrElse(0) { Color(0xFFFF80C8) }, brightness * dim)
    val c1 = TonalPalette.brightness(paletteColors.getOrElse(1) { Color(0xFF80E0FF) }, brightness * dim)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val shader = remember { RuntimeShader(REACTION_SPOTS_AGSL) }
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
            // Low-contrast wash underneath the spots keeps their edges soft and cellular.
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        TonalPalette.withAlpha(c1, 0.10f * dim),
                        Color.Transparent,
                    ),
                    center = Offset(w * 0.52f, h * 0.48f),
                    radius = minOf(w, h) * 0.72f,
                ),
            )
            blobs.forEach { blob ->
                val n = loopedFbm(time, radius = 1.25f, seedOffset = blob.colorIndex * 23)
                val x = (blob.x0 + 0.012f * (n * 2f - 1f) + 1f) % 1f * w
                val y = (blob.y0 + 0.01f * sin(time * 2f * PI.toFloat() + blob.phaseOffset) + 1f) % 1f * h
                val r = minOf(w, h) * blob.radius * (0.9f + 0.12f * n)
                val col = if (blob.colorIndex % 2 == 0) c0 else c1
                drawCircle(
                    brush = Brush.radialGradient(
                        0f to TonalPalette.withAlpha(col, 0.30f * dim),
                        0.70f to TonalPalette.withAlpha(col, 0.20f * dim),
                        0.92f to TonalPalette.withAlpha(col, 0.015f * dim),
                        1f to Color.Transparent,
                        center = Offset(x, y),
                        radius = r,
                    ),
                    radius = r,
                    center = Offset(x, y),
                )
                drawCircle(
                    color = TonalPalette.withAlpha(col, 0.10f * dim),
                    radius = r * 0.70f,
                    center = Offset(x, y),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = r * 0.08f),
                )
            }
        }
    }
}

private const val REACTION_SPOTS_AGSL = """
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
    float a = hash21(i);
    float b = hash21(i + float2(1.0, 0.0));
    float c = hash21(i + float2(0.0, 1.0));
    float d = hash21(i + float2(1.0, 1.0));
    return mix(mix(a, b, f.x), mix(c, d, f.x), f.y);
}

half4 main(float2 fragCoord) {
    float2 uv = fragCoord / iResolution;
    float2 p = uv * float2(iResolution.x / iResolution.y, 1.0) * 8.0;
    float t = iTime;
    float n0 = valueNoise(p * 0.58 + float2(cos(t), sin(t)) * 0.28);
    float n1 = valueNoise(p * 1.07 + float2(sin(t * 0.7), cos(t * 0.7)) * 0.18);
    float2 warped = p + (float2(n0, n1) - 0.5) * 0.78;

    float minDist = 10.0;
    float siteHue = 0.0;
    float2 cell = floor(warped);
    for (int y = -1; y <= 1; y++) {
        for (int x = -1; x <= 1; x++) {
            float2 neighbor = cell + float2(float(x), float(y));
            float h = hash21(neighbor);
            float2 site = neighbor + 0.18 + 0.64 * float2(h, hash21(neighbor + 17.7));
            float d = length(warped - site);
            if (d < minDist) {
                minDist = d;
                siteHue = hash21(neighbor + 43.1);
            }
        }
    }

    float radius = 0.29 + 0.10 * valueNoise(warped * 0.42 + 2.3);
    float edgeNoise = (valueNoise(warped * 2.1 + float2(t * 0.2, -t * 0.15)) - 0.5) * 0.10;
    float spot = 1.0 - smoothstep(radius - 0.035, radius + 0.035, minDist + edgeNoise);
    float rim = 1.0 - smoothstep(0.02, 0.07, abs(minDist - radius - edgeNoise));
    float3 pigment = mix(iColor0, iColor1, smoothstep(0.25, 0.75, siteHue));
    float3 base = float3(0.031, 0.024, 0.047) * (0.75 + 0.35 * n0);
    float3 col = mix(base, pigment * (0.24 + 0.30 * n1), clamp(spot * iEnergy, 0.0, 1.0));
    col += pigment * rim * 0.07 * iEnergy;
    return half4(col, 1.0);
}
"""

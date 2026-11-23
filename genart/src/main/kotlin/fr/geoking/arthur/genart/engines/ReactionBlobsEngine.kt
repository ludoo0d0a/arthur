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
import kotlin.math.cos
import kotlin.math.sin

private data class ReactionBlobsBlob(
    val x0: Float,
    val y0: Float,
    val radius: Float,
    val phaseOffset: Float,
    val orbit: Float,
    val colorIndex: Int,
)

/** Soft Ambient Reaction Soft Blobs — merging pastel metaballs on a dark bed. */
@Composable
internal fun ReactionBlobsEngine(
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
            ReactionBlobsBlob(
                x0 = seededUnit(i * 19 + 5),
                y0 = seededUnit(i * 31 + 9),
                radius = seededRange(i * 43 + 13, 0.08f, 0.20f),
                phaseOffset = seededRange(i * 53 + 17, 0f, 2f * PI.toFloat()),
                orbit = seededRange(i * 61 + 21, 0.02f, 0.07f),
                colorIndex = i,
            )
        }
    }
    val transition = rememberInfiniteTransition(label = "reactionblobs")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((80000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "reactionblobs_t",
    )
    val dim = if (isActive) 1f else 0.55f
    val c0 = TonalPalette.brightness(paletteColors.getOrElse(0) { Color(0xFFFF80C8) }, brightness * dim)
    val c1 = TonalPalette.brightness(paletteColors.getOrElse(1) { Color(0xFF80E0FF) }, brightness * dim)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val shader = remember { RuntimeShader(REACTION_BLOBS_AGSL) }
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
            val minDim = minOf(w, h)
            val time = phase01(t)
            drawRect(color = TonalPalette.brightness(Color(0xFF08060C), brightness * dim))
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(TonalPalette.withAlpha(c1, 0.12f * dim), Color.Transparent),
                    center = Offset(w * 0.5f, h * 0.52f),
                    radius = minDim * 0.7f,
                ),
            )
            blobs.forEach { blob ->
                val n = loopedFbm(time, radius = 1.25f, seedOffset = blob.colorIndex * 23)
                val angle = time * 2f * PI.toFloat() + blob.phaseOffset
                val x = (blob.x0 + blob.orbit * cos(angle) + 0.03f * (n * 2f - 1f) + 1f) % 1f * w
                val y = (blob.y0 + blob.orbit * sin(angle * 0.85f) + 0.025f * sin(angle) + 1f) % 1f * h
                val r = minDim * blob.radius * (0.88f + 0.22f * n)
                val col = if (blob.colorIndex % 2 == 0) c0 else c1
                drawCircle(
                    brush = Brush.radialGradient(
                        0f to TonalPalette.withAlpha(col, 0.38f * dim),
                        0.45f to TonalPalette.withAlpha(col, 0.22f * dim),
                        0.82f to TonalPalette.withAlpha(col, 0.06f * dim),
                        1f to Color.Transparent,
                        center = Offset(x, y),
                        radius = r,
                    ),
                    radius = r,
                    center = Offset(x, y),
                )
                drawCircle(
                    color = TonalPalette.withAlpha(col, 0.18f * dim),
                    radius = r * 0.42f,
                    center = Offset(x, y),
                )
            }
        }
    }
}

private const val REACTION_BLOBS_AGSL = """
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
    float2 p = (uv - 0.5) * float2(iResolution.x / iResolution.y, 1.0);
    float t = iTime;
    float field = 0.0;
    float tint = 0.0;
    for (int i = 0; i < 7; i++) {
        float fi = float(i);
        float2 c = float2(
            sin(t * 0.18 + fi * 1.7) * 0.42 + cos(t * 0.11 + fi) * 0.12,
            cos(t * 0.15 + fi * 1.3) * 0.38 + sin(t * 0.09 + fi * 0.7) * 0.10
        );
        float r = 0.18 + 0.07 * hash21(float2(fi + 2.1, fi * 0.37));
        float d = length(p - c);
        float contrib = r * r / max(d * d + 0.0008, 0.0008);
        field += contrib;
        tint += contrib * (fi < 3.5 ? 0.0 : 1.0);
    }
    float soft = smoothstep(1.05, 2.35, field);
    float rim = smoothstep(0.9, 1.35, field) - soft;
    float n = valueNoise(p * 3.2 + float2(cos(t), sin(t)) * 0.25);
    float3 pigment = mix(iColor0, iColor1, clamp(tint / max(field, 0.001), 0.0, 1.0));
    float3 base = float3(0.031, 0.024, 0.047) * (0.78 + 0.28 * n);
    float3 col = mix(base, pigment * (0.28 + 0.22 * n), soft * iEnergy);
    col += pigment * rim * 0.16 * iEnergy;
    float vignette = smoothstep(1.1, 0.2, length(p));
    col *= 0.82 + 0.18 * vignette;
    return half4(col, 1.0);
}
"""

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

private data class MitosisCell(
    val x: Float,
    val y: Float,
    val radius: Float,
    val angle: Float,
    val phase: Float,
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
    val cells = remember(count) {
        List(count) { i ->
            MitosisCell(
                x = seededRange(i * 19 + 5, 0.12f, 0.88f),
                y = seededRange(i * 31 + 9, 0.12f, 0.88f),
                radius = seededRange(i * 43 + 13, 0.045f, 0.085f),
                angle = seededRange(i * 53 + 17, 0f, 2f * PI.toFloat()),
                phase = seededRange(i * 67 + 23, 0f, 2f * PI.toFloat()),
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
    val phase = phase01(t)

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val shader = remember { RuntimeShader(REACTION_MITOSIS_AGSL) }
        val brush = remember(shader) { ShaderBrush(shader) }
        Canvas(modifier = modifier) {
            shader.setFloatUniform("iResolution", size.width, size.height)
            shader.setFloatUniform("iTime", phase * 2f * PI.toFloat())
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
            drawRect(color = TonalPalette.brightness(Color(0xFF08060C), brightness * dim))
            cells.forEach { cell ->
                val localPhase = phase * 2f * PI.toFloat() + cell.phase
                val motion = loopedFbm(phase, radius = 1.15f, octaves = 2, seedOffset = cell.colorIndex * 29)
                val cx = cell.x * w + sin(localPhase) * minDim * 0.009f
                val cy = cell.y * h + cos(localPhase * 0.7f) * minDim * 0.008f
                val axis = cell.angle + 0.12f * sin(localPhase)
                val separation = cell.radius * minDim * (0.12f + 0.56f * (0.5f + 0.5f * sin(localPhase)))
                val radius = cell.radius * minDim * (0.82f + 0.08f * motion)
                val dx = cos(axis) * separation
                val dy = sin(axis) * separation
                val color = if (cell.colorIndex % 2 == 0) c0 else c1
                val alpha = (0.42f + 0.08f * motion) * dim
                val glowRadius = radius * 2.4f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(TonalPalette.withAlpha(color, 0.15f * dim), Color.Transparent),
                        center = Offset(cx, cy),
                        radius = glowRadius,
                    ),
                    radius = glowRadius,
                    center = Offset(cx, cy),
                )
                drawCircle(color = TonalPalette.withAlpha(color, alpha), radius = radius, center = Offset(cx - dx, cy - dy))
                drawCircle(color = TonalPalette.withAlpha(color, alpha), radius = radius, center = Offset(cx + dx, cy + dy))
                // The narrow dark furrow makes the paired cells read as a division seam.
                drawCircle(
                    color = TonalPalette.withAlpha(Color(0xFF08060C), 0.72f * dim),
                    radius = radius * 0.19f,
                    center = Offset(cx, cy),
                )
                drawCircle(color = TonalPalette.withAlpha(Color.White, 0.42f * dim), radius = radius * 0.055f, center = Offset(cx, cy))
            }
        }
    }
}

private const val REACTION_MITOSIS_AGSL = """
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
    float2 p = uv * float2(iResolution.x / iResolution.y, 1.0) * 6.0;
    float t = iTime;
    float n0 = valueNoise(p * 0.56 + float2(cos(t), sin(t)) * 0.16);
    float n1 = valueNoise(p * 1.1 + float2(sin(t * 0.8), cos(t * 0.8)) * 0.12);
    float2 warped = p + (float2(n0, n1) - 0.5) * 0.48;

    float nearest = 10.0;
    float siteId = 0.0;
    float2 cell = floor(warped);
    for (int y = -1; y <= 1; y++) {
        for (int x = -1; x <= 1; x++) {
            float2 neighbor = cell + float2(float(x), float(y));
            float h = hash21(neighbor);
            float2 site = neighbor + 0.22 + 0.56 * float2(h, hash21(neighbor + 17.7));
            float d = length(warped - site);
            if (d < nearest) {
                nearest = d;
                siteId = hash21(neighbor + 43.1);
            }
        }
    }

    float2 local = warped - (cell + 0.5);
    float angle = 6.2831853 * hash21(cell + 7.9) + 0.10 * sin(t + siteId * 6.28);
    float2 axis = float2(cos(angle), sin(angle));
    float separation = 0.07 + 0.12 * (0.5 + 0.5 * sin(t + siteId * 6.28));
    float2 a = local - axis * separation;
    float2 b = local + axis * separation;
    float radius = 0.31 + 0.025 * sin(t + siteId * 4.0) + 0.018 * (n1 - 0.5);
    float dCell = min(length(a * float2(1.0, 0.88)), length(b * float2(1.0, 0.88)));
    float edgeNoise = (valueNoise(warped * 2.0 + float2(t * 0.1, -t * 0.08)) - 0.5) * 0.025;
    float body = 1.0 - smoothstep(radius - 0.035, radius + 0.035, dCell + edgeNoise);
    float seam = 1.0 - smoothstep(0.012, 0.045, abs(dot(local, axis)));
    seam *= 1.0 - smoothstep(0.035, 0.14, abs(dot(local, float2(-axis.y, axis.x))));
    float rim = 1.0 - smoothstep(0.018, 0.055, abs(dCell - radius - edgeNoise));
    float3 pigment = mix(iColor0, iColor1, smoothstep(0.25, 0.75, siteId));
    float3 base = float3(0.031, 0.024, 0.047) * (0.78 + 0.30 * n0);
    float3 col = mix(base, pigment * (0.22 + 0.22 * n1), clamp(body * iEnergy, 0.0, 1.0));
    col = mix(col, base * 0.55, seam * body * 0.65);
    col += pigment * rim * 0.065 * iEnergy;
    float glow = exp(-nearest * nearest * 1.8) * (0.05 + 0.04 * n1);
    col += mix(iColor0, iColor1, 0.5) * glow * iEnergy;
    return half4(col, 1.0);
}
"""

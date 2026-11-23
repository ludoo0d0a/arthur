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
import androidx.compose.ui.graphics.drawscope.Stroke
import fr.geoking.arthur.genart.GenartQuality
import fr.geoking.arthur.genart.TonalPalette
import fr.geoking.arthur.genart.loopedFbm
import fr.geoking.arthur.genart.phase01
import fr.geoking.arthur.genart.qualityCount
import fr.geoking.arthur.genart.seededRange
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private data class PhysarumNodeNode(
    val x0: Float,
    val y0: Float,
    val radius: Float,
    val phaseOffset: Float,
    val colorIndex: Int,
    val clusterIndex: Int,
)


@Composable
internal fun PhysarumNodeEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val count = qualityCount(quality, low = 8, medium = 14, high = 22)
    val nodes = remember(count) {
        val centers = listOf(Offset(0.25f, 0.30f), Offset(0.72f, 0.28f), Offset(0.34f, 0.72f), Offset(0.73f, 0.70f))
        List(count) { i ->
            val cluster = i % centers.size
            val center = centers[cluster]
            PhysarumNodeNode(
                x0 = (center.x + seededRange(i * 17 + 3, -0.13f, 0.13f)).coerceIn(0.06f, 0.94f),
                y0 = (center.y + seededRange(i * 29 + 7, -0.12f, 0.12f)).coerceIn(0.06f, 0.94f),
                radius = seededRange(i * 41 + 11, 0.009f, 0.019f),
                phaseOffset = seededRange(i * 53 + 13, 0f, 2f * PI.toFloat()),
                colorIndex = i,
                clusterIndex = cluster,
            )
        }
    }
    val transition = rememberInfiniteTransition(label = "physarumnode")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((90000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "physarumnode_t",
    )
    val dim = if (isActive) 1f else 0.55f
    val c0 = TonalPalette.brightness(paletteColors.getOrElse(0) { Color(0xFF7CFFB2) }, brightness * dim)
    val dark = TonalPalette.brightness(Color(0xFF05080A), brightness * dim)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val shader = remember { RuntimeShader(PHYSARUM_SOFT_AGSL) }
        val brush = remember(shader) { ShaderBrush(shader) }
        Canvas(modifier = modifier) {
            shader.setFloatUniform("iResolution", size.width, size.height)
            shader.setFloatUniform("iTime", phase01(t) * 6.2831853f)
            shader.setFloatUniform("iEnergy", dim)
            shader.setFloatUniform("iColor0", c0.red, c0.green, c0.blue)
            drawRect(brush = brush)
        }
    } else {
        Canvas(modifier = modifier) {
            val w = size.width
            val h = size.height
            val minDim = minOf(w, h)
            val time = phase01(t)
            drawRect(color = dark)
            val centers = listOf(Offset(w * 0.25f, h * 0.30f), Offset(w * 0.72f, h * 0.28f), Offset(w * 0.34f, h * 0.72f), Offset(w * 0.73f, h * 0.70f))
            centers.forEach { center ->
                val glowRadius = minDim * 0.24f
                drawCircle(
                    brush = Brush.radialGradient(
                        0f to TonalPalette.withAlpha(c0, 0.10f * dim),
                        0.48f to TonalPalette.withAlpha(c0, 0.045f * dim),
                        1f to Color.Transparent,
                        center = center,
                        radius = glowRadius,
                    ),
                    radius = glowRadius,
                    center = center,
                )
            }
            // Each compact nest is a small branching network with a few cross-links.
            for (i in nodes.indices) {
                val a = nodes[i]
                val bIndex = nodes.indices
                    .drop(i + 1)
                    .firstOrNull { nodes[it].clusterIndex == a.clusterIndex }
                    ?: continue
                val b = nodes[bIndex]
                val na = loopedFbm(time, radius = 1.3f, seedOffset = a.colorIndex * 17)
                val nb = loopedFbm(time, radius = 1.25f, seedOffset = b.colorIndex * 19)
                val ax = (a.x0 + 0.010f * sin(time * 2f * PI.toFloat() + a.phaseOffset) + 0.012f * (na * 2f - 1f)) * w
                val ay = (a.y0 + 0.010f * cos(time * 2f * PI.toFloat() + a.phaseOffset)) * h
                val bx = (b.x0 + 0.010f * sin(time * 2f * PI.toFloat() + b.phaseOffset) + 0.012f * (nb * 2f - 1f)) * w
                val by = (b.y0 + 0.010f * cos(time * 2f * PI.toFloat() + b.phaseOffset)) * h
                val bend = minDim * (0.012f + 0.010f * na)
                val link = Path().apply {
                    moveTo(ax, ay)
                    quadraticTo((ax + bx) * 0.5f + bend, (ay + by) * 0.5f - bend, bx, by)
                }
                drawPath(
                    path = link,
                    color = TonalPalette.withAlpha(c0, 0.20f * dim),
                    style = Stroke(width = minDim * 0.0028f),
                )
            }
            for (a in nodes) {
                val noise = loopedFbm(time, radius = 1.3f, seedOffset = a.colorIndex * 17)
                val ax = (a.x0 + 0.010f * sin(time * 2f * PI.toFloat() + a.phaseOffset) + 0.012f * (noise * 2f - 1f)) * w
                val ay = (a.y0 + 0.010f * cos(time * 2f * PI.toFloat() + a.phaseOffset)) * h
                val breathe = 0.82f + 0.18f * sin(time * 2f * PI.toFloat() + a.phaseOffset)
                val radius = minDim * a.radius * breathe
                drawCircle(
                    brush = Brush.radialGradient(
                        0f to TonalPalette.withAlpha(c0, 0.62f * dim),
                        0.38f to TonalPalette.withAlpha(c0, 0.22f * dim),
                        1f to Color.Transparent,
                        center = Offset(ax, ay),
                        radius = radius * 3f,
                    ),
                    radius = radius * 3f,
                    center = Offset(ax, ay),
                )
                drawCircle(color = TonalPalette.withAlpha(c0, 0.76f * dim), radius = radius * 0.48f, center = Offset(ax, ay))
            }
        }
    }
}

private const val PHYSARUM_SOFT_AGSL = """
uniform float2 iResolution;
uniform float iTime;
uniform float iEnergy;
uniform float3 iColor0;

float segmentGlow(float2 p, float2 a, float2 b, float thickness) {
    float2 ab = b - a;
    float h = clamp(dot(p - a, ab) / max(dot(ab, ab), 0.0001), 0.0, 1.0);
    float d = length(p - (a + h * ab));
    return 1.0 - smoothstep(thickness, thickness * 2.8, d);
}

float nodeGlow(float2 p, float2 c, float radius) {
    float d = length(p - c);
    return exp(-d * d / max(radius * radius, 0.0001));
}

float nest(float2 p, float2 center, float scale, float t) {
    float2 q = (p - center) / scale;
    q += 0.035 * float2(sin(q.y * 3.0 + t), cos(q.x * 3.2 - t));
    float2 a = float2(-0.42, -0.10);
    float2 b = float2(-0.13, -0.34);
    float2 c = float2(0.24, -0.28);
    float2 d = float2(0.40, 0.06);
    float2 e = float2(0.20, 0.36);
    float2 f = float2(-0.18, 0.30);
    float lines = segmentGlow(q, a, b, 0.025) + segmentGlow(q, b, c, 0.024)
        + segmentGlow(q, c, d, 0.024) + segmentGlow(q, d, e, 0.024)
        + segmentGlow(q, e, f, 0.024) + segmentGlow(q, f, a, 0.024)
        + segmentGlow(q, b, f, 0.018) + segmentGlow(q, c, e, 0.018);
    float nodes = nodeGlow(q, a, 0.075) + nodeGlow(q, b, 0.085)
        + nodeGlow(q, c, 0.085) + nodeGlow(q, d, 0.075)
        + nodeGlow(q, e, 0.085) + nodeGlow(q, f, 0.080);
    return lines * 0.34 + nodes * 0.42;
}

half4 main(float2 fragCoord) {
    float2 uv = fragCoord / iResolution;
    float2 p = uv * 2.0 - 1.0;
    p.x *= iResolution.x / iResolution.y;
    float t = iTime;
    float2 q = p + 0.012 * float2(sin(p.y * 2.3 + t * 0.7), cos(p.x * 2.0 - t * 0.6));
    float energy = nest(q, float2(-0.53, -0.45), 0.48, t)
        + nest(q, float2(0.52, -0.46), 0.45, t + 1.1)
        + nest(q, float2(-0.42, 0.48), 0.47, t + 2.3)
        + nest(q, float2(0.52, 0.46), 0.46, t + 3.6);
    float breathe = 0.92 + 0.08 * sin(t * 0.7 + p.x * 2.0 + p.y);
    energy *= breathe * iEnergy;
    float3 base = float3(0.012, 0.022, 0.025);
    float3 col = base + iColor0 * energy * 0.82 + iColor0 * energy * energy * 0.22;
    return half4(col, 1.0);
}
"""

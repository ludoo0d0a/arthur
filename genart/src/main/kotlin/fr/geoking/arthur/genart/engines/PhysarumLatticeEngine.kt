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
import androidx.compose.ui.graphics.Path
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

private data class PhysarumLatticeNode(
    val x0: Float,
    val y0: Float,
    val radius: Float,
    val phaseOffset: Float,
    val colorIndex: Int,
)


@Composable
internal fun PhysarumLatticeEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val count = qualityCount(quality, low = 8, medium = 14, high = 22)
    val nodes = remember(count) {
        List(count) { i ->
            PhysarumLatticeNode(
                x0 = seededUnit(i * 17 + 3),
                y0 = seededUnit(i * 29 + 7),
                radius = seededRange(i * 41 + 11, 0.01f, 0.035f),
                phaseOffset = seededRange(i * 53 + 13, 0f, 2f * PI.toFloat()),
                colorIndex = i,
            )
        }
    }
    val transition = rememberInfiniteTransition(label = "physarumlattice")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((90000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "physarumlattice_t",
    )
    val dim = if (isActive) 1f else 0.55f
    val c0 = TonalPalette.brightness(paletteColors.getOrElse(0) { Color(0xFF7CFFB2) }, brightness * dim)
    val c1 = TonalPalette.brightness(paletteColors.getOrElse(1) { Color(0xFFFF70C8) }, brightness * dim)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val shader = remember { RuntimeShader(PHYSARUM_LATTICE_AGSL) }
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
            drawRect(color = TonalPalette.brightness(Color(0xFF05080A), brightness * dim))
            // Organic branching filaments between drifting nodes.
            for (i in nodes.indices) {
                val a = nodes[i]
                val b = nodes[(i + 1) % nodes.size]
                val branch = nodes[(i + 3) % nodes.size]
                val na = loopedFbm(time, radius = 1.3f, seedOffset = a.colorIndex * 17)
                val nb = loopedFbm(time, radius = 1.25f, seedOffset = b.colorIndex * 19)
                val ax = (a.x0 + 0.04f * (na * 2f - 1f) + 1f) % 1f * w
                val ay = (a.y0 + 0.04f * sin(time * 2f * PI.toFloat() + a.phaseOffset) + 1f) % 1f * h
                val bx = (b.x0 + 0.04f * (nb * 2f - 1f) + 1f) % 1f * w
                val by = (b.y0 + 0.04f * cos(time * 2f * PI.toFloat() + b.phaseOffset) + 1f) % 1f * h
                val branchN = loopedFbm(time, radius = 1.2f, seedOffset = branch.colorIndex * 23)
                val branchX = (branch.x0 + 0.04f * (branchN * 2f - 1f) + 1f) % 1f * w
                val branchY = (branch.y0 + 0.04f * sin(time * 2f * PI.toFloat() + branch.phaseOffset) + 1f) % 1f * h
                val midX = (ax + bx) * 0.5f
                val midY = (ay + by) * 0.5f
                val bend = minOf(w, h) * (0.035f + 0.02f * na)
                val link = Path().apply {
                    moveTo(ax, ay)
                    quadraticTo(midX + bend, midY - bend, bx, by)
                }
                drawPath(
                    path = link,
                    color = TonalPalette.withAlpha(c0, 0.28f * dim),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = minOf(w, h) * 0.004f),
                )
                drawLine(
                    color = TonalPalette.withAlpha(c1, 0.16f * dim),
                    start = Offset(ax, ay),
                    end = Offset(branchX, branchY),
                    strokeWidth = minOf(w, h) * 0.0025f,
                )
                drawLine(
                    color = TonalPalette.withAlpha(c0, 0.18f * dim),
                    start = Offset(bx, by),
                    end = Offset(branchX, branchY),
                    strokeWidth = minOf(w, h) * 0.0025f,
                )
                drawCircle(color = TonalPalette.withAlpha(c0, 0.42f * dim), radius = minOf(w, h) * a.radius, center = Offset(ax, ay))
            }
        }
    }
}

private const val PHYSARUM_LATTICE_AGSL = """
uniform float2 iResolution;
uniform float iTime;
uniform float iEnergy;
uniform float3 iColor0;
uniform float3 iColor1;

half4 main(float2 fragCoord) {
    float2 uv = fragCoord / iResolution;
    float2 p = uv * 2.0 - 1.0;
    p.x *= iResolution.x / iResolution.y;
    float t = iTime;

    // A softly warped grid makes a loose lattice, then branching filaments
    // weave through the intersections like an organic transport network.
    float2 q = p + 0.12 * float2(sin(p.y * 3.4 + t * 0.65), cos(p.x * 3.1 - t * 0.55));
    float gx = abs(sin(q.x * 8.0 + 0.45 * sin(q.y * 2.5 + t)));
    float gy = abs(sin(q.y * 7.0 + 0.4 * cos(q.x * 2.2 - t)));
    float lattice = 1.0 - smoothstep(0.08, 0.23, min(gx, gy));
    float branchA = abs(sin((q.x + q.y * 0.55) * 5.2 + sin(q.y * 3.0 + t * 0.7)));
    float branchB = abs(sin((q.x - q.y * 0.5) * 5.7 + cos(q.x * 2.6 - t * 0.6)));
    float branches = 1.0 - smoothstep(0.035, 0.12, min(branchA, branchB));
    float nodes = exp(-8.0 * abs(sin(q.x * 4.0) * sin(q.y * 3.5)));
    float energy = clamp((lattice * 0.65 + branches * 0.32 + nodes * 0.28) * iEnergy, 0.0, 1.4);
    float3 base = float3(0.012, 0.025, 0.03);
    float3 filament = mix(iColor0, iColor1, clamp(branches * 0.72 + 0.12 * sin(t + q.x * 2.0), 0.0, 1.0));
    float3 col = base + filament * energy * 0.72 + iColor0 * energy * energy * 0.24;
    return half4(col, 1.0);
}
"""

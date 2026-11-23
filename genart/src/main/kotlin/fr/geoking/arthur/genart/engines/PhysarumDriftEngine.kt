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

private const val PHYSARUM_DRIFT_AGSL = """
uniform float2 iResolution;
uniform float iTime;
uniform float iEnergy;
uniform float3 iColor0;

half4 main(float2 fragCoord) {
    float2 uv = fragCoord / iResolution;
    float2 p = uv * 2.0 - 1.0;
    p.x *= iResolution.x / iResolution.y;
    float t = iTime;

    // A set of curved, branching paths with a gentle travelling pulse.
    float bend = sin(p.y * 3.4 + t) * 0.18 + sin(p.y * 6.0 - t * 2.0) * 0.06;
    float spine = abs(p.x - bend - 0.12 * sin(p.y * 1.7 + t));
    float branchA = abs(p.x - bend - (0.30 + 0.10 * sin(p.y * 2.2 + t)) * sin(p.y * 2.9 + 0.7));
    float branchB = abs(p.x - bend + (0.27 + 0.10 * cos(p.y * 2.4 - t * 2.0)) * sin(p.y * 3.3 - 1.1));
    float branchC = abs(p.y - 0.35 * sin(p.x * 2.8 + t * 2.0) - 0.18 * sin(p.x * 5.2 - t));

    float trails = exp(-spine * 38.0) + 0.72 * exp(-branchA * 46.0)
        + 0.66 * exp(-branchB * 46.0) + 0.38 * exp(-branchC * 34.0);
    float pulse = 0.72 + 0.28 * sin((p.y + p.x * 0.24) * 7.0 - t * 2.0);
    float pools = exp(-length(p - float2(-0.48, 0.18)) * 3.8)
        + 0.7 * exp(-length(p - float2(0.48, -0.24)) * 4.0);

    float3 base = float3(0.008, 0.014, 0.018);
    float3 teal = iColor0 * clamp(trails * pulse * iEnergy, 0.0, 1.0);
    float3 violet = float3(0.12, 0.08, 0.22) * clamp(pools * 0.55 * iEnergy, 0.0, 1.0);
    float glow = clamp(trails * 0.18 + pools * 0.12, 0.0, 0.35) * iEnergy;
    return half4(base + teal + violet + iColor0 * glow, 1.0);
}
"""


@Composable
internal fun PhysarumDriftEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val count = qualityCount(quality, low = 8, medium = 14, high = 22)
    val nodeRadii = remember(count) {
        List(count) { i ->
            seededRange(i * 41 + 11, 0.01f, 0.035f)
        }
    }
    val transition = rememberInfiniteTransition(label = "physarumdrift")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((90000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "physarumdrift_t",
    )
    val dim = if (isActive) 1f else 0.55f
    val c0 = TonalPalette.brightness(paletteColors.getOrElse(0) { Color(0xFF7CFFB2) }, brightness * dim)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val shader = remember { RuntimeShader(PHYSARUM_DRIFT_AGSL) }
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
            val time = phase01(t)
            drawRect(color = TonalPalette.brightness(Color(0xFF05080A), brightness * dim))
            val shortSide = minOf(w, h)
            // Soft pools under the organic trail network.
            listOf(0.26f to 0.38f, 0.76f to 0.68f).forEachIndexed { i, (x, y) ->
                val drift = loopedFbm(time, radius = 1.2f, seedOffset = 180 + i * 17)
                val center = Offset((x + 0.025f * (drift - 0.5f)) * w, y * h)
                drawCircle(
                    brush = Brush.radialGradient(
                        0f to TonalPalette.withAlpha(c0, 0.12f * dim),
                        1f to Color.Transparent,
                        center = center,
                        radius = shortSide * 0.24f,
                    ),
                    radius = shortSide * 0.24f,
                    center = center,
                )
            }
            val trailCount = qualityCount(quality, low = 8, medium = 14, high = 22)
            for (i in 0 until trailCount) {
                val seed = i * 23 + 9
                val x0 = seededRange(seed, 0.08f, 0.92f) * w
                val y0 = seededRange(seed + 1, -0.08f, 1.08f) * h
                val drift = loopedFbm(time, radius = 1.1f + i * 0.01f, seedOffset = seed)
                val sway = (drift - 0.5f) * shortSide * 0.07f
                val reach = shortSide * seededRange(seed + 2, 0.10f, 0.22f)
                val direction = if (i % 2 == 0) 1f else -1f
                val start = Offset(x0, y0)
                val end = Offset(x0 + sway + direction * reach * 0.25f, y0 + reach * direction)
                val path = Path().apply {
                    moveTo(start.x, start.y)
                    cubicTo(
                        start.x + sway * 0.6f - direction * reach * 0.30f,
                        start.y + reach * direction * 0.34f,
                        end.x - sway * 0.5f + direction * reach * 0.28f,
                        end.y - reach * direction * 0.28f,
                        end.x,
                        end.y,
                    )
                }
                val alpha = (0.18f + 0.14f * drift) * dim
                drawPath(
                    path = path,
                    color = TonalPalette.withAlpha(c0, alpha * 0.45f),
                    style = Stroke(width = shortSide * 0.018f, cap = StrokeCap.Round),
                )
                drawPath(
                    path = path,
                    color = TonalPalette.withAlpha(c0, alpha),
                    style = Stroke(width = shortSide * 0.004f, cap = StrokeCap.Round),
                )
                val travel = 0.5f + 0.5f * kotlin.math.sin(time * 6.2831853f + i * 0.137f)
                val nodeX = start.x + (end.x - start.x) * travel
                val nodeY = start.y + (end.y - start.y) * travel
                drawCircle(
                    color = TonalPalette.withAlpha(c0, 0.3f * dim),
                    radius = shortSide * nodeRadii[i % nodeRadii.size],
                    center = Offset(nodeX, nodeY),
                )
            }
        }
    }
}

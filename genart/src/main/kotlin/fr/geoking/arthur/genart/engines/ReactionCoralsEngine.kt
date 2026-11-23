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
import androidx.compose.ui.graphics.StrokeCap
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

private data class ReactionCoralsBlob(
    val x0: Float,
    val y0: Float,
    val radius: Float,
    val phaseOffset: Float,
    val colorIndex: Int,
)

private const val REACTION_CORALS_AGSL = """
uniform float2 iResolution;
uniform float iTime;
uniform float iEnergy;
uniform float3 iColor0;
uniform float3 iColor1;

half4 main(float2 fragCoord) {
    float2 uv = fragCoord / iResolution;
    float2 p = (uv - 0.5) * float2(iResolution.x / iResolution.y, 1.0);
    float t = iTime;
    float2 warp = float2(
        sin(p.y * 5.2 + t * 0.45) + cos(p.x * 4.1 - t * 0.35),
        cos(p.x * 5.6 + t * 0.38) + sin(p.y * 3.8 - t * 0.42)
    ) * 0.085;
    float2 q = p + warp;
    float field = 0.0;
    for (int i = 0; i < 7; i++) {
        float fi = float(i);
        float a = fi * 0.897 + t * 0.18;
        float2 c = float2(cos(a) * 0.38, sin(a * 1.31) * 0.38);
        float2 d = q - c;
        float ang = atan(d.y, d.x);
        float r = length(d);
        float branch = abs(sin(ang * (3.0 + mod(fi, 3.0)) + r * 8.0 + fi));
        float vein = 1.0 - smoothstep(0.0, 0.16, abs(branch - 0.18) * 0.055 + abs(r - (0.16 + 0.05 * sin(ang * 3.0 + t * 0.22))) * 0.18);
        float pool = exp(-r * r * 18.0);
        field = max(field, vein * 0.72 + pool * 0.28);
    }
    float contour = smoothstep(0.08, 0.42, field);
    float glow = smoothstep(0.02, 0.22, field) * iEnergy;
    float3 base = float3(0.031, 0.024, 0.047);
    float3 coral = mix(iColor0, iColor1, clamp(0.5 + 0.5 * sin(q.x * 4.0 + q.y * 3.0), 0.0, 1.0));
    float3 col = base + coral * (contour * 0.82 + glow * 0.32);
    col += iColor1 * exp(-length(q) * 3.0) * 0.08;
    return half4(col, 1.0);
}
"""

@Composable
internal fun ReactionCoralsEngine(
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
            ReactionCoralsBlob(
                x0 = seededUnit(i * 19 + 5),
                y0 = seededUnit(i * 31 + 9),
                radius = seededRange(i * 43 + 13, 0.05f, 0.16f),
                phaseOffset = seededRange(i * 53 + 17, 0f, 2f * PI.toFloat()),
                colorIndex = i,
            )
        }
    }
    val transition = rememberInfiniteTransition(label = "reactioncorals")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((80000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "reactioncorals_t",
    )
    val dim = if (isActive) 1f else 0.55f
    val c0 = TonalPalette.brightness(paletteColors.getOrElse(0) { Color(0xFFFF80C8) }, brightness * dim)
    val c1 = TonalPalette.brightness(paletteColors.getOrElse(1) { Color(0xFF80E0FF) }, brightness * dim)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val shader = remember { RuntimeShader(REACTION_CORALS_AGSL) }
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
            val minDim = minOf(w, h)
            blobs.forEach { blob ->
                val n = loopedFbm(time, radius = 1.3f, octaves = 3, seedOffset = blob.colorIndex * 31 + 7)
                val cx = blob.x0 * w
                val cy = blob.y0 * h
                val baseAngle = blob.phaseOffset + time * 2f * PI.toFloat() * 0.08f
                val branchCount = 3 + blob.colorIndex % 3
                for (branch in 0 until branchCount) {
                    val angle = baseAngle + branch * (2f * PI.toFloat() / branchCount)
                    val length = minDim * blob.radius * (1.35f + n * 0.35f)
                    val path = Path().apply {
                        moveTo(cx, cy)
                        val segments = 7
                        for (step in 1..segments) {
                            val f = step / segments.toFloat()
                            val bend = sin(f * PI.toFloat() * 1.5f + baseAngle) * minDim * 0.018f
                            val x = cx + cos(angle) * length * f - sin(angle) * bend
                            val y = cy + sin(angle) * length * f + cos(angle) * bend
                            if (step == 1) lineTo(x, y) else {
                                val prevF = (step - 0.5f) / segments
                                val prevBend = sin(prevF * PI.toFloat() * 1.5f + baseAngle) * minDim * 0.018f
                                quadraticTo(
                                    cx + cos(angle) * length * prevF - sin(angle) * prevBend,
                                    cy + sin(angle) * length * prevF + cos(angle) * prevBend,
                                    x,
                                    y,
                                )
                            }
                        }
                    }
                    val col = if ((blob.colorIndex + branch) % 2 == 0) c0 else c1
                    drawPath(
                        path = path,
                        color = TonalPalette.withAlpha(col, 0.12f * dim),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = minDim * 0.028f, cap = StrokeCap.Round),
                    )
                    drawPath(
                        path = path,
                        color = TonalPalette.withAlpha(col, 0.72f * dim),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = minDim * 0.0045f, cap = StrokeCap.Round),
                    )
                    val tipX = cx + cos(angle) * length
                    val tipY = cy + sin(angle) * length
                    drawCircle(color = TonalPalette.withAlpha(c1, 0.45f * dim), radius = minDim * 0.012f, center = Offset(tipX, tipY))
                }
                drawCircle(color = TonalPalette.withAlpha(c1, 0.18f * dim), radius = minDim * 0.07f, center = Offset(cx, cy))
                drawCircle(color = TonalPalette.withAlpha(c0, 0.7f * dim), radius = minDim * 0.009f, center = Offset(cx, cy))
            }
        }
    }
}

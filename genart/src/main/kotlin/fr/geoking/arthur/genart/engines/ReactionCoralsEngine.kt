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

private const val REACTION_CORALS_LOOP_MS = 11_000f
private const val REACTION_CORALS_BRANCH_CYCLES = 0.65f

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
        sin(p.y * 5.8 + t * 1.05) + cos(p.x * 4.6 - t * 0.88),
        cos(p.x * 6.1 + t * 0.92) + sin(p.y * 4.2 - t * 0.95)
    ) * 0.095;
    float2 q = p + warp;
    float field = 0.0;
    float tips = 0.0;
    for (int i = 0; i < 8; i++) {
        float fi = float(i);
        float a = fi * 0.897 + t * 0.92;
        float2 c = float2(cos(a) * 0.40, sin(a * 1.31) * 0.36);
        float2 d = q - c;
        float ang = atan(d.y, d.x);
        float r = length(d);
        float arms = 3.0 + mod(fi, 3.0);
        float branch = abs(sin(ang * arms + r * 11.0 + fi + t * 0.35));
        float vein = 1.0 - smoothstep(0.0, 0.11, abs(branch - 0.16) * 0.06
            + abs(r - (0.14 + 0.06 * sin(ang * arms + t * 0.85))) * 0.16);
        float twig = abs(sin(ang * (arms + 2.0) + r * 18.0 - t * 0.6 + fi * 0.4));
        float side = (1.0 - smoothstep(0.0, 0.09, abs(twig - 0.22) * 0.07)) * smoothstep(0.05, 0.35, r);
        float pool = exp(-r * r * 16.0);
        field = max(field, vein * 0.78 + side * 0.42 + pool * 0.22);
        tips = max(tips, side * exp(-r * 5.0));
    }
    float contour = smoothstep(0.06, 0.38, field);
    float filigree = smoothstep(0.03, 0.16, field) * (1.0 - contour * 0.35);
    float glow = smoothstep(0.015, 0.20, field) * iEnergy;
    float3 base = float3(0.028, 0.022, 0.045);
    float3 coral = mix(iColor0, iColor1, clamp(0.5 + 0.5 * sin(q.x * 5.0 + q.y * 3.6 + t * 0.4), 0.0, 1.0));
    float3 col = base + coral * (contour * 0.95 + filigree * 0.38 + glow * 0.28);
    col += iColor1 * (tips * 0.22 + exp(-length(q) * 3.2) * 0.07) * iEnergy;
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
                radius = seededRange(i * 43 + 13, 0.06f, 0.18f),
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
            animation = tween((REACTION_CORALS_LOOP_MS / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
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
            val branchSpin = time * 2f * PI.toFloat() * REACTION_CORALS_BRANCH_CYCLES
            drawRect(color = TonalPalette.brightness(Color(0xFF08060C), brightness * dim))
            val minDim = minOf(w, h)
            blobs.forEach { blob ->
                val n = loopedFbm(time, radius = 1.3f, octaves = 3, seedOffset = blob.colorIndex * 31 + 7)
                val cx = blob.x0 * w
                val cy = blob.y0 * h
                val baseAngle = blob.phaseOffset + branchSpin
                val branchCount = 3 + blob.colorIndex % 3
                val colonyR = minDim * blob.radius * (1.05f + n * 0.4f)
                for (branch in 0 until branchCount) {
                    val angle = baseAngle + branch * (2f * PI.toFloat() / branchCount)
                    val length = colonyR * (1.45f + n * 0.35f)
                    val path = Path().apply {
                        moveTo(cx, cy)
                        val segments = 8
                        for (step in 1..segments) {
                            val f = step / segments.toFloat()
                            val bend = sin(f * PI.toFloat() * 1.5f + baseAngle) * minDim * 0.022f
                            val x = cx + cos(angle) * length * f - sin(angle) * bend
                            val y = cy + sin(angle) * length * f + cos(angle) * bend
                            if (step == 1) lineTo(x, y) else {
                                val prevF = (step - 0.5f) / segments
                                val prevBend = sin(prevF * PI.toFloat() * 1.5f + baseAngle) * minDim * 0.022f
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
                        color = TonalPalette.withAlpha(col, 0.16f * dim),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = minDim * 0.032f, cap = StrokeCap.Round),
                    )
                    drawPath(
                        path = path,
                        color = TonalPalette.withAlpha(col, 0.82f * dim),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = minDim * 0.005f, cap = StrokeCap.Round),
                    )
                    for (step in 2..6 step 2) {
                        val f = step / 8f
                        val bx = cx + cos(angle) * length * f
                        val by = cy + sin(angle) * length * f
                        val side = if ((step + branch) % 2 == 0) 1f else -1f
                        val twig = Path().apply {
                            moveTo(bx, by)
                            lineTo(
                                bx + cos(angle + side * 0.82f) * colonyR * 0.48f,
                                by + sin(angle + side * 0.82f) * colonyR * 0.48f,
                            )
                        }
                        drawPath(
                            path = twig,
                            color = TonalPalette.withAlpha(if ((blob.colorIndex + branch) % 2 == 0) c1 else c0, 0.55f * dim),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = minDim * 0.0035f, cap = StrokeCap.Round),
                        )
                    }
                    val tipX = cx + cos(angle) * length
                    val tipY = cy + sin(angle) * length
                    drawCircle(color = TonalPalette.withAlpha(c1, 0.52f * dim), radius = minDim * 0.013f, center = Offset(tipX, tipY))
                }
                drawCircle(color = TonalPalette.withAlpha(c1, 0.22f * dim), radius = colonyR * 1.05f, center = Offset(cx, cy))
                drawCircle(color = TonalPalette.withAlpha(c0, 0.78f * dim), radius = minDim * 0.01f, center = Offset(cx, cy))
            }
        }
    }
}
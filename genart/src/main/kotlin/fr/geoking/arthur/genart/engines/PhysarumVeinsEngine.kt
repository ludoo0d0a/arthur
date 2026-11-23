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
import fr.geoking.arthur.genart.seededUnit
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private data class VeinRoot(
    val x: Float,
    val y: Float,
    val angle: Float,
    val length: Float,
    val width: Float,
    val phase: Float,
)

/** Soft Ambient Physarum Veins — bioluminescent branching trails on a dark organic bed. */
@Composable
internal fun PhysarumVeinsEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val count = qualityCount(quality, low = 8, medium = 14, high = 22)
    val roots = remember(count) {
        List(count) { i ->
            VeinRoot(
                x = seededRange(i * 17 + 3, 0.08f, 0.92f),
                y = seededRange(i * 29 + 7, 0.08f, 0.92f),
                angle = seededRange(i * 41 + 11, 0f, 2f * PI.toFloat()),
                length = seededRange(i * 53 + 13, 0.16f, 0.29f),
                width = seededRange(i * 67 + 19, 0.0015f, 0.0035f),
                phase = seededRange(i * 71 + 23, 0f, 2f * PI.toFloat()),
            )
        }
    }
    val transition = rememberInfiniteTransition(label = "physarumveins")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((90000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "physarumveins_t",
    )
    val dim = if (isActive) 1f else 0.55f
    val accent = TonalPalette.brightness(
        paletteColors.getOrElse(0) { Color(0xFF7CFFB2) },
        brightness * dim,
    )
    val ground = TonalPalette.brightness(Color(0xFF05080A), brightness * dim)

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val shader = remember { RuntimeShader(PHYSARUM_VEINS_AGSL) }
        val brush = remember(shader) { ShaderBrush(shader) }
        Canvas(modifier = modifier) {
            shader.setFloatUniform("iResolution", size.width, size.height)
            shader.setFloatUniform("iTime", phase01(t) * 6.2831853f)
            shader.setFloatUniform("iEnergy", dim)
            shader.setFloatUniform("iColor0", accent.red, accent.green, accent.blue)
            drawRect(brush = brush)
        }
    } else {
        Canvas(modifier = modifier) {
            val w = size.width
            val h = size.height
            val minDim = minOf(w, h)
            val time = phase01(t)
            drawRect(
                brush = Brush.radialGradient(
                    0f to TonalPalette.brightness(Color(0xFF0B1714), brightness * dim),
                    1f to ground,
                    center = Offset(w * 0.47f, h * 0.48f),
                    radius = minDim * 0.78f,
                ),
            )

            // A low contrast substrate of organic pools gives the fine veins depth.
            roots.forEachIndexed { i, root ->
                val n = loopedFbm(time, radius = 1.2f, octaves = 2, seedOffset = i * 19 + 5)
                val sway = sin(time * 2f * PI.toFloat() + root.phase) * minDim * 0.012f
                val cx = root.x * w + sway
                val cy = root.y * h + (n - 0.5f) * minDim * 0.025f
                drawCircle(
                    brush = Brush.radialGradient(
                        0f to TonalPalette.withAlpha(accent, 0.055f * dim),
                        1f to Color.Transparent,
                        center = Offset(cx, cy),
                        radius = minDim * root.length * 0.72f,
                    ),
                    radius = minDim * root.length * 0.72f,
                    center = Offset(cx, cy),
                )
            }

            roots.forEachIndexed { i, root ->
                val n = loopedFbm(time, radius = 1.35f, octaves = 3, seedOffset = i * 31 + 7)
                val drift = sin(time * 2f * PI.toFloat() + root.phase) * 0.08f
                val angle = root.angle + drift
                val start = Offset(root.x * w, root.y * h)
                val len = minDim * root.length * (0.83f + n * 0.25f)
                val end = Offset(start.x + cos(angle) * len, start.y + sin(angle) * len)
                val normal = Offset(-sin(angle), cos(angle))
                val bend = (n - 0.5f) * len * 0.42f
                val c1 = Offset(
                    start.x + cos(angle) * len * 0.34f + normal.x * bend,
                    start.y + sin(angle) * len * 0.34f + normal.y * bend,
                )
                val c2 = Offset(
                    start.x + cos(angle) * len * 0.72f - normal.x * bend * 0.4f,
                    start.y + sin(angle) * len * 0.72f - normal.y * bend * 0.4f,
                )
                val mainPath = Path().apply {
                    moveTo(start.x, start.y)
                    cubicTo(c1.x, c1.y, c2.x, c2.y, end.x, end.y)
                }
                val alpha = (0.42f + n * 0.18f) * dim
                drawPath(
                    mainPath,
                    brush = Brush.radialGradient(
                        0f to TonalPalette.withAlpha(accent, 0.18f * dim),
                        1f to Color.Transparent,
                        center = Offset((start.x + end.x) * 0.5f, (start.y + end.y) * 0.5f),
                        radius = len * 0.54f,
                    ),
                    style = Stroke(width = minDim * 0.014f, cap = StrokeCap.Round),
                )
                drawPath(
                    path = mainPath,
                    color = TonalPalette.withAlpha(accent, alpha),
                    style = Stroke(width = minDim * root.width, cap = StrokeCap.Round),
                )

                // Two tapering branches grow from each main strand at different angles.
                for (branch in 0..1) {
                    val sign = if (branch == 0) -1f else 1f
                    val branchAngle = angle + sign * (0.52f + 0.16f * n)
                    val junction = Offset(
                        start.x + cos(angle) * len * (0.48f + branch * 0.12f) + normal.x * bend * 0.18f,
                        start.y + sin(angle) * len * (0.48f + branch * 0.12f) + normal.y * bend * 0.18f,
                    )
                    val branchLen = len * (0.42f + 0.10f * loopedFbm(time, radius = 1.1f, seedOffset = i * 37 + branch))
                    val branchEnd = Offset(
                        junction.x + cos(branchAngle) * branchLen,
                        junction.y + sin(branchAngle) * branchLen,
                    )
                    val branchNormal = Offset(-sin(branchAngle), cos(branchAngle))
                    val branchBend = (n - 0.5f) * branchLen * 0.36f
                    val branchPath = Path().apply {
                        moveTo(junction.x, junction.y)
                        cubicTo(
                            junction.x + cos(branchAngle) * branchLen * 0.32f + branchNormal.x * branchBend,
                            junction.y + sin(branchAngle) * branchLen * 0.32f + branchNormal.y * branchBend,
                            junction.x + cos(branchAngle) * branchLen * 0.72f - branchNormal.x * branchBend * 0.3f,
                            junction.y + sin(branchAngle) * branchLen * 0.72f - branchNormal.y * branchBend * 0.3f,
                            branchEnd.x,
                            branchEnd.y,
                        )
                    }
                    drawPath(
                        path = branchPath,
                        color = TonalPalette.withAlpha(accent, alpha * 0.68f),
                        style = Stroke(width = minDim * root.width * 0.68f, cap = StrokeCap.Round),
                    )
                    drawCircle(
                        color = TonalPalette.withAlpha(accent, alpha * 0.42f),
                        radius = minDim * root.width * 1.1f,
                        center = branchEnd,
                    )
                }
                drawCircle(
                    brush = Brush.radialGradient(
                        0f to TonalPalette.withAlpha(accent, 0.50f * dim),
                        1f to Color.Transparent,
                        center = start,
                        radius = minDim * 0.026f,
                    ),
                    radius = minDim * 0.026f,
                    center = start,
                )
            }
        }
    }
}

private const val PHYSARUM_VEINS_AGSL = """
uniform float2 iResolution;
uniform float iTime;
uniform float iEnergy;
uniform float3 iColor0;

float segmentDistance(float2 p, float2 a, float2 b) {
    float2 pa = p - a;
    float2 ba = b - a;
    float h = clamp(dot(pa, ba) / max(dot(ba, ba), 0.0001), 0.0, 1.0);
    return length(pa - ba * h);
}

half4 main(float2 fragCoord) {
    float2 uv = fragCoord / iResolution;
    float2 p = uv * 2.0 - 1.0;
    p.x *= iResolution.x / iResolution.y;
    float t = iTime;
    float field = 0.0;
    float glow = 0.0;
    for (int i = 0; i < 9; i++) {
        float fi = float(i);
        float a = fi * 2.399963 + 0.28 * sin(t + fi * 0.71);
        float2 root = float2(cos(fi * 4.17) * 0.58, sin(fi * 2.93) * 0.54);
        float n = sin(t * 0.7 + fi * 1.9);
        float2 dir = float2(cos(a), sin(a));
        float2 normal = float2(-dir.y, dir.x);
        float2 tip = root + dir * (0.46 + 0.08 * n) + normal * 0.07 * sin(t + fi);
        float d = segmentDistance(p, root, tip);
        field += 0.006 / (0.004 + d * d);
        for (int b = 0; b < 2; b++) {
            float s = (b == 0) ? -1.0 : 1.0;
            float ba = a + s * 0.72 + 0.08 * sin(t + fi + float(b));
            float2 junction = mix(root, tip, 0.48 + 0.1 * float(b));
            float2 end = junction + float2(cos(ba), sin(ba)) * 0.28;
            float bd = segmentDistance(p, junction, end);
            field += 0.0035 / (0.004 + bd * bd);
            glow += 0.0006 / (0.018 + bd * bd);
        }
        glow += 0.0008 / (0.018 + d * d);
    }
    float vignette = 1.0 - smoothstep(0.15, 1.55, length(p));
    float veins = clamp(field * 0.34, 0.0, 1.0);
    float3 base = float3(0.012, 0.023, 0.021) * (0.55 + 0.45 * vignette);
    float3 col = base + iColor0 * (veins * 0.52 + glow * 0.34) * iEnergy;
    return half4(half3(col), 1.0);
}
"""

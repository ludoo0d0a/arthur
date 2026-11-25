package fr.geoking.arthur.genart.engines

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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import fr.geoking.arthur.genart.GenartQuality
import fr.geoking.arthur.genart.TonalPalette
import fr.geoking.arthur.genart.phase01
import fr.geoking.arthur.genart.sin01
import fr.geoking.arthur.genart.valueNoise2D
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/** Kanizsa loop duration at default speed (ms) — snappy, seamless, within the 8–14 s brief. */
private const val KANIZSA_LOOP_MS = 11000f

/** Whole-illusory-triangle sway amplitude (± degrees). */
private const val KANIZSA_TILT_DEG = 2.5f

/** Static figure proportions in `min(w, h)` units, precomputed once and scaled per frame. */
private data class KanizsaGeom(
    val r: Float,
    val spread: Float,
    val centers: List<Offset>,
)

private fun kanizsaGeom(): KanizsaGeom = KanizsaGeom(
    r = 0.17f,
    spread = 0.32f,
    centers = listOf(
        Offset(0f, -0.32f),
        Offset(-0.277127f, 0.176f),
        Offset(0.277127f, 0.176f),
    ),
)

/**
 * Kanizsa triangle — three pac-man inducers + soft luminous illusory triangle; inducers keep
 * rigid mouth alignment while the whole figure sways ±2.5° and the luminous halo travels the
 * three illusory edges.
 */
@Composable
internal fun KanizsaTriangleEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val strokeMul = when (quality) {
        GenartQuality.Low -> 0.85f
        GenartQuality.Medium -> 1f
        GenartQuality.High -> 1.2f
    }
    val geo = remember { kanizsaGeom() }
    val transition = rememberInfiniteTransition(label = "kanizsa_triangle")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((KANIZSA_LOOP_MS / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "kanizsa_triangle_t",
    )
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val minDim = min(w, h)
        val time = phase01(t) * 2f * PI.toFloat()
        val dim = if (isActive) 1f else 0.6f
        val grain = 0.97f + 0.05f * valueNoise2D(w * 0.5f * 22f, h * 0.5f * 22f, 7)
        // Warm-to-cold radial backdrop + soft vignette + faint grain.
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    TonalPalette.brightness(Color(0xFFEDE4D2), (brightness * 0.85f) * grain),
                    TonalPalette.brightness(Color(0xFF9AA0A8), brightness * 0.55f),
                    TonalPalette.brightness(Color(0xFF6E7684), (brightness * 0.9f) * grain),
                ),
                center = Offset(w * 0.5f, h * 0.5f),
                radius = maxOf(w, h) * 0.9f,
            ),
        )
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(Color.Transparent, TonalPalette.withAlpha(Color(0xFF23262E), 0.5f)),
                center = Offset(w * 0.5f, h * 0.5f),
                radius = maxOf(w, h) * 0.75f,
            ),
            topLeft = Offset(0f, 0f),
            size = Size(w, h),
        )

        val cx = w * 0.5f
        val cy = h * 0.5f
        val r = geo.r * minDim
        val spread = geo.spread * minDim
        val cos30 = cos(PI.toFloat() / 6f)
        val c0 = Offset(cx, cy - spread)
        val c1 = Offset(cx - spread * cos30, cy + spread * 0.55f)
        val c2 = Offset(cx + spread * cos30, cy + spread * 0.55f)
        val tri = listOf(
            Offset(c0.x, c0.y + r * 0.08f),
            Offset(c1.x + r * 0.18f, c1.y - r * 0.28f),
            Offset(c2.x - r * 0.18f, c2.y - r * 0.28f),
        )
        val centroid = Offset((tri[0].x + tri[1].x + tri[2].x) / 3f, (tri[0].y + tri[1].y + tri[2].y) / 3f)
        val tilt = (KANIZSA_TILT_DEG * (sin01(time) * 2f - 1f)) * PI.toFloat() / 180f
        val cosT = cos(tilt)
        val sinT = sin(tilt)
        fun rot(p: Offset): Offset {
            val dx = p.x - centroid.x
            val dy = p.y - centroid.y
            return Offset(centroid.x + dx * cosT - dy * sinT, centroid.y + dx * sinT + dy * cosT)
        }
        val rt0 = rot(tri[0])
        val rt1 = rot(tri[1])
        val rt2 = rot(tri[2])
        val rc0 = rot(c0)
        val rc1 = rot(c1)
        val rc2 = rot(c2)
        val aim0 = atan2(centroid.y - c0.y, centroid.x - c0.x) + tilt
        val aim1 = atan2(centroid.y - c1.y, centroid.x - c1.x) + tilt
        val aim2 = atan2(centroid.y - c2.y, centroid.x - c2.x) + tilt
        val openRad = (0.18f + 0.025f * sin01(time + 0.4f)) * 2f * PI.toFloat()
        val discFill = TonalPalette.brightness(Color(0xFF111318), brightness * 0.85f)
        val glowColor = TonalPalette.mix(TonalPalette.pick(paletteColors, 1), Color.White, 0.55f)
        val triFillA = (0.10f + 0.12f * sin01(time)) * dim
        val triStrokeA = (0.20f + 0.18f * sin01(time + 1.2f)) * dim
        val haloA = (0.12f + 0.16f * sin01(time)) * dim
        val shadowOff = Offset(r * 0.1f, r * 0.14f)

        fun piePath(c: Offset, aim: Float): Path = Path().apply {
            moveTo(c.x, c.y)
            arcTo(
                rect = Rect(c.x - r, c.y - r, c.x + r, c.y + r),
                startAngleDegrees = Math.toDegrees((aim - openRad * 0.5f).toDouble()).toFloat(),
                sweepAngleDegrees = Math.toDegrees((2f * PI - openRad).toDouble()).toFloat(),
                forceMoveTo = false,
            )
            close()
        }
        fun drawDisc(c: Offset, aim: Float, alpha: Float) {
            drawPath(
                path = piePath(c, aim),
                color = TonalPalette.withAlpha(discFill, alpha),
            )
        }

        // Soft drop shadows (figure lifted off the backdrop).
        drawDisc(Offset(rc0.x + shadowOff.x, rc0.y + shadowOff.y), aim0, 0.30f * dim)
        drawDisc(Offset(rc1.x + shadowOff.x, rc1.y + shadowOff.y), aim1, 0.30f * dim)
        drawDisc(Offset(rc2.x + shadowOff.x, rc2.y + shadowOff.y), aim2, 0.30f * dim)
        val shadowTri = Path().apply {
            moveTo(rt0.x + shadowOff.x, rt0.y + shadowOff.y)
            lineTo(rt1.x + shadowOff.x, rt1.y + shadowOff.y)
            lineTo(rt2.x + shadowOff.x, rt2.y + shadowOff.y)
            close()
        }
        drawPath(shadowTri, color = TonalPalette.withAlpha(discFill, 0.30f * dim))

        // Inducer discs — deep bodies with mouths aimed at the triangle centre.
        drawDisc(rc0, aim0, 0.97f * dim)
        drawDisc(rc1, aim1, 0.97f * dim)
        drawDisc(rc2, aim2, 0.97f * dim)

        // Illusory triangle — soft breathable fill + luminous contour.
        val triPath = Path().apply {
            moveTo(rt0.x, rt0.y)
            lineTo(rt1.x, rt1.y)
            lineTo(rt2.x, rt2.y)
            close()
        }
        drawPath(
            path = triPath,
            color = TonalPalette.withAlpha(
                TonalPalette.brightness(Color(0xFFF6F7F9), brightness * 0.95f),
                triFillA,
            ),
        )
        drawPath(
            path = triPath,
            color = TonalPalette.withAlpha(glowColor, triStrokeA),
            style = Stroke(
                width = r * 0.07f * strokeMul,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round,
            ),
        )

        // Incomplete inverted outlines (classic Kanizsa companion).
        val invSpread = spread * 0.72f
        val inv0 = Offset(cx, cy + invSpread * 0.72f)
        val inv1 = Offset(cx - invSpread * cos30, cy - invSpread * 0.38f)
        val inv2 = Offset(cx + invSpread * cos30, cy - invSpread * 0.38f)
        val iv0 = rot(inv0)
        val iv1 = rot(inv1)
        val iv2 = rot(inv2)
        val outlineColor = TonalPalette.withAlpha(discFill, 0.88f * dim)
        val invStrokeW = r * 0.09f * strokeMul
        for (i in 0..2) {
            val a = when (i) {
                0 -> iv0
                1 -> iv1
                else -> iv2
            }
            val b = when ((i + 1) % 3) {
                0 -> iv0
                1 -> iv1
                else -> iv2
            }
            val dx = b.x - a.x
            val dy = b.y - a.y
            drawLine(
                color = outlineColor,
                start = a,
                end = Offset(a.x + dx * 0.28f, a.y + dy * 0.28f),
                strokeWidth = invStrokeW,
                cap = StrokeCap.Butt,
            )
            drawLine(
                color = outlineColor,
                start = Offset(a.x + dx * 0.72f, a.y + dy * 0.72f),
                end = b,
                strokeWidth = invStrokeW,
                cap = StrokeCap.Butt,
            )
        }

        // Animated light rim along each mouth contour.
        fun drawRim(c: Offset, aim: Float, phase: Float) {
            val startA = aim - openRad * 0.5f
            val p = Path().apply {
                moveTo(c.x + r * cos(startA), c.y + r * sin(startA))
                arcTo(
                    rect = Rect(c.x - r, c.y - r, c.x + r, c.y + r),
                    startAngleDegrees = Math.toDegrees(startA.toDouble()).toFloat(),
                    sweepAngleDegrees = Math.toDegrees((2f * PI - openRad).toDouble()).toFloat(),
                    forceMoveTo = true,
                )
            }
            drawPath(
                path = p,
                color = TonalPalette.withAlpha(
                    glowColor,
                    (0.25f + 0.25f * ((sin(time * 2f + phase) + 1f) * 0.5f)) * dim,
                ),
                style = Stroke(
                    width = r * 0.05f * strokeMul * (0.8f + 0.5f * ((sin(time * 2f) + 1f) * 0.5f)),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round,
                ),
            )
        }
        drawRim(rc0, aim0, 0f)
        drawRim(rc1, aim1, 2.4f)
        drawRim(rc2, aim2, 4.8f)

        // Traveling luminous halo around the three illusory edges.
        fun dist(a: Offset, b: Offset): Float {
            val dx = a.x - b.x
            val dy = a.y - b.y
            return sqrt(dx * dx + dy * dy)
        }
        val len01 = dist(rt0, rt1)
        val len12 = dist(rt1, rt2)
        val len20 = dist(rt2, rt0)
        val perimeter = len01 + len12 + len20
        var lightU = phase01(t) * perimeter
        var lightP = rt0
        var acc = 0f
        for (e in 0..2) {
            val a = if (e == 0) rt0 else if (e == 1) rt1 else rt2
            val b = if (e == 0) rt1 else if (e == 1) rt2 else rt0
            val len = if (e == 0) len01 else if (e == 1) len12 else len20
            if (lightU <= acc + len) {
                val f = ((lightU - acc) / len).coerceIn(0f, 1f)
                lightP = Offset(a.x + (b.x - a.x) * f, a.y + (b.y - a.y) * f)
                break
            }
            acc += len
        }
        drawCircle(
            color = TonalPalette.withAlpha(glowColor, haloA),
            radius = len01 * 0.42f,
            center = lightP,
        )
        drawCircle(
            color = TonalPalette.withAlpha(TonalPalette.mix(glowColor, Color.White, 0.6f), haloA * 0.9f),
            radius = len01 * 0.12f,
            center = lightP,
        )
    }
}

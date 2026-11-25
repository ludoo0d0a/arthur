package fr.geoking.arthur.genart.stills

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import fr.geoking.arthur.genart.AnimationPalette
import fr.geoking.arthur.genart.phase01
import fr.geoking.arthur.genart.valueNoise2D
import androidx.compose.ui.geometry.Offset
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/** Bakes one frozen frame of Kanizsa Triangle for Auto/Ambient album art. */
internal object KanizsaTriangleStill {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)
    private val path = Path()
    private val arcRect = RectF()

    fun draw(
        canvas: Canvas,
        size: Int,
        generation: Long,
        phase: Float,
        rotationDeg: Float,
        pulse: Float,
        palette: AnimationPalette,
    ) {
        val w = size.toFloat()
        val h = size.toFloat()
        val minDim = min(w, h)
        val time = phase + rotationDeg * PI.toFloat() / 180f * 0.06f
        val loop = phase01(time / (2f * PI.toFloat()))
        val ang = loop * 2f * PI.toFloat()
        val sinA = (sin(ang) + 1f) * 0.5f
        val dim = 0.65f + 0.35f * pulse
        val grain = 0.97f + 0.05f * valueNoise2D(w * 0.5f * 22f, h * 0.5f * 22f, 7)
        val innerC = grainColor(0xFFEDE4D2.toInt(), grain)
        val coldC = grainColor(0xFF6E7684.toInt(), grain)
        paint.shader = RadialGradient(
            w * 0.5f, h * 0.5f, maxOf(w, h) * 0.9f,
            intArrayOf(innerC, 0xFF9AA0A8.toInt(), coldC),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP,
        )
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        val cx = w * 0.5f
        val cy = h * 0.5f
        val r = 0.17f * minDim
        val spread = 0.32f * minDim
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
        val tilt = (2.5f * (sinA * 2f - 1f)) * PI.toFloat() / 180f
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
        val openRad = (0.18f + 0.025f * ((sin(ang + 0.4f) + 1f) * 0.5f)) * 2f * PI.toFloat()
        val discBase = Color.argb(255, 17, 19, 24)
        val glow = mixArgb(palette.colorAt(1), Color.WHITE, 0.55f)
        val triFillA = (0.10f + 0.12f * sinA) * dim
        val triStrokeA = (0.20f + 0.18f * ((sin(ang + 1.2f) + 1f) * 0.5f)) * dim
        val haloA = (0.12f + 0.16f * sinA) * dim
        val shadowOff = Offset(r * 0.1f, r * 0.14f)

        fun drawDisc(c: Offset, aim: Float, alpha: Float) {
            val startA = aim - openRad * 0.5f
            arcRect.set(c.x - r, c.y - r, c.x + r, c.y + r)
            path.reset()
            path.moveTo(c.x, c.y)
            path.arcTo(arcRect, toDeg(startA), toDeg(2f * PI.toFloat() - openRad))
            path.close()
            paint.color = withAlpha(discBase, alpha)
            canvas.drawPath(path, paint)
        }

        fun shadowPie(c: Offset, aim: Float) {
            val startA = aim - openRad * 0.5f
            arcRect.set(c.x - r, c.y - r, c.x + r, c.y + r)
            path.reset()
            path.moveTo(c.x, c.y)
            path.arcTo(arcRect, toDeg(startA), toDeg(2f * PI.toFloat() - openRad))
            path.close()
            paint.color = withAlpha(discBase, (0.30f * dim).coerceIn(0f, 1f))
            canvas.drawPath(path, paint)
        }

        // Soft drop shadows (figure lifted off the backdrop).
        shadowPie(Offset(rc0.x + shadowOff.x, rc0.y + shadowOff.y), aim0)
        shadowPie(Offset(rc1.x + shadowOff.x, rc1.y + shadowOff.y), aim1)
        shadowPie(Offset(rc2.x + shadowOff.x, rc2.y + shadowOff.y), aim2)
        val shadowTri = Path().apply {
            moveTo(rt0.x + shadowOff.x, rt0.y + shadowOff.y)
            lineTo(rt1.x + shadowOff.x, rt1.y + shadowOff.y)
            lineTo(rt2.x + shadowOff.x, rt2.y + shadowOff.y)
            close()
        }
        paint.color = withAlpha(discBase, (0.30f * dim).coerceIn(0f, 1f))
        canvas.drawPath(shadowTri, paint)

        // Inducer discs — deep bodies with mouths aimed at the triangle centre.
        drawDisc(rc0, aim0, (0.97f * dim).coerceIn(0f, 1f))
        drawDisc(rc1, aim1, (0.97f * dim).coerceIn(0f, 1f))
        drawDisc(rc2, aim2, (0.97f * dim).coerceIn(0f, 1f))

        // Illusory triangle — soft breathable fill + luminous contour.
        val triPath = Path().apply {
            moveTo(rt0.x, rt0.y)
            lineTo(rt1.x, rt1.y)
            lineTo(rt2.x, rt2.y)
            close()
        }
        paint.color = Color.argb(((triFillA) * 255).toInt().coerceIn(0, 255), 245, 246, 248)
        canvas.drawPath(triPath, paint)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = r * 0.07f
        paint.strokeCap = Paint.Cap.ROUND
        paint.strokeJoin = Paint.Join.ROUND
        paint.color = withAlpha(mixArgb(glow, Color.WHITE, 0.55f), triStrokeA)
        canvas.drawPath(triPath, paint)
        paint.style = Paint.Style.FILL

        // Incomplete inverted outlines (classic Kanizsa companion).
        val invSpread = spread * 0.72f
        val inv0 = Offset(cx, cy + invSpread * 0.72f)
        val inv1 = Offset(cx - invSpread * cos30, cy - invSpread * 0.38f)
        val inv2 = Offset(cx + invSpread * cos30, cy - invSpread * 0.38f)
        val iv0 = rot(inv0)
        val iv1 = rot(inv1)
        val iv2 = rot(inv2)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = r * 0.09f
        paint.strokeCap = Paint.Cap.BUTT
        paint.color = withAlpha(discBase, (0.88f * dim).coerceIn(0f, 1f))
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
            canvas.drawLine(a.x, a.y, a.x + dx * 0.28f, a.y + dy * 0.28f, paint)
            canvas.drawLine(a.x + dx * 0.72f, a.y + dy * 0.72f, b.x, b.y, paint)
        }
        paint.style = Paint.Style.FILL

        // Animated light rim along each mouth contour.
        fun drawRim(c: Offset, aim: Float, phase: Float) {
            val startA = aim - openRad * 0.5f
            arcRect.set(c.x - r, c.y - r, c.x + r, c.y + r)
            path.reset()
            path.moveTo(c.x + r * cos(startA), c.y + r * sin(startA))
            path.arcTo(arcRect, toDeg(startA), toDeg(2f * PI.toFloat() - openRad))
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = r * 0.05f * (0.8f + 0.5f * ((sin(ang * 2f + phase) + 1f) * 0.5f))
            paint.strokeCap = Paint.Cap.ROUND
            paint.strokeJoin = Paint.Join.ROUND
            paint.color = withAlpha(mixArgb(glow, Color.WHITE, 0.35f), (0.25f + 0.25f * ((sin(ang * 2f + phase) + 1f) * 0.5f)) * dim)
            canvas.drawPath(path, paint)
            paint.style = Paint.Style.FILL
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
        var lightU = phase01(time) * perimeter
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
        paint.color = withAlpha(glow, haloA)
        canvas.drawCircle(lightP.x, lightP.y, len01 * 0.42f, paint)
        paint.color = withAlpha(mixArgb(glow, Color.WHITE, 0.6f), haloA * 0.9f)
        canvas.drawCircle(lightP.x, lightP.y, len01 * 0.12f, paint)

        paint.alpha = 255
    }

    private fun grainColor(base: Int, g: Float): Int {
        fun ch(c: Int) = (Color.red(c) * g).toInt().coerceIn(0, 255)
        return Color.argb(Color.alpha(base), ch(Color.red(base)), ch(Color.green(base)), ch(Color.blue(base)))
    }

    private fun withAlpha(argb: Int, alpha: Float): Int =
        Color.argb(
            (alpha * 255).toInt().coerceIn(0, 255),
            Color.red(argb),
            Color.green(argb),
            Color.blue(argb),
        )

    private fun mixArgb(a: Int, b: Int, t: Float): Int {
        val u = t.coerceIn(0f, 1f)
        return Color.argb(
            255,
            (Color.red(a) + (Color.red(b) - Color.red(a)) * u).toInt().coerceIn(0, 255),
            (Color.green(a) + (Color.green(b) - Color.green(a)) * u).toInt().coerceIn(0, 255),
            (Color.blue(a) + (Color.blue(b) - Color.blue(a)) * u).toInt().coerceIn(0, 255),
        )
    }

    private fun toDeg(rad: Float): Float = Math.toDegrees(rad.toDouble()).toFloat()
}

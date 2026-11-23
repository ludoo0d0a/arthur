package fr.geoking.arthur.genart.stills

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import fr.geoking.arthur.genart.AnimationPalette
import fr.geoking.arthur.genart.loopedFbm
import kotlin.math.PI
import kotlin.math.sin

/** Bakes one frozen frame of Moto Night Lean for Auto/Ambient album art. */
internal object MotoNightLeanStill {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)
    private val roadPath = Path()
    private val leftShoulder = Path()
    private val rightShoulder = Path()
    private val centerLine = Path()
    private val ridgePath = Path()

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
        val minDim = size.toFloat()
        val t01 = ((phase % (2f * PI.toFloat())) / (2f * PI.toFloat())).let { if (it < 0f) it + 1f else it }
        val noise = loopedFbm(t01, radius = 1.35f, octaves = 2, seedOffset = 11)
        val lean = sin(t01 * 2f * PI.toFloat()) * 0.11f + (noise - 0.5f) * 0.045f
        val horizonY = h * (0.52f + (noise - 0.5f) * 0.025f)
        val horizonX = w * (0.5f + lean * 0.45f)
        val dim = 0.85f + 0.15f * pulse
        val accent = mixArgb(0xFF6080C0.toInt(), palette.colorAt(0), 0.28f)

        paint.style = Paint.Style.FILL
        paint.shader = LinearGradient(
            0f, 0f, 0f, h,
            intArrayOf(0xFF0C1018.toInt(), 0xFF181E28.toInt(), 0xFF080A0C.toInt()),
            floatArrayOf(0f, 0.52f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        paint.shader = LinearGradient(
            0f, horizonY - h * 0.11f, 0f, horizonY + h * 0.055f,
            intArrayOf(Color.TRANSPARENT, withAlpha(accent, (0.18f * dim * 255).toInt())),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, horizonY - h * 0.11f, w, horizonY + h * 0.055f, paint)
        paint.shader = null

        ridgePath.reset()
        ridgePath.moveTo(0f, horizonY + h * 0.025f)
        ridgePath.cubicTo(w * 0.18f, horizonY - h * 0.025f, w * 0.27f, horizonY + h * 0.08f, horizonX, horizonY + h * 0.035f)
        ridgePath.cubicTo(w * 0.72f, horizonY - h * 0.015f, w * 0.86f, horizonY + h * 0.07f, w, horizonY + h * 0.01f)
        ridgePath.lineTo(w, h)
        ridgePath.lineTo(0f, h)
        ridgePath.close()
        paint.color = withAlpha(0xFF080A10.toInt(), (0.52f * dim * 255).toInt())
        canvas.drawPath(ridgePath, paint)

        val leftBottomX = -w * 0.08f
        val rightBottomX = w * 1.08f
        leftShoulder.reset()
        leftShoulder.moveTo(horizonX - w * 0.018f, horizonY)
        leftShoulder.cubicTo(horizonX - w * 0.11f + lean * w * 0.10f, h * 0.66f, w * 0.20f + lean * w * 0.30f, h * 0.84f, leftBottomX, h * 1.04f)
        rightShoulder.reset()
        rightShoulder.moveTo(horizonX + w * 0.018f, horizonY)
        rightShoulder.cubicTo(horizonX + w * 0.11f + lean * w * 0.10f, h * 0.66f, w * 0.80f + lean * w * 0.30f, h * 0.84f, rightBottomX, h * 1.04f)

        roadPath.reset()
        roadPath.moveTo(horizonX - w * 0.018f, horizonY)
        roadPath.cubicTo(horizonX - w * 0.11f + lean * w * 0.10f, h * 0.66f, w * 0.20f + lean * w * 0.30f, h * 0.84f, leftBottomX, h * 1.04f)
        roadPath.lineTo(rightBottomX, h * 1.04f)
        roadPath.cubicTo(w * 0.80f + lean * w * 0.30f, h * 0.84f, horizonX + w * 0.11f + lean * w * 0.10f, h * 0.66f, horizonX + w * 0.018f, horizonY)
        roadPath.close()
        paint.shader = LinearGradient(
            0f, horizonY, 0f, h,
            intArrayOf(withAlpha(0xFF202834.toInt(), (0.78f * dim * 255).toInt()), withAlpha(0xFF05070A.toInt(), (0.97f * dim * 255).toInt())),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawPath(roadPath, paint)
        paint.shader = null

        paint.style = Paint.Style.STROKE
        paint.strokeCap = Paint.Cap.ROUND
        paint.strokeWidth = minDim * 0.004f
        paint.color = withAlpha(accent, (0.28f * dim * 255).toInt())
        canvas.drawPath(leftShoulder, paint)
        paint.strokeWidth = minDim * 0.0035f
        paint.color = withAlpha(accent, (0.24f * dim * 255).toInt())
        canvas.drawPath(rightShoulder, paint)

        centerLine.reset()
        centerLine.moveTo(horizonX, horizonY)
        centerLine.cubicTo(horizonX + lean * w * 0.10f, h * 0.67f, w * 0.50f + lean * w * 0.30f, h * 0.84f, w * (0.50f + lean * 0.30f), h * 1.02f)
        paint.strokeWidth = minDim * 0.004f
        paint.color = withAlpha(0xFF9AAAC8.toInt(), (0.32f * dim * 255).toInt())
        paint.pathEffect = android.graphics.DashPathEffect(floatArrayOf(minDim * 0.05f, minDim * 0.055f), -t01 * minDim * 0.105f)
        canvas.drawPath(centerLine, paint)
        paint.pathEffect = null
        paint.style = Paint.Style.FILL

        for (i in 0 until 9) {
            val side = if (i % 2 == 0) -1f else 1f
            val phaseOffset = (0.13f + i * 0.173f) % 1f
            val depthSeed = 0.12f + ((i * 37 % 100) / 100f) * 0.78f
            val travel = (phaseOffset + t01 * (0.45f + depthSeed * 0.25f)) % 1f
            val perspective = (0.12f + travel * 0.88f).let { it * it }
            val center = horizonX + lean * w * perspective * 0.45f
            val roadHalf = w * (0.025f + perspective * 0.57f)
            val x = center + side * (roadHalf * (1.18f + depthSeed * 0.38f))
            val y = horizonY + (h - horizonY) * perspective
            val dx = side * (w * (0.035f + (i % 4) * 0.017f) * (0.20f + perspective))
            val dy = h * (0.035f + (i % 3) * 0.02f) * (0.12f + perspective * 0.65f)
            val light = mixArgb(palette.colorAt(i), accent, 0.42f)
            paint.color = withAlpha(light, ((0.12f + 0.16f * perspective) * dim * 255).toInt())
            paint.strokeWidth = minDim * (0.0015f + perspective * 0.003f)
            paint.strokeCap = Paint.Cap.ROUND
            canvas.drawLine(x - dx * 0.18f, y - dy * 0.25f, x + dx, y + dy, paint)
            paint.color = withAlpha(light, (0.24f * dim * 255).toInt())
            canvas.drawCircle(x, y, minDim * (0.002f + perspective * 0.004f), paint)
        }

        val lampX = w * (0.5f + lean * 0.24f)
        val lampY = h * 0.82f
        paint.shader = RadialGradient(
            lampX, lampY, minDim * 0.62f,
            intArrayOf(withAlpha(accent, (0.16f * dim * 255).toInt()), withAlpha(accent, (0.055f * dim * 255).toInt()), Color.TRANSPARENT),
            floatArrayOf(0f, 0.55f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(lampX, lampY, minDim * 0.62f, paint)
        paint.shader = RadialGradient(
            horizonX, horizonY + h * 0.01f, minDim * 0.16f,
            intArrayOf(withAlpha(0xFFC06068.toInt(), (0.10f * dim * 255).toInt()), Color.TRANSPARENT),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(horizonX, horizonY + h * 0.01f, minDim * 0.16f, paint)
        paint.shader = null
        paint.style = Paint.Style.FILL
    }

    private fun mixArgb(a: Int, b: Int, t: Float): Int {
        val u = t.coerceIn(0f, 1f)
        val r = (Color.red(a) + (Color.red(b) - Color.red(a)) * u).toInt().coerceIn(0, 255)
        val g = (Color.green(a) + (Color.green(b) - Color.green(a)) * u).toInt().coerceIn(0, 255)
        val bl = (Color.blue(a) + (Color.blue(b) - Color.blue(a)) * u).toInt().coerceIn(0, 255)
        return Color.rgb(r, g, bl)
    }

    private fun withAlpha(rgb: Int, alpha: Int): Int =
        Color.argb(alpha.coerceIn(0, 255), Color.red(rgb), Color.green(rgb), Color.blue(rgb))
}

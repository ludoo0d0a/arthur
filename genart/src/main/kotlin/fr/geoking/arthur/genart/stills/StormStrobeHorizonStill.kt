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
import kotlin.math.exp

internal object StormStrobeHorizonStill {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)
    private val cloudPath = Path()
    private val landPath = Path()

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
        val t01 = ((phase % (2f * PI.toFloat())) / (2f * PI.toFloat())).let { v -> if (v < 0f) v + 1f else v }
        val drift = loopedFbm(t01, radius = 1.15f, octaves = 2, seedOffset = 7)
        val flashPhase = (t01 + 0.41f) % 1f
        val flash = exp(-((flashPhase - 0.31f) * (flashPhase - 0.31f)) / 0.0018f)
        val secondary = exp(-((flashPhase - 0.76f) * (flashPhase - 0.76f)) / 0.0013f)
        val flashAmt = ((flash + 0.38f * secondary) * (0.74f + 0.26f * pulse)).coerceIn(0f, 1f)

        paint.style = Paint.Style.FILL
        paint.shader = LinearGradient(
            0f, 0f, 0f, h,
            intArrayOf(0xFF0A0E18.toInt(), 0xFF151D2A.toInt(), 0xFF1A2230.toInt()),
            floatArrayOf(0f, 0.52f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        // Soft lightning glow stays low on the horizon, illuminating only the nearby cloud base.
        val glowX = w * (0.57f + (drift - 0.5f) * 0.10f)
        val glowY = h * 0.67f
        val radius = size * 0.64f
        paint.shader = RadialGradient(
            glowX, glowY, radius,
            intArrayOf(
                Color.argb((255f * (0.13f + 0.31f * flashAmt)).toInt(), 201, 217, 242),
                Color.argb((255f * (0.08f + 0.15f * flashAmt)).toInt(), 135, 157, 188),
                Color.TRANSPARENT,
            ),
            floatArrayOf(0f, 0.38f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawOval(glowX - radius, glowY - radius * 0.55f, glowX + radius, glowY + radius * 0.55f, paint)
        paint.shader = null

        // Distant cloud shelf.
        cloudPath.reset()
        cloudPath.moveTo(0f, h * 0.59f)
        cloudPath.cubicTo(w * 0.18f, h * 0.54f, w * 0.27f, h * 0.61f, w * 0.43f, h * 0.57f)
        cloudPath.cubicTo(w * 0.63f, h * 0.52f, w * 0.78f, h * 0.60f, w, h * (0.55f + 0.025f * drift))
        cloudPath.lineTo(w, h * 0.84f)
        cloudPath.lineTo(0f, h * 0.84f)
        cloudPath.close()
        paint.color = Color.argb((225f + 20f * flashAmt).toInt().coerceIn(0, 255), 38, 48, 64)
        canvas.drawPath(cloudPath, paint)

        // Two nearer cloud banks frame the distant illumination.
        drawCloud(canvas, w * (0.20f + (drift - 0.5f) * 0.02f), h * 0.51f, w * 0.70f, h * 0.25f, flashAmt, 0.80f, size * 0.006f)
        drawCloud(canvas, w * (0.80f - (drift - 0.5f) * 0.025f), h * 0.57f, w * 0.62f, h * 0.22f, flashAmt, 0.92f, size * 0.006f)

        // Thin reflected glow traces the horizon without introducing a hard light source.
        paint.color = Color.argb((18f + 45f * flashAmt).toInt().coerceIn(0, 90), 195, 211, 236)
        paint.strokeWidth = size * 0.006f
        paint.strokeCap = Paint.Cap.ROUND
        canvas.drawLine(w * 0.18f, h * 0.73f, w * 0.81f, h * (0.72f + 0.008f * drift), paint)

        landPath.reset()
        landPath.moveTo(0f, h * 0.76f)
        landPath.cubicTo(w * 0.18f, h * 0.72f, w * 0.32f, h * 0.78f, w * 0.47f, h * 0.75f)
        landPath.cubicTo(w * 0.66f, h * 0.71f, w * 0.80f, h * 0.79f, w, h * 0.74f)
        landPath.lineTo(w, h)
        landPath.lineTo(0f, h)
        landPath.close()
        paint.color = Color.rgb(10, 14, 24)
        canvas.drawPath(landPath, paint)
    }

    private fun drawCloud(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        width: Float,
        height: Float,
        flash: Float,
        shade: Float,
        strokeWidth: Float,
    ) {
        cloudPath.reset()
        cloudPath.moveTo(cx - width * 0.55f, cy + height * 0.16f)
        cloudPath.cubicTo(cx - width * 0.48f, cy - height * 0.08f, cx - width * 0.32f, cy - height * 0.10f, cx - width * 0.23f, cy - height * 0.02f)
        cloudPath.cubicTo(cx - width * 0.12f, cy - height * 0.28f, cx + width * 0.06f, cy - height * 0.24f, cx + width * 0.15f, cy - height * 0.04f)
        cloudPath.cubicTo(cx + width * 0.28f, cy - height * 0.16f, cx + width * 0.43f, cy - height * 0.06f, cx + width * 0.50f, cy + height * 0.16f)
        cloudPath.lineTo(cx + width * 0.55f, cy + height * 0.44f)
        cloudPath.lineTo(cx - width * 0.55f, cy + height * 0.44f)
        cloudPath.close()
        paint.color = Color.argb((220f * shade).toInt().coerceIn(0, 255), 34, 44, 59)
        canvas.drawPath(cloudPath, paint)
        paint.color = Color.argb((8f + 30f * flash).toInt().coerceIn(0, 50), 184, 200, 222)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = strokeWidth
        canvas.drawPath(cloudPath, paint)
        paint.style = Paint.Style.FILL
    }
}

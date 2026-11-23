package fr.geoking.arthur.genart.stills

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import fr.geoking.arthur.genart.AnimationPalette
import kotlin.math.sin

/** Bakes one frozen frame of Distant Dog Walk for Auto/Ambient album art. */
internal object DogDistantWalkStill {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)
    private val rect = RectF()
    private val path = Path()

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
        val minDim = w.coerceAtMost(h)

        // Dusk sky gradient
        paint.shader = LinearGradient(
            0f, 0f, 0f, h,
            intArrayOf(0xFF0E1428.toInt(), 0xFF1A2438.toInt(), 0xFF3A2E3A.toInt(), 0xFF6E4A3E.toInt(), 0xFF1A1210.toInt()),
            floatArrayOf(0f, 0.25f, 0.5f, 0.78f, 1f),
            Shader.TileMode.CLAMP,
        )
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        val horizonY = h * 0.58f

        // Distant hills
        paint.color = 0xFF1E1810.toInt()
        path.reset()
        path.moveTo(0f, horizonY + h * 0.02f)
        path.cubicTo(w * 0.15f, horizonY - h * 0.08f, w * 0.35f, horizonY - h * 0.05f, w * 0.5f, horizonY - h * 0.01f)
        path.cubicTo(w * 0.65f, horizonY + h * 0.02f, w * 0.82f, horizonY - h * 0.06f, w, horizonY + h * 0.01f)
        path.lineTo(w, horizonY + h * 0.05f)
        path.lineTo(0f, horizonY + h * 0.05f)
        path.close()
        canvas.drawPath(path, paint)

        // Ground
        paint.color = 0xFF1C1612.toInt()
        canvas.drawRect(0f, horizonY, w, h, paint)

        // Dusk glow on horizon
        val duskGlow = mixArgb(0xFFD08858.toInt(), palette.colorAt(1), 0.2f)
        paint.shader = RadialGradient(
            w * 0.65f, horizonY - h * 0.02f, w * 0.45f,
            intArrayOf(
                Color.argb((90 * pulse).toInt().coerceIn(50, 130), Color.red(duskGlow), Color.green(duskGlow), Color.blue(duskGlow)),
                Color.argb(30, Color.red(duskGlow), Color.green(duskGlow), Color.blue(duskGlow)),
                Color.TRANSPARENT,
            ),
            floatArrayOf(0f, 0.6f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, horizonY - h * 0.15f, w, horizonY + h * 0.1f, paint)
        paint.shader = null

        // Grass blades along horizon
        paint.color = mixArgb(0xFF2A2210.toInt(), palette.colorAt(2), 0.15f)
        paint.strokeWidth = minDim * 0.005f
        paint.strokeCap = Paint.Cap.ROUND
        paint.style = Paint.Style.STROKE
        val grassPositions = floatArrayOf(0.08f, 0.18f, 0.28f, 0.42f, 0.55f, 0.65f, 0.76f, 0.88f, 0.95f)
        grassPositions.forEachIndexed { i, gx ->
            val baseX = gx * w
            val baseY = horizonY + minDim * 0.012f
            val tipX = baseX + sin(phase + i * 0.7f) * minDim * 0.006f
            val tipY = baseY - minDim * (0.035f + i * 0.003f)
            canvas.drawLine(baseX, baseY, tipX, tipY, paint)
        }
        paint.style = Paint.Style.FILL

        // Dog silhouette
        val t = ((phase % (2f * Math.PI.toFloat())) / (2f * Math.PI.toFloat()) + 1f) % 1f
        val walkCycle = if (t < 0.5f) t * 2f else (1f - t) * 2f
        val dogX = w * (0.18f + 0.64f * walkCycle)
        val dogY = horizonY - minDim * 0.02f
        val scale = minDim * 0.035f * (1f + 0.02f * pulse)
        paint.color = mixArgb(0xFF0E0C0A.toInt(), palette.colorAt(0), 0.15f)

        rect.set(dogX - scale * 1.4f, dogY - scale * 1.1f, dogX + scale * 1.4f, dogY + scale * 0.4f)
        canvas.drawOval(rect, paint)
        canvas.drawCircle(dogX + scale * 1.5f, dogY - scale * 0.9f, scale * 0.7f, paint)
        // Snout
        rect.set(dogX + scale * 2.0f, dogY - scale * 0.85f, dogX + scale * 2.55f, dogY - scale * 0.5f)
        canvas.drawOval(rect, paint)

        // Legs
        paint.strokeWidth = scale * 0.35f
        paint.strokeCap = Paint.Cap.ROUND
        paint.style = Paint.Style.STROKE
        val legPhase = sin(phase * 4f) * scale * 0.25f
        path.reset()
        path.moveTo(dogX - scale * 0.8f, dogY)
        path.lineTo(dogX - scale * 0.9f, dogY + scale * 1.2f + legPhase)
        path.moveTo(dogX + scale * 0.2f, dogY)
        path.lineTo(dogX + scale * 0.35f, dogY + scale * 1.2f - legPhase)
        path.moveTo(dogX + scale * 0.9f, dogY)
        path.lineTo(dogX + scale * 1.0f, dogY + scale * 1.15f + legPhase * 0.7f)
        path.moveTo(dogX - scale * 0.3f, dogY)
        path.lineTo(dogX - scale * 0.35f, dogY + scale * 1.18f - legPhase * 0.5f)
        canvas.drawPath(path, paint)

        // Ear
        paint.style = Paint.Style.FILL
        path.reset()
        val ehx = dogX + scale * 1.5f
        val ehy = dogY - scale * 0.9f
        path.moveTo(ehx - scale * 0.2f, ehy - scale * 0.3f)
        path.lineTo(ehx - scale * 0.55f, ehy - scale * 1.0f)
        path.lineTo(ehx + scale * 0.1f, ehy - scale * 0.45f)
        path.close()
        canvas.drawPath(path, paint)

        // Tail
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = scale * 0.3f
        path.reset()
        val tailBaseX = dogX - scale * 1.3f
        val tailBaseY = dogY - scale * 0.6f
        path.moveTo(tailBaseX, tailBaseY)
        path.quadTo(
            tailBaseX - scale * 0.4f, tailBaseY - scale * 0.8f,
            tailBaseX - scale * 0.15f, tailBaseY - scale * 1.3f,
        )
        canvas.drawPath(path, paint)
        paint.style = Paint.Style.FILL
    }

    private fun mixArgb(a: Int, b: Int, t: Float): Int {
        val u = t.coerceIn(0f, 1f)
        val r = (Color.red(a) + (Color.red(b) - Color.red(a)) * u).toInt().coerceIn(0, 255)
        val g = (Color.green(a) + (Color.green(b) - Color.green(a)) * u).toInt().coerceIn(0, 255)
        val bl = (Color.blue(a) + (Color.blue(b) - Color.blue(a)) * u).toInt().coerceIn(0, 255)
        return Color.rgb(r, g, bl)
    }
}

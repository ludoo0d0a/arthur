package fr.geoking.arthur.genart.stills

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import fr.geoking.arthur.genart.AnimationPalette
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/** Bakes one frozen frame of the Distant Dinosaur Silhouettes engine for Auto/Ambient album art. */
internal object DistantDinosaursStill {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()
    private val duskSkyTop = Color.rgb(0x24, 0x17, 0x36)
    private val duskSkyHorizon = Color.rgb(0x6B, 0x4A, 0x55)
    private val groundColor = Color.rgb(0x0C, 0x09, 0x12)
    private const val DINO_COUNT = 2

    fun draw(
        canvas: Canvas,
        size: Int,
        generation: Long,
        phase: Float,
        rotationDeg: Float,
        pulse: Float,
        palette: AnimationPalette,
    ) {
        val rnd = Random(generation)
        val w = size.toFloat()
        val h = size.toFloat()
        val minDim = w.coerceAtMost(h)
        val groundY = h * 0.74f

        paint.shader = LinearGradient(
            0f,
            0f,
            0f,
            groundY,
            intArrayOf(duskSkyTop, duskSkyHorizon, groundColor),
            floatArrayOf(0f, 0.75f, 1f),
            Shader.TileMode.CLAMP,
        )
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w, groundY, paint)
        paint.shader = null

        paint.color = groundColor
        canvas.drawRect(0f, groundY, w, h, paint)

        for (i in 0 until DINO_COUNT) {
            val xFrac = 0.22f + rnd.nextFloat() * 0.56f
            val scaleFrac = 0.8f + rnd.nextFloat() * 0.35f
            val faceDir = if (rnd.nextFloat() < 0.5f) -1f else 1f
            val swayFreq = 0.12f + rnd.nextFloat() * 0.08f
            val phaseOffset = rnd.nextFloat() * 2f * PI.toFloat()

            val bodyWidth = minDim * scaleFrac * 0.34f
            val bodyHeight = bodyWidth * 0.55f
            val bodyCx = w * xFrac
            val bodyCy = groundY - bodyHeight * 0.35f

            val baseColor = palette.colorAt(i)
            val silhouette = scaleBrightness(mixArgb(groundColor, baseColor, 0.14f), pulse.coerceIn(0.3f, 1.2f))

            paint.color = silhouette
            canvas.drawOval(
                bodyCx - bodyWidth / 2f,
                bodyCy - bodyHeight / 2f,
                bodyCx + bodyWidth / 2f,
                bodyCy + bodyHeight / 2f,
                paint,
            )

            val neckHeight = bodyHeight * 3.1f
            val headRadius = bodyWidth * 0.16f
            val neckBaseX = bodyCx + faceDir * bodyWidth * 0.32f
            val neckBaseY = bodyCy - bodyHeight * 0.32f
            val tipDx = faceDir * neckHeight * 0.45f
            val bendDx = faceDir * neckHeight * 0.22f

            val swayAngle = sin(phase * swayFreq + phaseOffset) * 6f

            canvas.save()
            canvas.rotate(swayAngle, neckBaseX, neckBaseY)

            val baseHalfWidth = headRadius * 1.1f
            val tipHalfWidth = headRadius * 0.6f
            val tipX = neckBaseX + tipDx
            val tipY = neckBaseY - neckHeight

            path.reset()
            path.moveTo(neckBaseX - baseHalfWidth, neckBaseY)
            path.cubicTo(
                neckBaseX - baseHalfWidth + bendDx * 0.3f,
                neckBaseY - neckHeight * 0.55f,
                tipX - tipHalfWidth - bendDx * 0.2f,
                tipY + neckHeight * 0.25f,
                tipX - tipHalfWidth,
                tipY,
            )
            path.lineTo(tipX + tipHalfWidth, tipY)
            path.cubicTo(
                tipX + tipHalfWidth - bendDx * 0.2f,
                tipY + neckHeight * 0.25f,
                neckBaseX + baseHalfWidth + bendDx * 0.3f,
                neckBaseY - neckHeight * 0.55f,
                neckBaseX + baseHalfWidth,
                neckBaseY,
            )
            path.close()

            paint.color = silhouette
            canvas.drawPath(path, paint)
            canvas.drawCircle(tipX, tipY, headRadius, paint)

            canvas.restore()
        }
    }

    private fun mixArgb(a: Int, b: Int, t: Float): Int {
        val u = t.coerceIn(0f, 1f)
        val r = (Color.red(a) + (Color.red(b) - Color.red(a)) * u).toInt().coerceIn(0, 255)
        val g = (Color.green(a) + (Color.green(b) - Color.green(a)) * u).toInt().coerceIn(0, 255)
        val bl = (Color.blue(a) + (Color.blue(b) - Color.blue(a)) * u).toInt().coerceIn(0, 255)
        return Color.rgb(r, g, bl)
    }

    private fun scaleBrightness(color: Int, factor: Float): Int {
        val r = (Color.red(color) * factor).toInt().coerceIn(0, 255)
        val g = (Color.green(color) * factor).toInt().coerceIn(0, 255)
        val b = (Color.blue(color) * factor).toInt().coerceIn(0, 255)
        return Color.rgb(r, g, b)
    }
}

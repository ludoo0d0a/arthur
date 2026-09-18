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

/** Bakes one frozen frame of "Reeds at Water's Edge" for Android Auto album art — deterministic per [generation]. */
internal object ReedsStill {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)
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
        val rnd = Random(generation)
        val w = size.toFloat()
        val h = size.toFloat()

        paint.shader = LinearGradient(
            0f,
            0f,
            0f,
            h,
            intArrayOf(0xFF03141A.toInt(), 0xFF0A2C30.toInt()),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP,
        )
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        val waterTint = palette.colorAt(0)
        val waterTop = Color.argb(0, Color.red(waterTint), Color.green(waterTint), Color.blue(waterTint))
        val waterBottom = Color.argb(
            (0.22f * 255).toInt().coerceIn(0, 255),
            Color.red(waterTint),
            Color.green(waterTint),
            Color.blue(waterTint),
        )
        paint.shader = LinearGradient(
            0f,
            h * 0.72f,
            0f,
            h,
            intArrayOf(waterTop, waterBottom),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, h * 0.72f, w, h, paint)
        paint.shader = null

        val reedCount = 20
        val reeds = (0 until reedCount).map { i ->
            val depth = i % 2
            val jitter = (rnd.nextFloat() - 0.5f) * (0.7f / reedCount)
            val heightFrac = if (depth == 0) {
                0.45f + rnd.nextFloat() * 0.2f
            } else {
                0.65f + rnd.nextFloat() * 0.27f
            }
            val widthPx = if (depth == 0) {
                1.5f + rnd.nextFloat() * 1f
            } else {
                2.5f + rnd.nextFloat() * 1.5f
            }
            val phaseOffset = rnd.nextFloat() * (2f * PI.toFloat())
            val swayAmp = if (depth == 0) {
                0.03f + rnd.nextFloat() * 0.03f
            } else {
                0.05f + rnd.nextFloat() * 0.05f
            }
            ReedStillSeed(
                x0 = ((i + 0.5f) / reedCount) + jitter,
                heightFrac = heightFrac,
                widthPx = widthPx,
                phaseOffset = phaseOffset,
                swayAmp = swayAmp,
                colorIndex = i,
                depth = depth,
            )
        }

        val time = phase * 2f * PI.toFloat()
        val pulseMix = 0.85f + pulse * 0.15f
        paint.strokeCap = Paint.Cap.ROUND

        for (depthPass in 0..1) {
            for (reed in reeds) {
                if (reed.depth != depthPass) continue
                val baseX = reed.x0 * w
                val baseY = h
                val reedHeight = reed.heightFrac * h * pulseMix
                val sway = sin(time + reed.phaseOffset + reed.x0 * 6f) * reed.swayAmp
                val controlX = baseX + sway * w * 0.5f
                val controlY = h - reedHeight * 0.55f
                val tipSway = sin(time + reed.phaseOffset + reed.x0 * 6f + 0.3f) * reed.swayAmp * 1.6f
                val tipX = baseX + tipSway * w
                val tipY = h - reedHeight

                path.reset()
                path.moveTo(baseX, baseY)
                path.quadTo(controlX, controlY, tipX, tipY)

                val baseColor = palette.colorAt(reed.colorIndex)
                val depthFactor = if (reed.depth == 0) 0.55f else 1f
                val alpha = if (reed.depth == 0) 0.5f else 0.85f
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = reed.widthPx
                paint.color = Color.argb(
                    (alpha * 255).toInt().coerceIn(0, 255),
                    (Color.red(baseColor) * depthFactor).toInt().coerceIn(0, 255),
                    (Color.green(baseColor) * depthFactor).toInt().coerceIn(0, 255),
                    (Color.blue(baseColor) * depthFactor).toInt().coerceIn(0, 255),
                )
                canvas.drawPath(path, paint)
            }
        }
        paint.style = Paint.Style.FILL
        paint.alpha = 255
    }
}

private data class ReedStillSeed(
    val x0: Float,
    val heightFrac: Float,
    val widthPx: Float,
    val phaseOffset: Float,
    val swayAmp: Float,
    val colorIndex: Int,
    val depth: Int,
)

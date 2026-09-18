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

/** Bakes one frozen frame of "Grass in Wind" for Android Auto album art — deterministic per [generation]. */
internal object GrassStill {
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
            intArrayOf(0xFF05090F.toInt(), 0xFF101B2B.toInt()),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP,
        )
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        val bladeCount = 40
        val blades = (0 until bladeCount).map { i ->
            val depth = i % 2
            val jitter = (rnd.nextFloat() - 0.5f) * (0.6f / bladeCount)
            val heightFrac = if (depth == 0) {
                0.28f + rnd.nextFloat() * 0.17f
            } else {
                0.45f + rnd.nextFloat() * 0.27f
            }
            val widthPx = if (depth == 0) {
                2f + rnd.nextFloat() * 2f
            } else {
                4f + rnd.nextFloat() * 3f
            }
            val phaseOffset = rnd.nextFloat() * (2f * PI.toFloat())
            val swayAmp = if (depth == 0) {
                0.04f + rnd.nextFloat() * 0.05f
            } else {
                0.08f + rnd.nextFloat() * 0.08f
            }
            BladeStillSeed(
                x0 = ((i + 0.5f) / bladeCount) + jitter,
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
            for (blade in blades) {
                if (blade.depth != depthPass) continue
                val baseX = blade.x0 * w
                val baseY = h
                val bladeHeight = blade.heightFrac * h * pulseMix
                val sway = sin(time + blade.phaseOffset + blade.x0 * 6f) * blade.swayAmp
                val controlX = baseX + sway * w * 0.5f
                val controlY = h - bladeHeight * 0.55f
                val tipSway = sin(time + blade.phaseOffset + blade.x0 * 6f + 0.3f) * blade.swayAmp * 1.6f
                val tipX = baseX + tipSway * w
                val tipY = h - bladeHeight

                path.reset()
                path.moveTo(baseX, baseY)
                path.quadTo(controlX, controlY, tipX, tipY)

                val baseColor = palette.colorAt(blade.colorIndex)
                val depthFactor = if (blade.depth == 0) 0.55f else 1f
                val alpha = if (blade.depth == 0) 0.5f else 0.85f
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = blade.widthPx
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

private data class BladeStillSeed(
    val x0: Float,
    val heightFrac: Float,
    val widthPx: Float,
    val phaseOffset: Float,
    val swayAmp: Float,
    val colorIndex: Int,
    val depth: Int,
)

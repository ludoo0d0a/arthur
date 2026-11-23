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

/** Bakes one frozen frame of Train Countryside Wash for Auto/Ambient album art. */
internal object TrainCountrysideStill {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)

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
        val angle = phase
        val t01 = ((angle % (2f * PI.toFloat())) / (2f * PI.toFloat())).let { if (it < 0f) it + 1f else it }
        val wash = loopedFbm(t01, radius = 1.25f, octaves = 3, seedOffset = 17)
        val dim = 0.85f + 0.15f * pulse
        val horizonY = h * (0.48f + 0.025f * sin(angle))
        val deep = 0xFF0A0C10.toInt()
        val farField = 0xFF5A7850.toInt()
        val nearField = 0xFF2A4030.toInt()
        val dawn = mixArgb(0xFFC8D080.toInt(), palette.colorAt(0), 0.22f)
        val frame = 0xFF080A0C.toInt()

        paint.style = Paint.Style.FILL
        paint.shader = LinearGradient(
            0f, 0f, 0f, h,
            intArrayOf(deep, mixArgb(deep, farField, 0.48f), nearField, frame),
            floatArrayOf(0f, 0.48f, 0.70f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        paint.shader = LinearGradient(
            0f, horizonY - h * 0.16f, 0f, horizonY + h * 0.16f,
            intArrayOf(Color.TRANSPARENT, withAlpha(dawn, (0.12f * dim * 255).toInt()), Color.TRANSPARENT),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, horizonY - h * 0.16f, w, horizonY + h * 0.16f, paint)
        paint.shader = RadialGradient(
            w * (0.68f + 0.04f * sin(angle)), horizonY, minOf(w, h) * 0.62f,
            intArrayOf(withAlpha(dawn, (0.20f * dim * 255).toInt()), withAlpha(dawn, (0.07f * dim * 255).toInt()), Color.TRANSPARENT),
            floatArrayOf(0f, 0.45f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(w * (0.68f + 0.04f * sin(angle)), horizonY, minOf(w, h) * 0.62f, paint)
        paint.shader = null

        val hills = Path().apply {
            moveTo(0f, horizonY + h * 0.07f)
            cubicTo(w * 0.18f, horizonY - h * 0.05f, w * 0.30f, horizonY + h * 0.11f, w * 0.47f, horizonY + h * 0.035f)
            cubicTo(w * 0.65f, horizonY - h * 0.04f, w * 0.77f, horizonY + h * 0.08f, w, horizonY + h * 0.015f)
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }
        paint.color = withAlpha(farField, (0.76f * dim * 255).toInt())
        canvas.drawPath(hills, paint)

        for (layer in 0..2) {
            val top = horizonY + h * (0.10f + layer * 0.105f)
            val shift = ((t01 * (0.025f + layer * 0.02f) + layer * 0.31f) % 1f) * w
            val bandColor = when (layer) {
                0 -> mixArgb(farField, dawn, 0.10f)
                1 -> mixArgb(nearField, farField, 0.22f)
                else -> nearField
            }
            val band = Path().apply {
                moveTo(0f, top)
                cubicTo(w * 0.30f, top - h * 0.025f, w * 0.66f, top + h * 0.035f, w, top + h * 0.005f)
                lineTo(w, top + h * 0.14f)
                lineTo(0f, top + h * 0.14f)
                close()
            }
            paint.color = withAlpha(bandColor, ((0.66f - layer * 0.08f) * dim * 255).toInt())
            canvas.drawPath(band, paint)

            val treeCount = 12
            val spacing = w / treeCount
            repeat(treeCount) { index ->
                val x = (index * spacing + shift + layer * spacing * 0.43f) % (w + spacing) - spacing * 0.5f
                val rhythm = sin(index * 12.9898f + layer * 5.37f)
                val treeHeight = h * (0.045f + 0.035f * (0.5f + 0.5f * rhythm)) * (1f - layer * 0.16f)
                val baseY = top + h * 0.04f
                val treeWidth = treeHeight * 0.52f
                val tree = Path().apply {
                    moveTo(x, baseY - treeHeight)
                    lineTo(x - treeWidth * 0.5f, baseY)
                    lineTo(x + treeWidth * 0.5f, baseY)
                    close()
                    moveTo(x, baseY - treeHeight * 0.73f)
                    lineTo(x - treeWidth * 0.68f, baseY - treeHeight * 0.25f)
                    lineTo(x + treeWidth * 0.68f, baseY - treeHeight * 0.25f)
                    close()
                }
                paint.color = withAlpha(if (layer == 0) nearField else frame, ((0.52f - layer * 0.09f) * dim * 255).toInt())
                canvas.drawPath(tree, paint)
            }
        }

        paint.shader = LinearGradient(
            0f, h * 0.82f, 0f, h,
            intArrayOf(Color.TRANSPARENT, withAlpha(frame, (0.62f * dim * 255).toInt())),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, h * 0.82f, w, h, paint)
        paint.shader = null
        paint.color = withAlpha(frame, (0.24f * dim * 255).toInt())
        canvas.drawRect(0f, 0f, w * 0.025f, h, paint)
        canvas.drawRect(w * 0.975f, 0f, w, h, paint)

        val glint = (0.5f + 0.5f * sin(angle + wash * 0.4f)) * dim
        paint.shader = LinearGradient(
            0f, 0f, w, 0f,
            intArrayOf(
                Color.TRANSPARENT,
                withAlpha(Color.WHITE, (0.012f * glint * 255).toInt()),
                withAlpha(Color.WHITE, (0.02f * glint * 255).toInt()),
                Color.TRANSPARENT,
            ),
            floatArrayOf(0f, 0.48f, 0.52f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null
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

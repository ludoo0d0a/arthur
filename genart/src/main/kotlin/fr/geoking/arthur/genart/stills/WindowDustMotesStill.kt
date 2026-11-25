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
import fr.geoking.arthur.genart.seededRange
import fr.geoking.arthur.genart.seededUnit
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Bakes one frozen frame of Window Dust Motes for Auto/Ambient album art. */
internal object WindowDustMotesStill {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)
    private val ray = Path()

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
        val minDim = minOf(w, h)
        val t = ((phase % (2f * PI.toFloat())) / (2f * PI.toFloat())).let { if (it < 0f) it + 1f else it }
        val dim = 0.85f + 0.15f * pulse
        val wash = loopedFbm(t, radius = 1.35f, octaves = 2, seedOffset = 11)
        val sourceX = w * (0.16f + 0.025f * (loopedFbm(t, radius = 1.2f, seedOffset = 29) - 0.5f))
        val sourceY = h * 0.28f
        val accent = mixArgb(0xFFFFE8C0.toInt(), palette.colorAt(0), 0.22f)

        paint.shader = LinearGradient(
            0f, 0f, 0f, h,
            intArrayOf(
                brightness(0xFF1A1714.toInt(), 0.92f + 0.08f * wash),
                0xFF2A241C.toInt(),
                brightness(0xFF4A4030.toInt(), 0.92f + 0.08f * wash),
            ),
            floatArrayOf(0f, 0.58f, 1f),
            Shader.TileMode.CLAMP,
        )
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        // Diffuse window source blooms through the otherwise dim room.
        paint.shader = RadialGradient(
            sourceX, sourceY, minDim * 0.72f,
            intArrayOf(withAlpha(accent, (0.72f * dim * 255).toInt()), withAlpha(0xFFFFDFA8.toInt(), (0.38f * dim * 255).toInt()), Color.TRANSPARENT),
            floatArrayOf(0f, 0.38f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(sourceX, sourceY, minDim * 0.72f, paint)
        paint.shader = null

        // Soft-edged diagonal light shafts.
        val rayDrift = (loopedFbm(t, radius = 1.4f, octaves = 2, seedOffset = 47) - 0.5f) * minDim * 0.025f
        for (i in 0..2) {
            val topX = w * (0.13f + i * 0.13f) + rayDrift
            val bottomX = w * (0.56f + i * 0.18f) + rayDrift
            val spread = w * (0.14f + i * 0.025f)
            ray.reset()
            ray.moveTo(topX, h * 0.12f)
            ray.lineTo(topX + spread, h * 0.12f)
            ray.lineTo(bottomX + spread * 1.8f, h * 0.92f)
            ray.lineTo(bottomX, h * 0.92f)
            ray.close()
            paint.shader = LinearGradient(
                topX, 0f, bottomX + spread, 0f,
                intArrayOf(Color.TRANSPARENT, withAlpha(accent, ((0.14f - i * 0.025f) * dim * 255).toInt()), Color.TRANSPARENT),
                floatArrayOf(0f, 0.5f, 1f),
                Shader.TileMode.CLAMP,
            )
            canvas.drawPath(ray, paint)
        }
        paint.shader = null

        // Window silhouette and crossbars, subdued by the luminous haze.
        paint.color = withAlpha(0xFF17130F.toInt(), (0.58f * dim * 255).toInt())
        canvas.drawRect(0f, h * 0.06f, w * 0.07f, h * 0.58f, paint)
        canvas.drawRect(w * 0.25f, h * 0.06f, w * 0.268f, h * 0.58f, paint)
        canvas.drawRect(0f, h * 0.30f, w * 0.268f, h * 0.318f, paint)

        // Small suspended particles drift gently through the bright air.
        for (i in 0 until 11) {
            val baseX = seededRange(i * 17 + 3, 0.20f, 0.92f)
            val baseY = seededRange(i * 29 + 7, 0.10f, 0.92f)
            val drift = seededRange(i * 53 + 13, 0.012f, 0.035f)
            val phaseOffset = seededRange(i * 67 + 19, 0f, 2f * PI.toFloat())
            val a = t * 2f * PI.toFloat() + phaseOffset
            val field = loopedFbm(t, radius = 1.2f, octaves = 2, seedOffset = i * 97 + 5)
            val x = (baseX + drift * sin(a) + (field - 0.5f) * 0.025f) * w
            val y = (baseY + drift * cos(a * 0.7f)) * h
            val radius = minDim * seededRange(i * 41 + 11, 0.0035f, 0.0095f) * (0.8f + 0.4f * field)
            val proximity = (1f - (x / w - y / h * 0.35f).coerceIn(0f, 1f)).coerceIn(0.25f, 1f)
            val mote = mixArgb(accent, palette.colorAt(i), 0.24f)

            paint.shader = RadialGradient(
                x, y, radius * 2.8f,
                intArrayOf(withAlpha(mote, (0.88f * proximity * dim * 255).toInt()), Color.TRANSPARENT),
                floatArrayOf(0f, 1f),
                Shader.TileMode.CLAMP,
            )
            canvas.drawCircle(x, y, radius * 2.8f, paint)
            paint.shader = null
            paint.color = withAlpha(mote, (0.92f * proximity * dim * 255).toInt())
            canvas.drawCircle(x, y, radius * 0.45f, paint)
        }
    }

    private fun mixArgb(a: Int, b: Int, t: Float): Int {
        val u = t.coerceIn(0f, 1f)
        val r = (Color.red(a) + (Color.red(b) - Color.red(a)) * u).toInt().coerceIn(0, 255)
        val g = (Color.green(a) + (Color.green(b) - Color.green(a)) * u).toInt().coerceIn(0, 255)
        val bl = (Color.blue(a) + (Color.blue(b) - Color.blue(a)) * u).toInt().coerceIn(0, 255)
        return Color.rgb(r, g, bl)
    }

    private fun brightness(color: Int, factor: Float): Int = Color.rgb(
        (Color.red(color) * factor).toInt().coerceIn(0, 255),
        (Color.green(color) * factor).toInt().coerceIn(0, 255),
        (Color.blue(color) * factor).toInt().coerceIn(0, 255),
    )

    private fun withAlpha(rgb: Int, alpha: Int): Int =
        Color.argb(alpha.coerceIn(0, 255), Color.red(rgb), Color.green(rgb), Color.blue(rgb))
}

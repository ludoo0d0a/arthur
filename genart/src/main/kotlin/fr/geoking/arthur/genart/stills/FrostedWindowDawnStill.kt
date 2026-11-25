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

/** Bakes a frost-laced window catching the first warm light of dawn. */
internal object FrostedWindowDawnStill {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)
    private val crystalPath = Path()

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
        val shortSide = size.toFloat()
        val t01 = ((phase % (2f * PI.toFloat())) / (2f * PI.toFloat())).let { if (it < 0f) it + 1f else it }
        val dawn = loopedFbm(t01, radius = 1.2f, octaves = 2, seedOffset = 29)
        val glowX = w * (0.28f + dawn * 0.44f)
        val glowY = h * (0.23f + loopedFbm(t01, radius = 1.4f, seedOffset = 47) * 0.16f)
        val dim = 0.85f + 0.15f * pulse
        val warm = mixArgb(0xFFFFE6C0.toInt(), palette.colorAt(0), 0.22f)

        paint.style = Paint.Style.FILL
        paint.shader = LinearGradient(
            0f, 0f, 0f, h,
            intArrayOf(0xFF667F9B.toInt(), 0xFFE8F0F8.toInt(), 0xFF9BB4C8.toInt()),
            floatArrayOf(0f, 0.54f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        paint.shader = RadialGradient(
            glowX, glowY, shortSide * 0.80f,
            intArrayOf(withAlpha(warm, (0.72f * dim * 255).toInt()), withAlpha(0xFFFFDDB0.toInt(), (0.38f * dim * 255).toInt()), Color.TRANSPARENT),
            floatArrayOf(0f, 0.32f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(glowX, glowY, shortSide * 0.80f, paint)
        paint.shader = null

        paint.shader = LinearGradient(
            0f, 0f, 0f, h,
            intArrayOf(Color.TRANSPARENT, withAlpha(0xFFFFE3BE.toInt(), (0.12f * dim * 255).toInt()), withAlpha(0xFF6B8195.toInt(), (0.28f * dim * 255).toInt())),
            floatArrayOf(0f, 0.46f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        // Feathered ice along the edges frames a clear view of the dawn.
        paint.shader = LinearGradient(
            0f, 0f, w, 0f,
            intArrayOf(withAlpha(Color.WHITE, (0.82f * dim * 255).toInt()), withAlpha(0xFFE8F0F8.toInt(), (0.44f * dim * 255).toInt()), Color.TRANSPARENT, Color.TRANSPARENT, withAlpha(0xFFE8F0F8.toInt(), (0.44f * dim * 255).toInt()), withAlpha(Color.WHITE, (0.82f * dim * 255).toInt())),
            floatArrayOf(0f, 0.10f, 0.26f, 0.74f, 0.90f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        paint.shader = LinearGradient(
            0f, 0f, 0f, h,
            intArrayOf(withAlpha(Color.WHITE, (0.66f * dim * 255).toInt()), withAlpha(0xFFE8F0F8.toInt(), (0.32f * dim * 255).toInt()), Color.TRANSPARENT, Color.TRANSPARENT, withAlpha(0xFFE8F0F8.toInt(), (0.38f * dim * 255).toInt()), withAlpha(Color.WHITE, (0.78f * dim * 255).toInt())),
            floatArrayOf(0f, 0.12f, 0.32f, 0.70f, 0.90f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        // Ice fern dendrites radiate from irregular crystals around the panes.
        for (i in 0 until 9) {
            val x = unit(i * 31 + 3) * w
            val y = unit(i * 37 + 7) * h
            val branch = shortSide * (0.06f + unit(i * 41 + 11) * 0.08f)
            val shimmer = 0.75f + 0.25f * loopedFbm(t01, radius = 1.1f, octaves = 2, seedOffset = i * 97 + 5)
            val alpha = (0.40f * shimmer * dim * 255).toInt()
            val direction = if (i % 2 == 0) 1f else -1f
            crystalPath.reset()
            crystalPath.moveTo(x, y + branch)
            crystalPath.lineTo(x, y - branch)
            crystalPath.moveTo(x, y - branch * 0.15f)
            crystalPath.lineTo(x + direction * branch * 0.50f, y - branch * 0.58f)
            crystalPath.moveTo(x, y - branch * 0.15f)
            crystalPath.lineTo(x - direction * branch * 0.50f, y - branch * 0.58f)
            crystalPath.moveTo(x, y + branch * 0.28f)
            crystalPath.lineTo(x + direction * branch * 0.34f, y - branch * 0.02f)
            crystalPath.moveTo(x, y + branch * 0.28f)
            crystalPath.lineTo(x - direction * branch * 0.34f, y - branch * 0.02f)
            paint.color = withAlpha(Color.WHITE, alpha)
            paint.strokeWidth = (shortSide * 0.0035f).coerceAtLeast(1.2f)
            paint.strokeCap = Paint.Cap.ROUND
            paint.style = Paint.Style.STROKE
            canvas.drawPath(crystalPath, paint)
            paint.style = Paint.Style.FILL
            paint.color = withAlpha(Color.WHITE, (alpha * 0.70f).toInt())
            canvas.drawCircle(x, y, shortSide * 0.007f, paint)
        }

        // Muntins separate four softly reflective panes.
        val bar = shortSide * 0.048f
        paint.color = withAlpha(0xFF63798D.toInt(), (0.72f * dim * 255).toInt())
        canvas.drawRect(w * 0.5f - bar * 0.5f, 0f, w * 0.5f + bar * 0.5f, h, paint)
        canvas.drawRect(0f, h * 0.52f - bar * 0.5f, w, h * 0.52f + bar * 0.5f, paint)
        paint.color = withAlpha(warm, (0.32f * dim * 255).toInt())
        canvas.drawRect(w * 0.5f + bar * 0.5f, 0f, w * 0.5f + bar * 0.62f, h, paint)
        canvas.drawRect(0f, h * 0.52f + bar * 0.5f, w, h * 0.52f + bar * 0.62f, paint)

        // Condensation pinpoints shimmer gently across the glass.
        paint.color = Color.WHITE
        for (i in 0 until 12) {
            val x = (unit(i * 53 + 19) + (loopedFbm(t01, radius = 1.3f, octaves = 2, seedOffset = i * 79 + 31) - 0.5f) * 0.025f) * w
            val y = (unit(i * 67 + 23) + 0.012f * sin(t01 * 2f * PI.toFloat() + i)) * h
            val sparkle = 0.5f + 0.5f * loopedFbm(t01, radius = 1.4f, seedOffset = i * 89 + 37)
            paint.color = withAlpha(Color.WHITE, ((0.14f + 0.24f * sparkle) * dim * 255).toInt())
            canvas.drawCircle(x, y, shortSide * 0.004f, paint)
        }
        paint.shader = null
        paint.style = Paint.Style.FILL
    }

    private fun unit(seed: Int): Float = loopedFbm(0f, radius = 1f, octaves = 1, seedOffset = seed)

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
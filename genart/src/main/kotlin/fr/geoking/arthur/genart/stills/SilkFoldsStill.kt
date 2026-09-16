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

/** Bakes one frozen frame of Silk Folds for Auto/Ambient album art. */
internal object SilkFoldsStill {
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
        val bg = palette.colorAt(0)
        paint.shader = LinearGradient(
            0f, 0f, 0f, h,
            intArrayOf(
                Color.argb(255, (Color.red(bg) * 0.35f).toInt(), (Color.green(bg) * 0.35f).toInt(), (Color.blue(bg) * 0.35f).toInt()),
                0xFF030208.toInt(),
            ),
            null,
            Shader.TileMode.CLAMP,
        )
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        val time = ((phase / (2f * PI.toFloat())) + rotationDeg / 360f)
        val loop = ((time % 1f) + 1f) % 1f
        val dim = 0.65f + 0.35f * pulse
        val folds = (0 until 7).map { i ->
            FoldSeed(
                baseY = 0.06f + rnd.nextFloat() * 0.86f,
                amplitude = 0.018f + rnd.nextFloat() * 0.06f,
                cycles = 0.8f + rnd.nextFloat() * 2.0f,
                cycles2 = 1.6f + rnd.nextFloat() * 2.4f,
                amp2Frac = 0.15f + rnd.nextFloat() * 0.3f,
                foldPhase = rnd.nextFloat() * 2f * PI.toFloat(),
                speedMul = 0.35f + rnd.nextFloat() * 0.8f,
                thickness = 0.05f + rnd.nextFloat() * 0.11f,
                colorIndex = i,
                colorMix = rnd.nextFloat(),
                alpha = 0.55f + rnd.nextFloat() * 0.35f,
            )
        }.sortedBy { it.baseY }

        val segments = 40
        val shadowPx = h * 0.012f
        val t = loop * 2f * PI.toFloat()
        folds.forEach { fold ->
            val band = buildBand(fold, w, h, t, segments)
            paint.color = Color.argb((0.28f * dim * 255).toInt().coerceIn(0, 255), 0, 0, 0)
            path.reset()
            path.addPath(band)
            path.offset(0f, shadowPx)
            canvas.drawPath(path, paint)

            val tint = mixArgb(palette.colorAt(fold.colorIndex), palette.colorAt(fold.colorIndex + 1), fold.colorMix)
            paint.color = Color.argb(
                (fold.alpha * 0.42f * dim * 255).toInt().coerceIn(0, 255),
                Color.red(tint), Color.green(tint), Color.blue(tint),
            )
            canvas.drawPath(band, paint)
        }
        paint.alpha = 255
    }

    private fun waveY(fold: FoldSeed, x: Float, w: Float, h: Float, time: Float, ampScale: Float): Float {
        val amp = fold.amplitude * h * ampScale
        val freq = fold.cycles * 2f * PI.toFloat() / w.coerceAtLeast(1f)
        val freq2 = fold.cycles2 * 2f * PI.toFloat() / w.coerceAtLeast(1f)
        return fold.baseY * h +
            amp * sin(x * freq + time * fold.speedMul + fold.foldPhase) +
            amp * fold.amp2Frac * sin(x * freq2 + time * fold.speedMul * 0.7f + fold.foldPhase * 1.3f)
    }

    private fun buildBand(fold: FoldSeed, w: Float, h: Float, time: Float, segments: Int): Path {
        val out = Path()
        val thick = fold.thickness * h
        for (k in 0..segments) {
            val x = (k / segments.toFloat()) * w
            val y = waveY(fold, x, w, h, time, 1f)
            if (k == 0) out.moveTo(x, y) else out.lineTo(x, y)
        }
        for (k in segments downTo 0) {
            val x = (k / segments.toFloat()) * w
            val y = waveY(fold, x, w, h, time, 0.7f) + thick
            out.lineTo(x, y)
        }
        out.close()
        return out
    }

    private fun mixArgb(a: Int, b: Int, t: Float): Int {
        val u = t.coerceIn(0f, 1f)
        return Color.argb(
            255,
            (Color.red(a) + (Color.red(b) - Color.red(a)) * u).toInt().coerceIn(0, 255),
            (Color.green(a) + (Color.green(b) - Color.green(a)) * u).toInt().coerceIn(0, 255),
            (Color.blue(a) + (Color.blue(b) - Color.blue(a)) * u).toInt().coerceIn(0, 255),
        )
    }

    private data class FoldSeed(
        val baseY: Float,
        val amplitude: Float,
        val cycles: Float,
        val cycles2: Float,
        val amp2Frac: Float,
        val foldPhase: Float,
        val speedMul: Float,
        val thickness: Float,
        val colorIndex: Int,
        val colorMix: Float,
        val alpha: Float,
    )
}

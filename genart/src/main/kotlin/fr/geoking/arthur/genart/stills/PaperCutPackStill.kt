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
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

/** Bakes one frozen frame of Paper-Cut Pack for Auto/Ambient album art. */
internal object PaperCutPackStill {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)
    private val rect = RectF()

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
        val minDim = min(w, h)
        val gap = palette.colorAt(3)
        paint.shader = RadialGradient(
            w * 0.5f, h * 0.45f, maxOf(w, h) * 0.9f,
            intArrayOf(
                Color.argb(255, (Color.red(gap) * 0.35f).toInt(), (Color.green(gap) * 0.35f).toInt(), (Color.blue(gap) * 0.35f).toInt()),
                0xFF0A0408.toInt(),
            ),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP,
        )
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        val time = ((phase / (2f * PI.toFloat())) + rotationDeg / 360f)
        val loop = ((time % 1f) + 1f) % 1f
        val dim = 0.65f + 0.35f * pulse
        val pieces = (0 until 22).map { i ->
            PackPiece(
                x = rnd.nextFloat(),
                y = rnd.nextFloat(),
                wFrac = 0.12f + rnd.nextFloat() * 0.3f,
                hFrac = 0.08f + rnd.nextFloat() * 0.2f,
                rot = (rnd.nextFloat() - 0.5f) * 1.1f,
                corner = 0.38f + rnd.nextFloat() * 0.15f,
                colorIndex = i,
                colorMix = rnd.nextFloat(),
                breathPhase = rnd.nextFloat() * 2f * PI.toFloat(),
                breathAmp = 0.02f + rnd.nextFloat() * 0.05f,
                z = rnd.nextFloat(),
                alpha = 0.72f + rnd.nextFloat() * 0.26f,
            )
        }.sortedBy { it.z }

        val shadowPx = minDim * 0.014f
        pieces.forEach { piece ->
            val breath = 1f + piece.breathAmp * sin(loop * 2f * PI.toFloat() + piece.breathPhase)
            val cx = piece.x * w
            val cy = piece.y * h
            val bw = piece.wFrac * minDim * breath
            val bh = piece.hFrac * minDim * breath
            val tint = mixArgb(palette.colorAt(piece.colorIndex), palette.colorAt(piece.colorIndex + 1), piece.colorMix)
            canvas.save()
            canvas.rotate(Math.toDegrees(piece.rot.toDouble()).toFloat(), cx, cy)
            paint.color = Color.argb((0.38f * dim * 255).toInt().coerceIn(0, 255), 0, 0, 0)
            rect.set(
                cx - bw * 0.5f + shadowPx * 0.45f,
                cy - bh * 0.5f + shadowPx,
                cx + bw * 0.5f + shadowPx * 0.45f,
                cy + bh * 0.5f + shadowPx,
            )
            canvas.drawRoundRect(rect, bw * piece.corner, bh * piece.corner, paint)
            paint.shader = LinearGradient(
                cx - bw * 0.4f, cy - bh * 0.45f,
                cx + bw * 0.35f, cy + bh * 0.45f,
                intArrayOf(
                    mixArgb(tint, Color.WHITE, 0.12f),
                    tint,
                    mixArgb(tint, Color.BLACK, 0.18f),
                ),
                null,
                Shader.TileMode.CLAMP,
            )
            paint.alpha = (piece.alpha * dim * 255).toInt().coerceIn(0, 255)
            rect.set(cx - bw * 0.5f, cy - bh * 0.5f, cx + bw * 0.5f, cy + bh * 0.5f)
            canvas.drawRoundRect(rect, bw * piece.corner, bh * piece.corner, paint)
            paint.shader = null
            paint.alpha = 255
            canvas.restore()
        }
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

    private data class PackPiece(
        val x: Float,
        val y: Float,
        val wFrac: Float,
        val hFrac: Float,
        val rot: Float,
        val corner: Float,
        val colorIndex: Int,
        val colorMix: Float,
        val breathPhase: Float,
        val breathAmp: Float,
        val z: Float,
        val alpha: Float,
    )
}

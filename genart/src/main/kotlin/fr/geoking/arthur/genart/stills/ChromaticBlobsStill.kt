package fr.geoking.arthur.genart.stills

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import fr.geoking.arthur.genart.AnimationPalette
import kotlin.math.PI
import kotlin.math.sin

/** Bakes one frozen frame of Chromatic Blobs for Auto/Ambient album art. */
internal object ChromaticBlobsStill {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

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

        paint.color = 0xFF07040B.toInt()
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w, h, paint)

        val count = 7
        val time = (phase / (2f * PI.toFloat())) + (rotationDeg / 360f)
        val dim = 0.65f + 0.35f * pulse

        val blobs = List(count) { i ->
            BlobStillData(
                x0 = 0.2f + (i * 0.12f) % 0.6f,
                y0 = 0.2f + (i * 0.15f) % 0.6f,
                rx = 0.18f + (i % 3) * 0.05f,
                ry = 0.18f + ((i + 1) % 3) * 0.05f,
                freqX = 0.8f + (i % 4) * 0.3f,
                freqY = 0.8f + ((i + 2) % 4) * 0.3f,
                phaseX = i * 0.9f,
                phaseY = i * 1.3f,
            )
        }

        // Draw Shadow Base
        blobs.forEach { blob ->
            val offsetX = (sin(time * 2f * PI.toFloat() * blob.freqX + blob.phaseX) * 0.5f) * blob.rx * w
            val offsetY = (sin(time * 2f * PI.toFloat() * blob.freqY + blob.phaseY) * 0.5f) * blob.ry * h
            val x = blob.x0 * w + offsetX
            val y = blob.y0 * h + offsetY
            val r = (blob.rx + blob.ry) * 0.5f * minDim

            val shadowX = x + r * 0.15f
            val shadowY = y + r * 0.18f
            paint.shader = RadialGradient(
                shadowX, shadowY, r * 1.4f,
                intArrayOf(
                    Color.argb((0.5f * dim * 255).toInt(), 0, 0, 0),
                    Color.TRANSPARENT,
                ),
                floatArrayOf(0f, 1f),
                Shader.TileMode.CLAMP
            )
            canvas.drawCircle(shadowX, shadowY, r * 1.4f, paint)
        }

        // Draw Liquid Blobs
        blobs.forEachIndexed { i, blob ->
            val offsetX = (sin(time * 2f * PI.toFloat() * blob.freqX + blob.phaseX) * 0.5f) * blob.rx * w
            val offsetY = (sin(time * 2f * PI.toFloat() * blob.freqY + blob.phaseY) * 0.5f) * blob.ry * h
            val x = blob.x0 * w + offsetX
            val y = blob.y0 * h + offsetY
            val r = (blob.rx + blob.ry) * 0.5f * minDim

            val color = palette.colorAt(i)
            val alpha1 = (0.85f * dim * 255).toInt().coerceIn(0, 255)
            val alpha2 = (0.45f * dim * 255).toInt().coerceIn(0, 255)
            val alpha3 = (0.1f * dim * 255).toInt().coerceIn(0, 255)

            paint.shader = RadialGradient(
                x, y, r,
                intArrayOf(
                    Color.argb(alpha1, Color.red(color), Color.green(color), Color.blue(color)),
                    Color.argb(alpha2, Color.red(color), Color.green(color), Color.blue(color)),
                    Color.argb(alpha3, Color.red(color), Color.green(color), Color.blue(color)),
                    Color.TRANSPARENT,
                ),
                floatArrayOf(0f, 0.45f, 0.8f, 1f),
                Shader.TileMode.CLAMP
            )
            canvas.drawCircle(x, y, r, paint)
            paint.shader = null
        }
    }

    private data class BlobStillData(
        val x0: Float,
        val y0: Float,
        val rx: Float,
        val ry: Float,
        val freqX: Float,
        val freqY: Float,
        val phaseX: Float,
        val phaseY: Float,
    )
}

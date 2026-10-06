package fr.geoking.arthur.genart.stills

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import fr.geoking.arthur.genart.AnimationPalette
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/** Bakes one frozen frame of Canyon Light Cut for Auto/Ambient album art. */
internal object CanyonLightStill {
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
        val minDim = minOf(w, h)
        val dim = 0.65f + 0.35f * pulse
        val time = phase + rotationDeg * PI.toFloat() / 180f * 0.15f

        val sky = palette.colorAt(0)
        paint.shader = LinearGradient(
            0f, 0f, 0f, h,
            intArrayOf(
                Color.argb(255, Color.red(sky) / 3, Color.green(sky) / 3, Color.blue(sky) / 3),
                0xFF1A0E08.toInt(),
                0xFF080402.toInt(),
            ),
            floatArrayOf(0f, 0.45f, 1f),
            Shader.TileMode.CLAMP,
        )
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        val planeCount = 3
        val planes = List(planeCount) { i ->
            val depthFrac = i / (planeCount - 1f)
            CanyonLightPlaneStillSeed(
                depthFrac = depthFrac,
                leftInset = 0.08f + depthFrac * 0.18f + (rnd.nextFloat() - 0.5f) * 0.04f,
                rightInset = 0.08f + depthFrac * 0.18f + (rnd.nextFloat() - 0.5f) * 0.04f,
                ridgeAmp = 0.02f + rnd.nextFloat() * 0.035f,
                ridgeFreq = 0.008f + rnd.nextFloat() * 0.01f,
                ridgePhase = rnd.nextFloat() * 2f * PI.toFloat(),
                parallax = (0.008f + rnd.nextFloat() * 0.02f) * (0.4f + depthFrac),
                colorIndex = i,
            )
        }

        val segments = (h / 10f).toInt().coerceIn(20, 120)
        val step = h / segments

        planes.forEach { plane ->
            val shift = sin(time * 0.35f + plane.ridgePhase) * plane.parallax
            val tint = palette.colorAt(plane.colorIndex)
            val brightness = 0.35f + plane.depthFrac * 0.55f
            val r = ((Color.red(tint) + 58) / 2 * brightness).toInt().coerceIn(0, 255)
            val g = ((Color.green(tint) + 34) / 2 * brightness).toInt().coerceIn(0, 255)
            val b = ((Color.blue(tint) + 24) / 2 * brightness).toInt().coerceIn(0, 255)
            val alpha = ((0.55f + plane.depthFrac * 0.4f) * 255).toInt().coerceIn(100, 242)
            paint.color = Color.argb(alpha, r, g, b)

            val leftBase = (plane.leftInset + shift) * w
            path.reset()
            path.moveTo(0f, 0f)
            path.lineTo(leftBase, 0f)
            for (s in 1..segments) {
                val y = s * step
                val ridge = sin(y * plane.ridgeFreq + plane.ridgePhase) * plane.ridgeAmp * w
                path.lineTo(leftBase + ridge, y)
            }
            path.lineTo(0f, h)
            path.close()
            canvas.drawPath(path, paint)

            val rightBase = (1f - plane.rightInset - shift) * w
            path.reset()
            path.moveTo(w, 0f)
            path.lineTo(rightBase, 0f)
            for (s in 1..segments) {
                val y = s * step
                val ridge = sin(y * plane.ridgeFreq + plane.ridgePhase + 1.7f) * plane.ridgeAmp * w
                path.lineTo(rightBase - ridge, y)
            }
            path.lineTo(w, h)
            path.close()
            canvas.drawPath(path, paint)
        }

        val nearest = planes.last()
        val gorgeLeft = (nearest.leftInset + 0.06f) * w
        val gorgeRight = (1f - nearest.rightInset - 0.06f) * w
        val beamY = h * (0.38f + 0.04f * sin(time * 0.25f))
        val beamHalf = h * (0.055f + 0.012f * sin(time * 0.4f))
        val beamTint = palette.colorAt(0)
        val br = (Color.red(beamTint) + 245) / 2
        val bg = (Color.green(beamTint) + 215) / 2
        val bb = (Color.blue(beamTint) + 138) / 2

        path.reset()
        path.moveTo(gorgeLeft, beamY - beamHalf * 0.35f)
        path.lineTo(gorgeRight, beamY - beamHalf)
        path.lineTo(gorgeRight, beamY + beamHalf)
        path.lineTo(gorgeLeft, beamY + beamHalf * 0.35f)
        path.close()
        paint.shader = LinearGradient(
            gorgeLeft, beamY, gorgeRight, beamY,
            intArrayOf(
                Color.argb((0.08f * dim * 255).toInt().coerceIn(0, 255), br, bg, bb),
                Color.argb((0.32f * dim * 255).toInt().coerceIn(0, 255), br, bg, bb),
                Color.argb((0.18f * dim * 255).toInt().coerceIn(0, 255), br, bg, bb),
                Color.TRANSPARENT,
            ),
            floatArrayOf(0f, 0.35f, 0.7f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawPath(path, paint)
        paint.shader = null

        val poolR = (minDim * 0.14f).coerceAtLeast(1f)
        paint.shader = RadialGradient(
            gorgeRight - minDim * 0.02f, beamY, poolR,
            intArrayOf(
                Color.argb((0.28f * dim * 255).toInt().coerceIn(0, 255), 255, 255, 255),
                Color.argb((0.2f * dim * 255).toInt().coerceIn(0, 255), br, bg, bb),
                Color.TRANSPARENT,
            ),
            floatArrayOf(0f, 0.35f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(gorgeRight - minDim * 0.02f, beamY, poolR, paint)
        paint.shader = null

        val dustCount = 48
        for (j in 0 until dustCount) {
            val along = 0.12f + rnd.nextFloat() * 0.8f
            val side = -0.85f + rnd.nextFloat() * 1.7f
            val twinkle = 0.35f + 0.65f * rnd.nextFloat()
            val px = gorgeLeft + (gorgeRight - gorgeLeft) * along
            val py = beamY + side * beamHalf * (0.5f + along * 0.5f)
            val moteR = (minDim * (0.0015f + rnd.nextFloat() * 0.0035f) * (0.7f + 0.5f * twinkle))
                .coerceAtLeast(0.5f)
            paint.color = Color.argb(
                (0.5f * twinkle * dim * 255).toInt().coerceIn(0, 255),
                (br + 255) / 2,
                (bg + 255) / 2,
                (bb + 255) / 2,
            )
            canvas.drawCircle(px, py, moteR, paint)
        }

        paint.shader = LinearGradient(
            0f, h * 0.72f, 0f, h,
            Color.TRANSPARENT,
            Color.argb((0.65f * dim * 255).toInt().coerceIn(0, 255), 4, 2, 1),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, h * 0.72f, w, h, paint)
        paint.shader = null
        paint.alpha = 255
    }

    private data class CanyonLightPlaneStillSeed(
        val depthFrac: Float,
        val leftInset: Float,
        val rightInset: Float,
        val ridgeAmp: Float,
        val ridgeFreq: Float,
        val ridgePhase: Float,
        val parallax: Float,
        val colorIndex: Int,
    )
}

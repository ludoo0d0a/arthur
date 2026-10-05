package fr.geoking.arthur.genart.stills

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RadialGradient
import android.graphics.Shader
import fr.geoking.arthur.genart.AnimationPalette
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** Bakes one frozen frame of Crossing Spotlights for Auto/Ambient album art. */
internal object CrossingSpotlightsStill {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)
    private val path = Path()
    private val plusMode = PorterDuffXfermode(PorterDuff.Mode.ADD)

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

        paint.xfermode = null
        paint.shader = RadialGradient(
            w * 0.5f, h * 0.35f, (minDim * 1.1f).coerceAtLeast(1f),
            intArrayOf(0xFF1A1C12.toInt(), 0xFF080A06.toInt(), 0xFF020302.toInt()),
            floatArrayOf(0f, 0.55f, 1f),
            Shader.TileMode.CLAMP,
        )
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        val time = phase + rotationDeg * PI.toFloat() / 180f * 0.2f
        val dim = 0.65f + 0.35f * pulse
        val spotCount = 3 + rnd.nextInt(3)

        data class Spot(
            val ox: Float,
            val oy: Float,
            val angle: Float,
            val halfWidth: Float,
            val reach: Float,
            val swayAmp: Float,
            val swayFreq: Float,
            val alpha: Float,
            val colorIndex: Int,
        )

        val spots = List(spotCount) { i ->
            Spot(
                ox = -0.05f + rnd.nextFloat() * 1.1f,
                oy = -0.12f + rnd.nextFloat() * 0.47f,
                angle = 0.15f + rnd.nextFloat() * (PI.toFloat() - 0.3f),
                halfWidth = 0.08f + rnd.nextFloat() * 0.1f,
                reach = 0.85f + rnd.nextFloat() * 0.5f,
                swayAmp = 0.02f + rnd.nextFloat() * 0.04f,
                swayFreq = 0.2f + rnd.nextFloat() * 0.35f,
                alpha = 0.18f + rnd.nextFloat() * 0.2f,
                colorIndex = i,
            )
        }

        paint.xfermode = plusMode
        spots.forEach { spot ->
            val sway = spot.swayAmp * sin(time * spot.swayFreq)
            val a = spot.angle + sway
            val ox = spot.ox * w
            val oy = spot.oy * h
            val reach = spot.reach * h
            val leftX = ox + sin(a - spot.halfWidth) * reach
            val leftY = oy + cos(a - spot.halfWidth) * reach
            val rightX = ox + sin(a + spot.halfWidth) * reach
            val rightY = oy + cos(a + spot.halfWidth) * reach
            val midX = (leftX + rightX) * 0.5f
            val midY = (leftY + rightY) * 0.5f
            val tint = palette.colorAt(spot.colorIndex)
            val r = Color.red(tint)
            val g = Color.green(tint)
            val b = Color.blue(tint)
            path.reset()
            path.moveTo(ox, oy)
            path.lineTo(leftX, leftY)
            path.lineTo(rightX, rightY)
            path.close()
            paint.shader = LinearGradient(
                ox, oy, midX, midY,
                intArrayOf(
                    Color.argb((spot.alpha * dim * 255).toInt().coerceIn(0, 255), r, g, b),
                    Color.argb((spot.alpha * 0.35f * dim * 255).toInt().coerceIn(0, 255), r, g, b),
                    Color.TRANSPARENT,
                ),
                floatArrayOf(0f, 0.45f, 1f),
                Shader.TileMode.CLAMP,
            )
            canvas.drawPath(path, paint)
            paint.shader = null

            val poolR = (minDim * (0.12f + spot.halfWidth * 0.55f)).coerceAtLeast(1f)
            paint.shader = RadialGradient(
                midX, midY, poolR,
                intArrayOf(
                    Color.argb((0.22f * dim * 255).toInt().coerceIn(0, 255), 255, 255, 255),
                    Color.argb((0.28f * dim * 255).toInt().coerceIn(0, 255), r, g, b),
                    Color.TRANSPARENT,
                ),
                floatArrayOf(0f, 0.35f, 1f),
                Shader.TileMode.CLAMP,
            )
            canvas.drawCircle(midX, midY, poolR, paint)
            paint.shader = null

            val sourceR = (minDim * 0.06f).coerceAtLeast(1f)
            paint.shader = RadialGradient(
                ox, oy, sourceR,
                Color.argb((0.45f * dim * 255).toInt().coerceIn(0, 255), 255, 255, 255),
                Color.TRANSPARENT,
                Shader.TileMode.CLAMP,
            )
            canvas.drawCircle(ox, oy, sourceR, paint)
            paint.shader = null
        }

        val dustCount = 50 + rnd.nextInt(40)
        for (j in 0 until dustCount) {
            val spot = spots[rnd.nextInt(spots.size)]
            val sway = spot.swayAmp * sin(time * spot.swayFreq)
            val a = spot.angle + sway
            val along = 0.12f + rnd.nextFloat() * 0.83f
            val side = -0.85f + rnd.nextFloat() * 1.7f
            val ox = spot.ox * w
            val oy = spot.oy * h
            val reach = spot.reach * h
            val sideA = a + spot.halfWidth * along * side
            val px = ox + sin(sideA) * reach * along
            val py = oy + cos(sideA) * reach * along
            val twinkle = 0.35f + 0.65f * rnd.nextFloat()
            val tint = palette.colorAt(spot.colorIndex)
            val r = ((Color.red(tint) + 255) / 2)
            val g = ((Color.green(tint) + 255) / 2)
            val b = ((Color.blue(tint) + 255) / 2)
            val moteR = (minDim * (0.0015f + rnd.nextFloat() * 0.004f) * (0.7f + 0.5f * twinkle))
                .coerceAtLeast(0.6f)
            paint.color = Color.argb((0.55f * twinkle * dim * 255).toInt().coerceIn(0, 255), r, g, b)
            canvas.drawCircle(px, py, moteR, paint)
        }

        paint.xfermode = null
        paint.alpha = 255
        paint.shader = null
    }
}

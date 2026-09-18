package fr.geoking.arthur.genart.stills

import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import fr.geoking.arthur.genart.AnimationPalette
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

private const val STAR_COUNT = 140
private const val ARM_COUNT = 3
private const val MAX_THETA = 4f * PI.toFloat()
private const val SPIRAL_B = 0.3f
private const val MAX_RADIUS_FRAC = 0.46f

/** Bakes one frozen frame of Spiral Galaxy Drift for Auto/Ambient album art. */
internal object SpiralGalaxyStill {
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
        val rnd = Random(generation)
        val w = size.toFloat()
        val h = size.toFloat()
        val minDim = w.coerceAtMost(h)
        val cx = w / 2f
        val cy = h / 2f

        paint.maskFilter = null
        paint.color = 0xFF03020A.toInt()
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w, h, paint)

        val coreRadius = minDim * 0.3f * (0.9f + 0.1f * pulse)
        val core = palette.colorAt(0)
        paint.maskFilter = BlurMaskFilter(minDim * 0.08f, BlurMaskFilter.Blur.NORMAL)
        paint.shader = RadialGradient(
            cx, cy, coreRadius.coerceAtLeast(1f),
            intArrayOf(
                Color.argb((0.5f * 255).toInt(), 255, 255, 255),
                Color.argb((0.35f * 255).toInt(), Color.red(core), Color.green(core), Color.blue(core)),
                Color.TRANSPARENT,
            ),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(cx, cy, coreRadius, paint)
        paint.shader = null
        paint.maskFilter = null

        val rotAngle = phase + rotationDeg * PI.toFloat() / 180f
        for (i in 0 until STAR_COUNT) {
            val armIndex = i % ARM_COUNT
            val armOffset = armIndex * (2f * PI.toFloat() / ARM_COUNT)
            val u = rnd.nextFloat()
            val theta = u * u * MAX_THETA
            val radialJitter = -0.025f + rnd.nextFloat() * 0.05f
            val angleJitter = -0.06f + rnd.nextFloat() * 0.12f
            val sizeUnit = rnd.nextFloat()
            val alphaUnit = 0.35f + rnd.nextFloat() * 0.65f
            val angle = theta + armOffset + angleJitter + rotAngle
            val rNorm = exp(SPIRAL_B * theta) / exp(SPIRAL_B * MAX_THETA)
            val r = (rNorm + radialJitter).coerceAtLeast(0f) * MAX_RADIUS_FRAC * minDim
            val x = cx + r * cos(angle)
            val y = cy + r * sin(angle)
            val radius = 0.6f + sizeUnit * 1.6f
            val tint = palette.colorAt(i)
            val alpha = (alphaUnit * (0.55f + 0.45f * pulse) * 255).toInt().coerceIn(0, 255)
            paint.color = Color.argb(
                alpha,
                (Color.red(tint) + 255) / 2,
                (Color.green(tint) + 255) / 2,
                (Color.blue(tint) + 255) / 2,
            )
            canvas.drawCircle(x, y, radius, paint)
        }
        paint.alpha = 255
    }
}

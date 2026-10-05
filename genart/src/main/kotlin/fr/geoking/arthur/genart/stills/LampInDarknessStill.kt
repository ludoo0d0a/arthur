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
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** Bakes one frozen frame of Lamp in Darkness for Auto/Ambient album art. */
internal object LampInDarknessStill {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)
    private val path = Path()
    private val oval = RectF()

    private enum class LampKind { Pendant, Floor, Desk, CeilingSpots, WallSconce }

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
        val kind = LampKind.entries[rnd.nextInt(LampKind.entries.size)]

        paint.shader = RadialGradient(
            w * 0.5f, h * 0.4f, (minDim * 1.15f).coerceAtLeast(1f),
            intArrayOf(0xFF14160E.toInt(), 0xFF070806.toInt(), 0xFF010201.toInt()),
            floatArrayOf(0f, 0.55f, 1f),
            Shader.TileMode.CLAMP,
        )
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        paint.shader = RadialGradient(
            w * 0.72f, h * 0.55f, (minDim * 0.45f).coerceAtLeast(1f),
            Color.argb(46, 28, 34, 20),
            Color.TRANSPARENT,
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(w * 0.72f, h * 0.55f, minDim * 0.45f, paint)
        paint.shader = null

        val beams = buildBeams(kind, rnd)
        val tint = palette.colorAt(0)
        val warmR = ((Color.red(tint) + 245) / 2)
        val warmG = ((Color.green(tint) + 214) / 2)
        val warmB = ((Color.blue(tint) + 106) / 2)

        beams.forEach { beam ->
            val sway = beam.swayAmp * sin(time * beam.swayFreq)
            val a = beam.angle + sway
            val ox = beam.ox * w
            val oy = beam.oy * h
            val reach = beam.reach * h
            val leftX = ox + sin(a - beam.halfWidth) * reach
            val leftY = oy + cos(a - beam.halfWidth) * reach
            val rightX = ox + sin(a + beam.halfWidth) * reach
            val rightY = oy + cos(a + beam.halfWidth) * reach
            val midX = (leftX + rightX) * 0.5f
            val midY = (leftY + rightY) * 0.5f

            path.reset()
            path.moveTo(ox, oy)
            path.lineTo(leftX, leftY)
            path.lineTo(rightX, rightY)
            path.close()
            paint.shader = LinearGradient(
                ox, oy, midX, midY,
                intArrayOf(
                    Color.argb((0.42f * dim * 255).toInt().coerceIn(0, 255), warmR, warmG, warmB),
                    Color.argb((0.18f * dim * 255).toInt().coerceIn(0, 255), warmR, warmG, warmB),
                    Color.TRANSPARENT,
                ),
                floatArrayOf(0f, 0.45f, 1f),
                Shader.TileMode.CLAMP,
            )
            canvas.drawPath(path, paint)
            paint.shader = null

            for (r in 0 until 8) {
                val u = (r + 0.5f) / 8f
                val rayA = a + (-beam.halfWidth + 2f * beam.halfWidth * u) * 0.92f
                val tipX = ox + sin(rayA) * reach
                val tipY = oy + cos(rayA) * reach
                paint.shader = LinearGradient(
                    ox, oy, tipX, tipY,
                    Color.argb((0.12f * dim * 255).toInt().coerceIn(0, 255), warmR, warmG, warmB),
                    Color.TRANSPARENT,
                    Shader.TileMode.CLAMP,
                )
                paint.strokeWidth = minDim * 0.004f
                paint.style = Paint.Style.STROKE
                canvas.drawLine(ox, oy, tipX, tipY, paint)
                paint.style = Paint.Style.FILL
                paint.shader = null
            }

            val poolR = (minDim * (0.14f + beam.halfWidth * 0.7f)).coerceAtLeast(1f)
            paint.shader = RadialGradient(
                midX, midY, poolR,
                intArrayOf(
                    Color.argb((0.55f * dim * 255).toInt().coerceIn(0, 255), 255, 255, 255),
                    Color.argb((0.35f * dim * 255).toInt().coerceIn(0, 255), warmR, warmG, warmB),
                    Color.TRANSPARENT,
                ),
                floatArrayOf(0f, 0.3f, 1f),
                Shader.TileMode.CLAMP,
            )
            canvas.drawCircle(midX, midY, poolR, paint)
            paint.shader = null

            val sourceR = (minDim * 0.045f).coerceAtLeast(1f)
            paint.shader = RadialGradient(
                ox, oy, sourceR,
                Color.argb((0.7f * dim * 255).toInt().coerceIn(0, 255), 255, 255, 255),
                Color.TRANSPARENT,
                Shader.TileMode.CLAMP,
            )
            canvas.drawCircle(ox, oy, sourceR, paint)
            paint.shader = null

            drawFixture(canvas, kind, beam, ox, oy, minDim, h)
        }

        val dustCount = 60 + rnd.nextInt(50)
        for (j in 0 until dustCount) {
            val beam = beams[rnd.nextInt(beams.size)]
            val sway = beam.swayAmp * sin(time * beam.swayFreq)
            val a = beam.angle + sway
            val along = 0.08f + rnd.nextFloat() * 0.88f
            val side = -0.9f + rnd.nextFloat() * 1.8f
            val ox = beam.ox * w
            val oy = beam.oy * h
            val reach = beam.reach * h
            val sideA = a + beam.halfWidth * along * side
            val px = ox + sin(sideA) * reach * along
            val py = oy + cos(sideA) * reach * along
            val twinkle = 0.3f + 0.7f * rnd.nextFloat()
            val moteR = (minDim * (0.0012f + rnd.nextFloat() * 0.0038f) * (0.7f + 0.6f * twinkle))
                .coerceAtLeast(0.5f)
            val alpha = (0.6f * twinkle * dim * 255).toInt().coerceIn(0, 255)
            paint.color = Color.argb(alpha, (warmR + 255) / 2, (warmG + 255) / 2, (warmB + 255) / 2)
            canvas.drawCircle(px, py, moteR, paint)
            if (rnd.nextFloat() > 0.92f) {
                val arm = moteR * 3.2f
                paint.strokeWidth = 1f
                paint.style = Paint.Style.STROKE
                canvas.drawLine(px - arm, py, px + arm, py, paint)
                canvas.drawLine(px, py - arm, px, py + arm, paint)
                paint.style = Paint.Style.FILL
            }
        }
        paint.alpha = 255
        paint.shader = null
    }

    private data class Beam(
        val ox: Float,
        val oy: Float,
        val angle: Float,
        val halfWidth: Float,
        val reach: Float,
        val swayAmp: Float,
        val swayFreq: Float,
    )

    private fun buildBeams(kind: LampKind, rnd: Random): List<Beam> = when (kind) {
        LampKind.Pendant -> listOf(
            Beam(
                ox = 0.42f + rnd.nextFloat() * 0.16f,
                oy = 0.06f + rnd.nextFloat() * 0.08f,
                angle = -0.12f + rnd.nextFloat() * 0.24f,
                halfWidth = 0.14f + rnd.nextFloat() * 0.08f,
                reach = 0.95f + rnd.nextFloat() * 0.25f,
                swayAmp = 0.01f + rnd.nextFloat() * 0.02f,
                swayFreq = 0.15f + rnd.nextFloat() * 0.25f,
            ),
        )
        LampKind.Floor -> listOf(
            Beam(
                ox = 0.28f + rnd.nextFloat() * 0.2f,
                oy = 0.55f + rnd.nextFloat() * 0.12f,
                angle = PI.toFloat() + (-0.35f + rnd.nextFloat() * 0.7f),
                halfWidth = 0.16f + rnd.nextFloat() * 0.1f,
                reach = 0.7f + rnd.nextFloat() * 0.3f,
                swayAmp = 0.01f + rnd.nextFloat() * 0.015f,
                swayFreq = 0.12f + rnd.nextFloat() * 0.23f,
            ),
        )
        LampKind.Desk -> listOf(
            Beam(
                ox = 0.18f + rnd.nextFloat() * 0.2f,
                oy = 0.28f + rnd.nextFloat() * 0.15f,
                angle = 0.55f + (-0.2f + rnd.nextFloat() * 0.55f),
                halfWidth = 0.12f + rnd.nextFloat() * 0.08f,
                reach = 0.75f + rnd.nextFloat() * 0.3f,
                swayAmp = 0.015f + rnd.nextFloat() * 0.025f,
                swayFreq = 0.2f + rnd.nextFloat() * 0.25f,
            ),
        )
        LampKind.CeilingSpots -> {
            val count = 2 + rnd.nextInt(3)
            List(count) { i ->
                Beam(
                    ox = 0.18f + i * (0.64f / (count - 1).coerceAtLeast(1)) + (-0.04f + rnd.nextFloat() * 0.08f),
                    oy = 0.04f + rnd.nextFloat() * 0.06f,
                    angle = -0.2f + rnd.nextFloat() * 0.4f,
                    halfWidth = 0.08f + rnd.nextFloat() * 0.06f,
                    reach = 0.85f + rnd.nextFloat() * 0.3f,
                    swayAmp = 0.008f + rnd.nextFloat() * 0.017f,
                    swayFreq = 0.15f + rnd.nextFloat() * 0.25f,
                )
            }
        }
        LampKind.WallSconce -> {
            val left = rnd.nextBoolean()
            listOf(
                Beam(
                    ox = if (left) 0.08f else 0.92f,
                    oy = 0.32f + rnd.nextFloat() * 0.2f,
                    angle = if (left) {
                        PI.toFloat() * 0.5f + (-0.25f + rnd.nextFloat() * 0.5f)
                    } else {
                        -PI.toFloat() * 0.5f + (-0.25f + rnd.nextFloat() * 0.5f)
                    },
                    halfWidth = 0.18f + rnd.nextFloat() * 0.1f,
                    reach = 0.7f + rnd.nextFloat() * 0.35f,
                    swayAmp = 0.01f + rnd.nextFloat() * 0.02f,
                    swayFreq = 0.15f + rnd.nextFloat() * 0.25f,
                ),
            )
        }
    }

    private fun drawFixture(
        canvas: Canvas,
        kind: LampKind,
        beam: Beam,
        ox: Float,
        oy: Float,
        minDim: Float,
        h: Float,
    ) {
        paint.shader = null
        paint.color = 0xFF121210.toInt()
        paint.style = Paint.Style.FILL
        when (kind) {
            LampKind.Pendant -> {
                paint.strokeWidth = minDim * 0.006f
                paint.style = Paint.Style.STROKE
                canvas.drawLine(ox, 0f, ox, oy, paint)
                paint.style = Paint.Style.FILL
                val shadeR = minDim * 0.045f
                canvas.drawCircle(ox, oy, shadeR, paint)
                oval.set(ox - shadeR * 1.35f, oy - shadeR * 0.2f, ox + shadeR * 1.35f, oy + shadeR * 1.4f)
                canvas.drawArc(oval, 10f, 160f, true, paint)
            }
            LampKind.Floor -> {
                val baseY = h * 0.92f
                paint.strokeWidth = minDim * 0.012f
                paint.style = Paint.Style.STROKE
                canvas.drawLine(ox, oy, ox, baseY, paint)
                paint.style = Paint.Style.FILL
                canvas.drawCircle(ox, baseY, minDim * 0.035f, paint)
                canvas.drawCircle(ox, oy, minDim * 0.05f, paint)
            }
            LampKind.Desk -> {
                val jointX = ox - minDim * 0.08f
                val jointY = oy + minDim * 0.06f
                val baseX = jointX - minDim * 0.04f
                val baseY = h * 0.78f
                paint.strokeWidth = minDim * 0.01f
                paint.style = Paint.Style.STROKE
                canvas.drawLine(ox, oy, jointX, jointY, paint)
                paint.strokeWidth = minDim * 0.012f
                canvas.drawLine(jointX, jointY, baseX, baseY, paint)
                paint.style = Paint.Style.FILL
                canvas.drawCircle(ox, oy, minDim * 0.04f, paint)
                canvas.drawCircle(baseX, baseY, minDim * 0.028f, paint)
            }
            LampKind.CeilingSpots -> {
                canvas.drawCircle(ox, oy, minDim * 0.028f, paint)
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = minDim * 0.006f
                paint.color = 0xFF2A2A28.toInt()
                canvas.drawCircle(ox, oy, minDim * 0.018f, paint)
                paint.style = Paint.Style.FILL
                paint.color = 0xFF121210.toInt()
            }
            LampKind.WallSconce -> {
                val inward = if (beam.ox < 0.5f) 1f else -1f
                val left = ox - minDim * 0.02f * inward - minDim * 0.02f
                canvas.drawRoundRect(
                    left,
                    oy - minDim * 0.04f,
                    left + minDim * 0.055f,
                    oy + minDim * 0.04f,
                    minDim * 0.012f,
                    minDim * 0.012f,
                    paint,
                )
            }
        }
    }
}

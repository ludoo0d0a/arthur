package fr.geoking.arthur.genart.stills

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import fr.geoking.arthur.genart.AnimationPalette
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** Bakes one frozen frame of Silk Bloom for Auto/Ambient album art. */
internal object SilkBloomStill {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)
    private val path = Path()
    private val rimPath = Path()

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
        val dim = 0.65f + 0.35f * pulse
        val time = ((phase / (2f * PI.toFloat())) + rotationDeg / 360f)
        val loop = ((time % 1f) + 1f) % 1f
        val spin = loop * 2f * PI.toFloat() * 0.28f
        val originX = w * 0.5f
        val originY = h * 0.48f

        paint.shader = null
        paint.color = Color.BLACK
        canvas.drawRect(0f, 0f, w, h, paint)

        val petalCount = 6
        val petals = (0 until petalCount).map { i ->
            val tone = i % 3
            StillSilkPetal(
                angle0 = (rnd.nextFloat() * 2f - 1f) * PI.toFloat(),
                lengthFrac = 0.38f + rnd.nextFloat() * 0.2f,
                widthFrac = 0.28f + rnd.nextFloat() * 0.2f,
                twist = (rnd.nextFloat() - 0.5f) * 1.7f,
                bend = 0.35f + rnd.nextFloat() * 0.6f,
                openPhase = rnd.nextFloat() * 2f * PI.toFloat(),
                openSpeed = 0.45f + rnd.nextFloat() * 0.5f,
                spinMul = 0.55f + rnd.nextFloat() * 0.6f,
                tone = tone,
                colorMix = rnd.nextFloat(),
                alpha = if (tone == 2) 0.35f + rnd.nextFloat() * 0.23f else 0.62f + rnd.nextFloat() * 0.26f,
                misty = tone == 2 || rnd.nextFloat() > 0.72f,
                layer = rnd.nextFloat(),
                grainSeed = rnd.nextInt(),
            )
        }.sortedBy { it.layer }

        petals.forEach { petal ->
            val open = 0.35f + 0.65f * (0.5f + 0.5f * sin(loop * 2f * PI.toFloat() * petal.openSpeed + petal.openPhase))
            val angle = petal.angle0 + spin * petal.spinMul + petal.twist * (1.15f - open) * 0.55f
            val len = minDim * petal.lengthFrac * (0.7f + 0.3f * open)
            val wid = minDim * petal.widthFrac * (0.55f + 0.45f * open)
            val (cA, cB, cC) = petalColors(petal, palette)
            val a = (petal.alpha * dim * 255).toInt().coerceIn(0, 255)

            canvas.save()
            canvas.translate(originX, originY)
            canvas.rotate(Math.toDegrees(angle.toDouble()).toFloat())
            buildRibbon(path, len, wid, petal.twist, petal.bend)
            paint.shader = LinearGradient(
                -wid * 0.35f, len * 0.15f, wid * 0.4f, -len * 0.85f,
                intArrayOf(
                    Color.argb(a, Color.red(cA), Color.green(cA), Color.blue(cA)),
                    Color.argb((a * 0.95f).toInt(), Color.red(cB), Color.green(cB), Color.blue(cB)),
                    Color.argb((a * 0.72f).toInt(), Color.red(cC), Color.green(cC), Color.blue(cC)),
                ),
                null,
                Shader.TileMode.CLAMP,
            )
            paint.style = Paint.Style.FILL
            canvas.drawPath(path, paint)

            buildRibbon(rimPath, len * 0.92f, wid * 0.78f, petal.twist * 0.85f, petal.bend)
            paint.shader = LinearGradient(
                -wid * 0.2f, 0f, wid * 0.35f, -len * 0.9f,
                intArrayOf(
                    Color.argb((0.14f * dim * 255).toInt(), 255, 255, 255),
                    Color.argb((0.08f * dim * 255).toInt(), Color.red(cC), Color.green(cC), Color.blue(cC)),
                    Color.TRANSPARENT,
                ),
                null,
                Shader.TileMode.CLAMP,
            )
            canvas.drawPath(rimPath, paint)
            paint.shader = null
            drawGrain(canvas, len, wid, petal.twist, petal.bend, petal.grainSeed, cA, cC, petal.misty, dim)
            canvas.restore()
        }

        paint.style = Paint.Style.FILL
        petals.forEachIndexed { idx, petal ->
            if (!petal.misty) return@forEachIndexed
            val open = 0.35f + 0.65f * (0.5f + 0.5f * sin(loop * 2f * PI.toFloat() * petal.openSpeed + petal.openPhase))
            val angle = petal.angle0 + spin * petal.spinMul + petal.twist * (1.15f - open) * 0.55f
            val len = minDim * petal.lengthFrac * (0.7f + 0.3f * open)
            val wid = minDim * petal.widthFrac * (0.55f + 0.45f * open)
            val cosA = cos(angle)
            val sinA = sin(angle)
            val mistRnd = Random(generation + idx * 97L)
            for (j in 0 until 90) {
                val along = 0.15f + mistRnd.nextFloat() * 0.77f
                val side = (mistRnd.nextFloat() * 2f - 1f) * 0.95f
                val life = mistRnd.nextFloat()
                val p = ribbonPoint(along, side, len, wid, petal.twist, petal.bend)
                val driftOut = life * minDim * 0.06f
                val lx = p.first + driftOut * side * 0.35f
                val ly = p.second - driftOut * 0.55f
                val px = originX + lx * cosA - ly * sinA
                val py = originY + lx * sinA + ly * cosA
                val fade = (1f - life * 0.55f) * open
                val alpha = ((0.25f + mistRnd.nextFloat() * 0.45f) * fade * dim * 255).toInt().coerceIn(0, 255)
                paint.color = Color.argb(alpha, 232, 238, 248)
                canvas.drawCircle(px, py, (0.7f + mistRnd.nextFloat() * 2.1f) * minDim * 0.0024f, paint)
            }
        }
    }

    private fun drawGrain(
        canvas: Canvas,
        length: Float,
        width: Float,
        twist: Float,
        bend: Float,
        seed: Int,
        cA: Int,
        cC: Int,
        misty: Boolean,
        dim: Float,
    ) {
        val rnd = Random(seed.toLong())
        val light = mixArgb(cC, Color.WHITE, 0.65f)
        val shade = mixArgb(cA, 0xFF2A1C18.toInt(), 0.35f)
        val lightA = ((if (misty) 0.28f else 0.16f) * dim * 255).toInt()
        val shadeA = (0.10f * dim * 255).toInt()
        for (i in 0 until 140) {
            val along = 0.05f + rnd.nextFloat() * 0.9f
            val side = rnd.nextFloat() * 2f - 1f
            val (x, y) = ribbonPoint(along, side, length, width, twist, bend)
            val r = (0.5f + rnd.nextFloat() * 1.6f) * length * 0.0038f
            val c = if (i % 4 == 0) shade else light
            val a = if (i % 4 == 0) shadeA else lightA
            paint.color = Color.argb(a.coerceIn(0, 255), Color.red(c), Color.green(c), Color.blue(c))
            canvas.drawCircle(x, y, r, paint)
        }
    }

    private fun buildRibbon(out: Path, length: Float, width: Float, twist: Float, bend: Float) {
        val half = width * 0.5f
        val lean = twist * half * 0.7f
        val arch = bend * length * 0.22f
        out.reset()
        out.moveTo(-half * 0.15f, length * 0.12f)
        out.cubicTo(
            -half * 0.95f - lean * 0.2f, length * 0.05f - arch * 0.2f,
            -half * 1.15f - lean, -length * 0.35f - arch * 0.3f,
            -half * 0.25f + lean * 0.4f, -length * 0.92f,
        )
        out.cubicTo(
            half * 0.15f + lean * 0.6f, -length * 1.02f,
            half * 1.1f + lean * 0.35f, -length * 0.45f + arch * 0.25f,
            half * 0.85f + lean * 0.15f, -length * 0.05f,
        )
        out.cubicTo(
            half * 0.45f, length * 0.18f,
            half * 0.05f, length * 0.22f,
            -half * 0.15f, length * 0.12f,
        )
        out.close()
    }

    private fun ribbonPoint(
        along: Float,
        side: Float,
        length: Float,
        width: Float,
        twist: Float,
        bend: Float,
    ): Pair<Float, Float> {
        val half = width * 0.5f
        val lean = twist * half * 0.55f * along
        val arch = bend * length * 0.18f * sin(along * PI.toFloat())
        val x = side * half * (0.35f + 0.65f * along) * (1f - along * 0.15f) + lean
        val y = length * 0.1f - along * length * 1.05f - arch
        return x to y
    }

    private fun petalColors(petal: StillSilkPetal, palette: AnimationPalette): Triple<Int, Int, Int> {
        val accents = when (petal.tone) {
            0 -> Triple(0xFFE8876A.toInt(), 0xFFF0A888.toInt(), 0xFFF5C4B0.toInt())
            1 -> Triple(0xFF8FB892.toInt(), 0xFFA8C9A4.toInt(), 0xFFC5DCC4.toInt())
            else -> Triple(0xFFC8C0D0.toInt(), 0xFFE0D8E4.toInt(), 0xFFF0ECF4.toInt())
        }
        val p = palette.colorAt(petal.tone)
        fun mix(c: Int) = mixArgb(c, p, 0.22f)
        return Triple(
            mix(mixArgb(accents.first, accents.second, petal.colorMix)),
            mix(accents.second),
            mix(accents.third),
        )
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

    private data class StillSilkPetal(
        val angle0: Float,
        val lengthFrac: Float,
        val widthFrac: Float,
        val twist: Float,
        val bend: Float,
        val openPhase: Float,
        val openSpeed: Float,
        val spinMul: Float,
        val tone: Int,
        val colorMix: Float,
        val alpha: Float,
        val misty: Boolean,
        val layer: Float,
        val grainSeed: Int,
    )
}

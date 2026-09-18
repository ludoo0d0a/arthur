package fr.geoking.arthur.genart.stills

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import fr.geoking.arthur.genart.AnimationPalette
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** Bakes a high-density 4K space nebula with deep blues, vibrant pinks, and rich starfields. */
internal object NebulaStill {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)

    // Cosmic Blue and Pink/Magenta palette references
    private val cosmicBluePinkTones = intArrayOf(
        0xFF0F172A.toInt(), // Dark slate blue
        0xFF1E3A8A.toInt(), // Deep royal blue
        0xFF2563EB.toInt(), // Vivid blue
        0xFF0284C7.toInt(), // Bright cyan-blue
        0xFF3B82F6.toInt(), // Electric blue
        0xFF701A75.toInt(), // Deep magenta
        0xFFBE185D.toInt(), // Rich pink
        0xFFDB2777.toInt(), // Hot pink
        0xFFEC4899.toInt(), // Vibrant pink
        0xFFF472B6.toInt(), // Soft glowing pink
        0xFFE0F2FE.toInt(), // Ice blue highlight
        0xFFFCE7F3.toInt(), // Ice pink highlight
    )

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
        val maxDim = maxOf(w, h)

        // 1. Deep Space Base Background
        paint.color = 0xFF03020A.toInt()
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w, h, paint)

        // Subtle background cosmic gradient
        val bgGrad = RadialGradient(
            w * 0.5f, h * 0.5f, maxDim * 0.85f,
            intArrayOf(0xFF0D0A22.toInt(), 0xFF04020A.toInt()),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP,
        )
        paint.shader = bgGrad
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        val time = ((phase / (2f * PI.toFloat())) + rotationDeg / 360f)
        val loop = ((time % 1f) + 1f) % 1f
        val dim = 0.70f + 0.30f * pulse

        // 2. Multi-layered Gas Clouds (Dense coverage filling 80%+ screen space)
        // Layer A: Ambient Large Background Dust (8 clouds)
        for (i in 0 until 8) {
            val x0 = rnd.nextFloat()
            val y0 = rnd.nextFloat()
            val speedX = 0.02f + rnd.nextFloat() * 0.04f
            val speedY = 0.01f + rnd.nextFloat() * 0.03f
            val x = (((x0 + loop * speedX) % 1f) + 1f) % 1f * w
            val y = (((y0 + loop * speedY) % 1f) + 1f) % 1f * h
            val radius = (0.45f + rnd.nextFloat() * 0.35f) * minDim
            val colorHex = resolveColor(i, palette, isHighlight = false)
            val alpha = (0.12f * dim * 255).toInt().coerceIn(0, 255)

            paint.shader = RadialGradient(
                x, y, radius.coerceAtLeast(1f),
                intArrayOf(
                    Color.argb(alpha, Color.red(colorHex), Color.green(colorHex), Color.blue(colorHex)),
                    Color.argb((alpha * 0.4f).toInt(), Color.red(colorHex), Color.green(colorHex), Color.blue(colorHex)),
                    Color.TRANSPARENT,
                ),
                floatArrayOf(0f, 0.5f, 1f),
                Shader.TileMode.CLAMP,
            )
            canvas.drawCircle(x, y, radius, paint)
            paint.shader = null
        }

        // Layer B: Main Body Clouds - Deep Blues & Magenta/Pinks (14 clouds)
        for (i in 0 until 14) {
            val x0 = rnd.nextFloat()
            val y0 = rnd.nextFloat()
            val speedX = 0.02f + rnd.nextFloat() * 0.06f
            val speedY = 0.01f + rnd.nextFloat() * 0.05f
            val radiusFrac = 0.25f + rnd.nextFloat() * 0.30f
            val pulseFreq = 0.15f + rnd.nextFloat() * 0.35f
            val pulsePhase = rnd.nextFloat() * 2f * PI.toFloat()
            val alphaBase = 0.18f + rnd.nextFloat() * 0.22f
            val x = (((x0 + loop * speedX) % 1f) + 1f) % 1f * w
            val y = (((y0 + loop * speedY) % 1f) + 1f) % 1f * h
            val pulseMul = 0.85f + 0.15f * sin(loop * 2f * PI.toFloat() * pulseFreq + pulsePhase) * pulse
            val radius = radiusFrac * minDim * pulseMul
            val colorHex = resolveColor(i + 2, palette, isHighlight = false)
            val alpha = (alphaBase * dim * 255).toInt().coerceIn(0, 255)

            paint.shader = RadialGradient(
                x, y, radius.coerceAtLeast(1f),
                intArrayOf(
                    Color.argb(alpha, Color.red(colorHex), Color.green(colorHex), Color.blue(colorHex)),
                    Color.argb((alpha * 0.45f).toInt(), Color.red(colorHex), Color.green(colorHex), Color.blue(colorHex)),
                    Color.TRANSPARENT,
                ),
                floatArrayOf(0f, 0.4f, 1f),
                Shader.TileMode.CLAMP,
            )
            canvas.drawCircle(x, y, radius, paint)
            paint.shader = null
        }

        // Layer C: Intense Core Highlights (6 clouds)
        for (i in 0 until 6) {
            val x0 = 0.2f + rnd.nextFloat() * 0.6f
            val y0 = 0.2f + rnd.nextFloat() * 0.6f
            val speedX = 0.01f + rnd.nextFloat() * 0.03f
            val speedY = 0.01f + rnd.nextFloat() * 0.03f
            val x = (((x0 + loop * speedX) % 1f) + 1f) % 1f * w
            val y = (((y0 + loop * speedY) % 1f) + 1f) % 1f * h
            val radius = (0.12f + rnd.nextFloat() * 0.18f) * minDim
            val colorHex = resolveColor(i, palette, isHighlight = true)
            val alpha = (0.35f * dim * 255).toInt().coerceIn(0, 255)

            paint.shader = RadialGradient(
                x, y, radius.coerceAtLeast(1f),
                intArrayOf(
                    Color.argb(alpha, Color.red(colorHex), Color.green(colorHex), Color.blue(colorHex)),
                    Color.argb((alpha * 0.3f).toInt(), Color.red(colorHex), Color.green(colorHex), Color.blue(colorHex)),
                    Color.TRANSPARENT,
                ),
                floatArrayOf(0f, 0.35f, 1f),
                Shader.TileMode.CLAMP,
            )
            canvas.drawCircle(x, y, radius, paint)
            paint.shader = null
        }

        // 3. Multi-Tier Starfield (fractions of minDim — crisp at near-4K, no pixel blocks)
        for (j in 0 until 220) {
            val sx = rnd.nextFloat() * w
            val sy = rnd.nextFloat() * h
            val a = 0.2f + rnd.nextFloat() * 0.55f
            val starSize = (0.0007f + rnd.nextFloat() * 0.0018f) * minDim
            val tint = if (j % 4 == 0) 0xFFDB2777.toInt() else if (j % 3 == 0) 0xFF60A5FA.toInt() else 0xFFFFFFFF.toInt()
            paint.color = Color.argb((a * 255).toInt(), Color.red(tint), Color.green(tint), Color.blue(tint))
            canvas.drawCircle(sx, sy, starSize.coerceAtLeast(0.8f), paint)
        }

        for (j in 0 until 48) {
            val sx = rnd.nextFloat() * w
            val sy = rnd.nextFloat() * h
            val a = 0.45f + rnd.nextFloat() * 0.5f
            val coreSize = (0.0016f + rnd.nextFloat() * 0.0024f) * minDim
            val glowSize = coreSize * (2.8f + rnd.nextFloat() * 2.2f)
            paint.shader = RadialGradient(
                sx, sy, glowSize,
                intArrayOf(Color.argb((a * 130).toInt(), 255, 255, 255), Color.TRANSPARENT),
                floatArrayOf(0f, 1f),
                Shader.TileMode.CLAMP,
            )
            canvas.drawCircle(sx, sy, glowSize, paint)
            paint.shader = null
            paint.color = Color.argb((a * 255).toInt(), 255, 255, 255)
            canvas.drawCircle(sx, sy, coreSize.coerceAtLeast(1f), paint)
        }

        for (j in 0 until 12) {
            val sx = rnd.nextFloat() * w
            val sy = rnd.nextFloat() * h
            val flareLen = (0.012f + rnd.nextFloat() * 0.02f) * minDim
            paint.color = Color.argb(200, 255, 255, 255)
            paint.strokeWidth = (0.0012f * minDim).coerceAtLeast(1.1f)
            canvas.drawLine(sx - flareLen, sy, sx + flareLen, sy, paint)
            canvas.drawLine(sx, sy - flareLen, sx, sy + flareLen, paint)
            paint.style = Paint.Style.FILL
            canvas.drawCircle(sx, sy, (0.0028f * minDim).coerceAtLeast(1.6f), paint)
        }

        paint.alpha = 255
        paint.strokeWidth = 1f
    }

    private fun resolveColor(index: Int, palette: AnimationPalette, isHighlight: Boolean): Int {
        if (isHighlight) {
            return if (index % 2 == 0) 0xFFE0F2FE.toInt() else 0xFFFCE7F3.toInt()
        }
        val cosmicColor = cosmicBluePinkTones[index % cosmicBluePinkTones.size]
        val paletteColor = palette.colorAt(index)
        // Blend palette color with signature blue/pink cosmic tones
        return if (index % 3 == 0) paletteColor else cosmicColor
    }
}

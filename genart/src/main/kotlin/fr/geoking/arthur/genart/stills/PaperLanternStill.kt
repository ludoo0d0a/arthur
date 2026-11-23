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
import fr.geoking.arthur.genart.loopedFbm
import fr.geoking.arthur.genart.positiveRadius
import fr.geoking.arthur.genart.seededRange
import fr.geoking.arthur.genart.seededUnit
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import kotlin.math.sqrt

/** Bakes one frozen frame of Paper Lantern Glow for Auto/Ambient album art. */
internal object PaperLanternStill {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)
    private val rect = RectF()
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
        val w = size.toFloat()
        val h = size.toFloat()
        val minDim = minOf(w, h)
        val t01 = ((phase % (2f * PI.toFloat())) / (2f * PI.toFloat())).let { v -> if (v < 0f) v + 1f else v }
        val dim = 0.85f + 0.15f * pulse

        // Color definitions
        val baseDark = 0xFF1A1210.toInt()
        val midDark = 0xFF281812.toInt()
        val deepDark = 0xFF3A2218.toInt()
        val accent = mixArgb(0xFFFFB060.toInt(), palette.colorAt(0), 0.35f)

        // Ambient background wash
        val wash = loopedFbm(t01, radius = 1.35f, octaves = 2, seedOffset = 11)
        val topWashDark = brightnessArgb(baseDark, 0.90f + 0.10f * wash)
        paint.shader = LinearGradient(
            0f, 0f, 0f, h,
            intArrayOf(topWashDark, midDark, deepDark),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP,
        )
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        // Upper architecture silhouette (eave / beam)
        val eaveH = h * 0.05f
        val woodDark = 0xFF120B08.toInt()
        paint.color = woodDark
        canvas.drawRect(0f, 0f, w, eaveH, paint)
        paint.color = 0xFF24150F.toInt()
        canvas.drawRect(0f, eaveH - minDim * 0.006f, w, eaveH, paint)

        // Sway & bob
        val sway = (loopedFbm(t01, radius = 1.25f, octaves = 2, seedOffset = 19) * 2f - 1f) * 0.032f
        val bob = (loopedFbm(t01, radius = 1.1f, octaves = 2, seedOffset = 47) * 2f - 1f) * 0.008f
        val cx = w * 0.5f + minDim * sway
        val cy = h * 0.44f + minDim * bob

        val rx = minDim * 0.21f
        val ry = minDim * 0.26f

        // Candle flicker
        val flicker = loopedFbm(t01, radius = 2.1f, octaves = 3, seedOffset = 91)
        val flickerScale = (0.94f + 0.12f * flicker) * (0.95f + 0.10f * pulse)
        val flickerAlpha = (0.88f + 0.22f * flicker) * dim

        // 1. Ambient warm radial lantern light pool
        val lightPoolRadius = (minDim * 0.82f).positiveRadius()
        paint.shader = RadialGradient(
            cx, cy, lightPoolRadius,
            intArrayOf(
                withAlpha(accent, (0.42f * flickerAlpha * 255).toInt()),
                withAlpha(0xFF9E4218.toInt(), (0.22f * dim * 255).toInt()),
                withAlpha(0xFF481A10.toInt(), (0.08f * dim * 255).toInt()),
                Color.TRANSPARENT,
            ),
            floatArrayOf(0f, 0.32f, 0.68f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(cx, cy, lightPoolRadius, paint)
        paint.shader = null

        // Floor reflection pool
        val floorGlowRadius = (minDim * 0.55f).positiveRadius()
        paint.shader = RadialGradient(
            cx, h * 0.90f, floorGlowRadius,
            intArrayOf(
                withAlpha(accent, (0.20f * flickerAlpha * 255).toInt()),
                withAlpha(0xFF5A2214.toInt(), (0.08f * dim * 255).toInt()),
                Color.TRANSPARENT,
            ),
            floatArrayOf(0f, 0.55f, 1f),
            Shader.TileMode.CLAMP,
        )
        rect.set(cx - minDim * 0.40f, h * 0.80f, cx + minDim * 0.40f, h * 1.00f)
        canvas.drawOval(rect, paint)
        paint.shader = null

        // 2. Suspension cord
        val cordTopY = eaveH
        val topCapH = minDim * 0.032f
        val topCapW = rx * 0.70f
        val cordBottomY = cy - ry - topCapH
        paint.color = woodDark
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = minDim * 0.005f
        paint.strokeCap = Paint.Cap.ROUND
        canvas.drawLine(w * 0.5f, cordTopY, cx, cordBottomY, paint)
        paint.style = Paint.Style.FILL

        // 3. Floating atmospheric motes
        for (i in 0 until 7) {
            val moteRadius = seededRange(i * 41 + 11, 0.025f, 0.065f)
            val driftX = seededRange(i * 53 + 13, 0.025f, 0.06f)
            val driftY = seededRange(i * 67 + 19, 0.035f, 0.08f)
            val phaseOffset = seededRange(i * 71 + 23, 0f, 2f * PI.toFloat())
            val x0 = seededUnit(i * 17 + 3)
            val y0 = seededUnit(i * 29 + 7)

            val n = loopedFbm(t01, radius = 1.3f + moteRadius, seedOffset = i * 97 + 5)
            val motX = (x0 + driftX * (n * 2f - 1f) + 1f) % 1f * w
            val motY = ((y0 - t01 * driftY + driftY * sin(t01 * 2f * PI.toFloat() + phaseOffset) * 0.25f) % 1f + 1f) % 1f * h
            val r = (minDim * moteRadius * (0.8f + 0.35f * n)).positiveRadius()

            val dx = motX - cx
            val dy = motY - cy
            val dist = sqrt(dx * dx + dy * dy)
            val glowProx = (1f - (dist / (minDim * 0.7f))).coerceIn(0.15f, 1f)

            val moteColor = mixArgb(accent, palette.colorAt(i), 0.4f)
            paint.shader = RadialGradient(
                motX, motY, r,
                intArrayOf(
                    withAlpha(moteColor, (0.45f * dim * glowProx * 255).toInt()),
                    Color.TRANSPARENT,
                ),
                floatArrayOf(0f, 1f),
                Shader.TileMode.CLAMP,
            )
            canvas.drawCircle(motX, motY, r, paint)
            paint.shader = null

            paint.color = withAlpha(0xFFFFF2D8.toInt(), ((0.4f + 0.5f * glowProx) * dim * 255).toInt())
            canvas.drawCircle(motX, motY, (r * 0.25f).positiveRadius(0.5f), paint)
        }

        // 4. Paper Lantern Translucent Body
        rect.set(cx - rx, cy - ry, cx + rx, cy + ry)
        val paperGlowRadius = (maxOf(rx, ry) * 1.05f).positiveRadius()
        val candleCoreY = cy + ry * 0.05f

        paint.shader = RadialGradient(
            cx, candleCoreY, paperGlowRadius,
            intArrayOf(
                mixArgb(0xFFFFF6D8.toInt(), accent, 0.22f),
                mixArgb(0xFFFFB458.toInt(), accent, 0.45f),
                0xFFD85E20.toInt(),
                0xFF88280E.toInt(),
                0xFF451208.toInt(),
            ),
            floatArrayOf(0f, 0.28f, 0.62f, 0.86f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawOval(rect, paint)
        paint.shader = null

        // 5. Candle flame core inside
        val flameRadius = (minDim * 0.12f * flickerScale).positiveRadius()
        paint.shader = RadialGradient(
            cx, candleCoreY, flameRadius,
            intArrayOf(
                withAlpha(Color.WHITE, (0.95f * dim * 255).toInt()),
                withAlpha(0xFFFFE88A.toInt(), (0.85f * dim * 255).toInt()),
                withAlpha(0xFFFF9426.toInt(), (0.45f * dim * 255).toInt()),
                Color.TRANSPARENT,
            ),
            floatArrayOf(0f, 0.32f, 0.68f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(cx, candleCoreY, flameRadius, paint)
        paint.shader = null

        // 6. Bamboo Ribs & Struts
        val strutColor = withAlpha(0xFF381408.toInt(), (0.35f * dim * 255).toInt())
        val strutWidth = minDim * 0.0035f
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = strutWidth
        paint.color = strutColor

        // Left vertical strut
        path.reset()
        path.moveTo(cx - topCapW * 0.35f, cy - ry)
        path.quadTo(cx - rx * 0.62f, cy, cx - topCapW * 0.32f, cy + ry)
        canvas.drawPath(path, paint)

        // Center vertical strut
        canvas.drawLine(cx, cy - ry, cx, cy + ry, paint)

        // Right vertical strut
        path.reset()
        path.moveTo(cx + topCapW * 0.35f, cy - ry)
        path.quadTo(cx + rx * 0.62f, cy, cx + topCapW * 0.32f, cy + ry)
        canvas.drawPath(path, paint)

        // Horizontal bamboo hoops
        val ribCount = 9
        for (i in 0 until ribCount) {
            val v = -0.74f + i * (1.48f / (ribCount - 1))
            val ribY = cy + ry * v
            val halfW = rx * sqrt((1f - v * v).coerceAtLeast(0f))
            val sag = minDim * 0.014f * (1f - 0.25f * v)

            path.reset()
            path.moveTo(cx - halfW, ribY)
            path.quadTo(cx, ribY + sag, cx + halfW, ribY)

            val ribAlpha = (0.32f + 0.14f * (1f - abs(v))) * dim
            paint.color = withAlpha(0xFF280E06.toInt(), (ribAlpha * 255).toInt())
            paint.strokeWidth = minDim * 0.004f
            canvas.drawPath(path, paint)

            // Warm highlight
            path.reset()
            path.moveTo(cx - halfW * 0.95f, ribY + minDim * 0.0025f)
            path.quadTo(cx, ribY + sag + minDim * 0.0025f, cx + halfW * 0.95f, ribY + minDim * 0.0025f)
            paint.color = withAlpha(accent, (0.16f * dim * (1f - abs(v)) * 255).toInt())
            paint.strokeWidth = minDim * 0.002f
            canvas.drawPath(path, paint)
        }

        // Silhouette rim
        paint.color = withAlpha(0xFF381206.toInt(), (0.55f * dim * 255).toInt())
        paint.strokeWidth = minDim * 0.004f
        canvas.drawOval(rect, paint)

        // 7. Wooden Top Collar / Cap
        paint.style = Paint.Style.FILL
        val topCapY = cy - ry - topCapH * 0.5f
        val capWood = 0xFF22130C.toInt()
        val capHighlight = 0xFF4C2A1A.toInt()

        paint.color = capWood
        rect.set(cx - topCapW * 0.5f, topCapY, cx + topCapW * 0.5f, topCapY + topCapH)
        canvas.drawRoundRect(rect, minDim * 0.008f, minDim * 0.008f, paint)

        paint.color = capHighlight
        canvas.drawRect(cx - topCapW * 0.45f, topCapY, cx + topCapW * 0.45f, topCapY + topCapH * 0.25f, paint)

        // Top loop
        paint.style = Paint.Style.STROKE
        paint.color = capWood
        paint.strokeWidth = minDim * 0.004f
        canvas.drawCircle(cx, topCapY, minDim * 0.012f, paint)
        paint.style = Paint.Style.FILL

        // 8. Wooden Bottom Collar / Rim
        val botCapH = minDim * 0.026f
        val botCapW = rx * 0.62f
        val botCapY = cy + ry - botCapH * 0.5f
        paint.color = capWood
        rect.set(cx - botCapW * 0.5f, botCapY, cx + botCapW * 0.5f, botCapY + botCapH)
        canvas.drawRoundRect(rect, minDim * 0.008f, minDim * 0.008f, paint)

        // Finial bead
        val finialY = botCapY + botCapH + minDim * 0.006f
        canvas.drawCircle(cx, finialY, minDim * 0.009f, paint)

        // 9. Hanging Silk Tassel
        val tasselLagSway = (loopedFbm(t01, radius = 1.2f, octaves = 2, seedOffset = 23) * 2f - 1f) * 0.050f
        val tasselLen = minDim * 0.13f
        val tasselTopY = finialY + minDim * 0.006f
        val tasselTipX = cx + minDim * tasselLagSway
        val tasselTipY = tasselTopY + tasselLen

        paint.color = 0xFF6B1A0E.toInt()
        canvas.drawCircle(cx, tasselTopY, minDim * 0.007f, paint)

        val tasselColors = intArrayOf(
            0xFF8C2212.toInt(),
            0xFFB83A1C.toInt(),
            0xFF6E180C.toInt(),
        )
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = minDim * 0.003f
        paint.strokeCap = Paint.Cap.ROUND
        for (k in -2..2) {
            val strandX = tasselTipX + k * minDim * 0.005f
            paint.color = tasselColors[abs(k) % tasselColors.size]
            canvas.drawLine(cx, tasselTopY, strandX, tasselTipY, paint)
        }
        paint.style = Paint.Style.FILL

        // 10. Soft atmospheric night vignette
        val vignetteRadius = (maxOf(w, h) * 0.85f).positiveRadius()
        paint.shader = RadialGradient(
            cx, cy, vignetteRadius,
            intArrayOf(
                Color.TRANSPARENT,
                Color.TRANSPARENT,
                withAlpha(0xFF0A0605.toInt(), (0.65f * dim * 255).toInt()),
            ),
            floatArrayOf(0f, 0.62f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null
    }

    private fun mixArgb(a: Int, b: Int, t: Float): Int {
        val u = t.coerceIn(0f, 1f)
        val r = (Color.red(a) + (Color.red(b) - Color.red(a)) * u).toInt().coerceIn(0, 255)
        val g = (Color.green(a) + (Color.green(b) - Color.green(a)) * u).toInt().coerceIn(0, 255)
        val bl = (Color.blue(a) + (Color.blue(b) - Color.blue(a)) * u).toInt().coerceIn(0, 255)
        return Color.rgb(r, g, bl)
    }

    private fun withAlpha(rgb: Int, alpha: Int): Int =
        Color.argb(alpha.coerceIn(0, 255), Color.red(rgb), Color.green(rgb), Color.blue(rgb))

    private fun brightnessArgb(argb: Int, factor: Float): Int {
        val r = (Color.red(argb) * factor).toInt().coerceIn(0, 255)
        val g = (Color.green(argb) * factor).toInt().coerceIn(0, 255)
        val b = (Color.blue(argb) * factor).toInt().coerceIn(0, 255)
        return Color.argb(Color.alpha(argb), r, g, b)
    }
}

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

/** Bakes one frozen frame of Billowing Cloth for Auto/Ambient album art. */
internal object BillowingClothStill {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)
    private val path = Path()
    private val shadow = Path()

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

        paint.shader = LinearGradient(
            0f, 0f, 0f, h,
            intArrayOf(0xFFD8E6F0.toInt(), 0xFFB4C8D8.toInt(), 0xFF8FA8BC.toInt()),
            floatArrayOf(0f, 0.55f, 1f),
            Shader.TileMode.CLAMP,
        )
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        val time = phase + rotationDeg * PI.toFloat() / 180f * 0.2f
        val gust = 0.4f + 0.6f * pulse
        val dim = 0.65f + 0.35f * pulse
        val lineY = h * 0.14f
        val clothCount = 4 + rnd.nextInt(3)
        val segments = 26

        paint.color = Color.argb((0.55f * dim * 255).toInt().coerceIn(0, 255), 58, 69, 80)
        paint.strokeWidth = (minDim * 0.004f).coerceAtLeast(1.2f)
        paint.strokeCap = Paint.Cap.ROUND
        paint.style = Paint.Style.STROKE
        canvas.drawLine(w * 0.02f, lineY, w * 0.98f, lineY, paint)
        paint.style = Paint.Style.FILL

        data class Sheet(
            val pinX: Float,
            val widthFrac: Float,
            val lengthFrac: Float,
            val sheetPhase: Float,
            val speedMul: Float,
            val waveCycles: Float,
            val swayAmp: Float,
            val colorIndex: Int,
            val colorMix: Float,
            val alpha: Float,
            val layer: Float,
        )

        val sheets = List(clothCount) { i ->
            Sheet(
                pinX = 0.08f + rnd.nextFloat() * 0.84f,
                widthFrac = 0.12f + rnd.nextFloat() * 0.1f,
                lengthFrac = 0.42f + rnd.nextFloat() * 0.3f,
                sheetPhase = rnd.nextFloat() * 2f * PI.toFloat(),
                speedMul = 0.55f + rnd.nextFloat() * 0.7f,
                waveCycles = 1.2f + rnd.nextFloat() * 1.4f,
                swayAmp = 0.04f + rnd.nextFloat() * 0.07f,
                colorIndex = i,
                colorMix = rnd.nextFloat(),
                alpha = 0.42f + rnd.nextFloat() * 0.3f,
                layer = rnd.nextFloat(),
            )
        }.sortedBy { it.layer }

        sheets.forEach { sheet ->
            val halfW = sheet.widthFrac * w * 0.5f
            val length = sheet.lengthFrac * h
            val sway = sheet.swayAmp * w * gust
            path.reset()
            for (k in 0..segments) {
                val u = k / segments.toFloat()
                val y = lineY + u * length
                val flare = u * u
                val wave = sin(u * sheet.waveCycles * 2f * PI.toFloat() + time * sheet.speedMul + sheet.sheetPhase)
                val x = sheet.pinX * w - halfW * (1f - 0.15f * u) + sway * flare * wave
                if (k == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            for (k in segments downTo 0) {
                val u = k / segments.toFloat()
                val y = lineY + u * length
                val flare = u * u
                val wave = sin(u * sheet.waveCycles * 2f * PI.toFloat() + time * sheet.speedMul + sheet.sheetPhase + 0.35f)
                val x = sheet.pinX * w + halfW * (1f - 0.15f * u) + sway * flare * wave
                path.lineTo(x, y)
            }
            path.close()

            shadow.reset()
            shadow.addPath(path)
            shadow.offset(minDim * 0.012f, minDim * 0.018f)
            paint.color = Color.argb((0.12f * dim * 255).toInt().coerceIn(0, 255), 0, 0, 0)
            canvas.drawPath(shadow, paint)

            val a = palette.colorAt(sheet.colorIndex)
            val b = palette.colorAt(sheet.colorIndex + 2)
            val tint = mixArgb(a, b, sheet.colorMix)
            val r = Color.red(tint)
            val g = Color.green(tint)
            val bl = Color.blue(tint)
            paint.shader = LinearGradient(
                0f, lineY, 0f, lineY + length,
                intArrayOf(
                    Color.argb((sheet.alpha * dim * 255).toInt().coerceIn(0, 255), (r + 255) / 2, (g + 255) / 2, (bl + 255) / 2),
                    Color.argb((sheet.alpha * 0.85f * dim * 255).toInt().coerceIn(0, 255), r, g, bl),
                    Color.argb((sheet.alpha * 0.55f * dim * 255).toInt().coerceIn(0, 255), r, g, bl),
                ),
                floatArrayOf(0f, 0.45f, 1f),
                Shader.TileMode.CLAMP,
            )
            canvas.drawPath(path, paint)
            paint.shader = null

            paint.style = Paint.Style.STROKE
            paint.strokeWidth = (minDim * 0.003f).coerceAtLeast(0.8f)
            paint.color = Color.argb((0.18f * dim * 255).toInt().coerceIn(0, 255), 255, 255, 255)
            canvas.drawPath(path, paint)
            paint.style = Paint.Style.FILL

            paint.color = Color.argb((0.7f * dim * 255).toInt().coerceIn(0, 255), 44, 53, 64)
            canvas.drawCircle(sheet.pinX * w, lineY, (minDim * 0.008f).coerceAtLeast(1.5f), paint)
        }

        paint.alpha = 255
        paint.shader = null
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
}

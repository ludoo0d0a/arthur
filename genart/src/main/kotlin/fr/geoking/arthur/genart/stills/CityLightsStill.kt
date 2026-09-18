package fr.geoking.arthur.genart.stills

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import fr.geoking.arthur.genart.AnimationPalette
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/** Bakes one static frame of City Night Lights for Auto/Ambient album art. */
internal object CityLightsStill {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)
    private val skylineColor = Color.rgb(0x06, 0x09, 0x12)
    private val windowWarm = Color.rgb(0xFF, 0xC8, 0x73)
    private const val BUILDING_COUNT = 16
    private const val LIGHT_COUNT = 40

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

        paint.shader = LinearGradient(
            0f, 0f, 0f, h,
            intArrayOf(0xFF060B1C.toInt(), 0xFF01030A.toInt()),
            null,
            Shader.TileMode.CLAMP,
        )
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        val rawWidths = FloatArray(BUILDING_COUNT) { 0.55f + rnd.nextFloat() * 0.7f }
        val total = rawWidths.sum()
        val xFracs = FloatArray(BUILDING_COUNT)
        val widthFracs = FloatArray(BUILDING_COUNT)
        val heightFracs = FloatArray(BUILDING_COUNT)
        var cursor = 0f
        for (i in 0 until BUILDING_COUNT) {
            widthFracs[i] = rawWidths[i] / total
            xFracs[i] = cursor
            cursor += widthFracs[i]
            heightFracs[i] = 0.14f + rnd.nextFloat() * 0.38f
        }
        for (i in 0 until BUILDING_COUNT) {
            val shade = 0.75f + rnd.nextFloat() * 0.3f
            val r = (Color.red(skylineColor) * shade).toInt().coerceIn(0, 255)
            val g = (Color.green(skylineColor) * shade).toInt().coerceIn(0, 255)
            val b = (Color.blue(skylineColor) * shade).toInt().coerceIn(0, 255)
            paint.color = Color.rgb(r, g, b)
            val left = xFracs[i] * w
            val top = h * (1f - heightFracs[i])
            val right = left + widthFracs[i] * w + 1f
            canvas.drawRect(left, top, right, h, paint)
        }

        val drift = phase + rotationDeg * PI.toFloat() / 180f
        for (i in 0 until LIGHT_COUNT) {
            val buildingIndex = i % BUILDING_COUNT
            val lx = (xFracs[buildingIndex] + (0.15f + rnd.nextFloat() * 0.7f) * widthFracs[buildingIndex]) * w
            val ly = h * (1f - (0.12f + rnd.nextFloat() * 0.78f) * heightFracs[buildingIndex])
            val twinkleFreq = 0.15f + rnd.nextFloat() * 0.45f
            val twinklePhase = rnd.nextFloat() * 2f * PI.toFloat()
            val twinkle = 0.5f + 0.5f * sin(drift * twinkleFreq + twinklePhase)
            val glowPulse = (0.6f + 0.4f * pulse * twinkle).coerceIn(0f, 1f)
            val tint = palette.colorAt(i)
            val r = (Color.red(tint) + 2 * Color.red(windowWarm)) / 3
            val g = (Color.green(tint) + 2 * Color.green(windowWarm)) / 3
            val b = (Color.blue(tint) + 2 * Color.blue(windowWarm)) / 3
            val glowR = 6.5f * glowPulse
            val alpha = (0.45f * glowPulse * 255).toInt().coerceIn(0, 255)
            paint.shader = RadialGradient(
                lx, ly, (glowR * 2.4f).coerceAtLeast(1f),
                Color.argb(alpha, r, g, b),
                Color.TRANSPARENT,
                Shader.TileMode.CLAMP,
            )
            canvas.drawCircle(lx, ly, glowR * 2.4f, paint)
            paint.shader = null
            paint.color = Color.argb((0.8f * glowPulse * 255).toInt().coerceIn(0, 255), r, g, b)
            canvas.drawCircle(lx, ly, 1.6f * glowPulse, paint)
        }
        paint.alpha = 255
    }
}

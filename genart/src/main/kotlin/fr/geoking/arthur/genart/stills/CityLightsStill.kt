package fr.geoking.arthur.genart.stills

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.graphics.toArgb
import fr.geoking.arthur.genart.AnimationPalette
import fr.geoking.arthur.genart.TonalPalette
import fr.geoking.arthur.genart.lerp
import fr.geoking.arthur.genart.phase01
import fr.geoking.arthur.genart.seededRange
import fr.geoking.arthur.genart.seededUnit
import fr.geoking.arthur.genart.sin01
import kotlin.math.PI

/** Frozen night city frame for Auto / Ambient album art. */
internal object CityLightsStill {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)
    private val skylineColor = 0xFF0B1430.toInt()
    private val windowWarm = 0xFFFFC873.toInt()
    private val windowCold = 0xFF8FE8FF.toInt()
    private const val BUILDING_COUNT = 7
    private const val LIGHT_COUNT = 40
    private const val CITY_LOOP_SECONDS = 16f
    private const val LIGHT_CYCLE_SECONDS = 8f
    private const val TWO_PI = 6.2831855f

    private fun smooth01(value: Float): Float = value * value * (3f - 2f * value)

    private fun colorScaled(color: Int, factor: Float): Int =
        Color.argb(
            255,
            (Color.red(color) * factor).toInt().coerceIn(0, 255),
            (Color.green(color) * factor).toInt().coerceIn(0, 255),
            (Color.blue(color) * factor).toInt().coerceIn(0, 255),
        )

    private fun mixArgb(a: Int, b: Int, amount: Float): Int {
        val t = amount.coerceIn(0f, 1f)
        return Color.argb(
            255,
            (Color.red(a) + (Color.red(b) - Color.red(a)) * t).toInt().coerceIn(0, 255),
            (Color.green(a) + (Color.green(b) - Color.green(a)) * t).toInt().coerceIn(0, 255),
            (Color.blue(a) + (Color.blue(b) - Color.blue(a)) * t).toInt().coerceIn(0, 255),
        )
    }

    private fun withAlphaArgb(color: Int, alpha: Float): Int =
        Color.argb((alpha * 255f).toInt().coerceIn(0, 255), Color.red(color), Color.green(color), Color.blue(color))

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
        val time01 = phase01(phase / TWO_PI)
        val dim = (0.85f + 0.15f * pulse).coerceIn(0.55f, 1f)
        val lightTurn = CITY_LOOP_SECONDS / LIGHT_CYCLE_SECONDS

        paint.shader = LinearGradient(
            0f, 0f, 0f, h,
            intArrayOf(
                colorScaled(0xFF030510.toInt(), dim),
                colorScaled(0xFF0A1230.toInt(), dim),
                colorScaled(0xFF131F3F.toInt(), dim),
            ),
            floatArrayOf(0f, 0.55f, 1f),
            Shader.TileMode.CLAMP,
        )
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        val horizonY = h * 0.8f
        val halo = (0.30f + 0.16f * sin01(time01 * TWO_PI * 1.3f + 0.8f)) * dim
        val haloColor = TonalPalette.mix(
            ComposeColor(0xFFFFC98A),
            TonalPalette.pick(palette.colorsAsComposeColor, 0),
            0.25f,
        )
        val haloArgb = TonalPalette.withAlpha(haloColor, halo).toArgb()
        paint.shader = RadialGradient(
            w * 0.5f, horizonY, h * 0.32f,
            intArrayOf(withAlphaArgb(haloArgb, halo), Color.TRANSPARENT),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(w * 0.5f, horizonY, h * 0.32f, paint)
        paint.shader = null

        val starCount = 16
        for (i in 0 until starCount) {
            val x = seededUnit(i * 13 + 7) * w
            val y = seededUnit(i * 29 + 3) * h * 0.62f
            val r = minDim * (0.0009f + seededUnit(i * 37 + 11) * 0.0013f)
            val tw = sin01(time01 * TWO_PI * 1.6f + seededUnit(i * 41 + 5) * TWO_PI)
            val a = (0.16f + 0.5f * tw * tw) * dim
            paint.color = Color.argb((a * 255f).toInt().coerceIn(0, 255), 232, 242, 255)
            canvas.drawCircle(x, y, r, paint)
        }

        val rawWidths = FloatArray(BUILDING_COUNT) { seededRange(it * 31 + 5, 0.55f, 1.25f) }
        val total = rawWidths.sum()
        val xFracs = FloatArray(BUILDING_COUNT)
        val widthFracs = FloatArray(BUILDING_COUNT)
        val heightFracs = FloatArray(BUILDING_COUNT)
        var cursor = 0f
        for (i in 0 until BUILDING_COUNT) {
            widthFracs[i] = rawWidths[i] / total
            xFracs[i] = cursor
            cursor += widthFracs[i]
            heightFracs[i] = seededRange(i * 43 + 9, 0.30f, 0.62f)
        }
        for (i in 0 until BUILDING_COUNT) {
            val shade = seededRange(i * 97 + 29, 0.78f, 1.02f)
            paint.color = Color.argb(
                255,
                (Color.red(skylineColor) * shade).toInt().coerceIn(0, 255),
                (Color.green(skylineColor) * shade).toInt().coerceIn(0, 255),
                (Color.blue(skylineColor) * shade).toInt().coerceIn(0, 255),
            )
            val left = xFracs[i] * w
            val top = h * (1f - heightFracs[i])
            val right = left + widthFracs[i] * w
            canvas.drawRect(left, top, right + 1f, top + h * heightFracs[i], paint)
            paint.color = Color.argb(
                255,
                (Color.red(0xFF3A5178) * shade).toInt().coerceIn(0, 255),
                (Color.green(0xFF3A5178) * shade).toInt().coerceIn(0, 255),
                (Color.blue(0xFF3A5178) * shade).toInt().coerceIn(0, 255),
            )
            paint.strokeWidth = if (minDim * 0.0012f > 1f) minDim * 0.0012f else 1f
            canvas.drawLine(left, top, right, top, paint)
        }

        for (i in 0 until LIGHT_COUNT) {
            val bi = i % BUILDING_COUNT
            val lx = (xFracs[bi] + (0.15f + seededRange(i * 53 + 13, 0f, 0.7f)) * widthFracs[bi]) * w
            val ly = h * (1f - (0.12f + seededRange(i * 61 + 17, 0f, 0.78f)) * heightFracs[bi])
            val radiusFrac = 0.011f + seededRange(i * 71 + 19, 0f, 0.008f)
            val cold = seededRange(i * 89 + 31, 0f, 1f) > 0.38f
            val lightPhase = seededRange(i * 83 + 23, 0f, TWO_PI)
            val breathe = seededRange(i * 97 + 37, 0.5f, 1.2f)
            val lit = smooth01(sin01(time01 * TWO_PI * lightTurn + lightPhase))
            if (lit < 0.02f) continue
            val intensity = (0.62f + 0.38f * sin01(time01 * TWO_PI * breathe + lightPhase * 0.8f)) * dim
            val color = if (cold) mixArgb(windowWarm, windowCold, 0.35f) else mixArgb(windowWarm, palette.colorAt(i), 0.2f)
            val coreAlpha = (lit * intensity * 0.85f).coerceIn(0f, 1f)
            val glowAlpha = (lit * intensity * 1.15f).coerceIn(0f, 1f)
            val radius = radiusFrac * minDim * 2f
            val glowArgb = withAlphaArgb(color, glowAlpha)
            paint.shader = RadialGradient(
                lx, ly, radius * 2.2f,
                intArrayOf(glowArgb, Color.TRANSPARENT),
                floatArrayOf(0f, 1f),
                Shader.TileMode.CLAMP,
            )
            canvas.drawCircle(lx, ly, radius * 2.2f, paint)
            paint.shader = null
            paint.color = withAlphaArgb(color, coreAlpha)
            canvas.drawCircle(lx, ly, radius * 0.55f, paint)
        }

        val trails = listOf(
            CityLightsStill.TrailSeed(yFrac = 0.74f, fromLeft = true, lengthFrac = 0.05f),
            CityLightsStill.TrailSeed(yFrac = 0.84f, fromLeft = false, lengthFrac = 0.045f),
        )
        for (trail in trails) {
            val stretched = smooth01(time01)
            val len = trail.lengthFrac * minDim
            val headX = if (trail.fromLeft) lerp(-len, w + len, stretched) else lerp(w + len, -len, stretched)
            val segStartX = if (trail.fromLeft) headX - len else headX
            val segEndX = if (trail.fromLeft) headX else headX + len
            val y = trail.yFrac * h
            val trailAlpha = (0.38f * dim * 1.28f).coerceIn(0.05f, 0.8f)
            paint.color = withAlphaArgb(0xFFFFE3B0.toInt(), trailAlpha)
            paint.strokeWidth = minDim * 0.0035f
            canvas.drawLine(segStartX, y, segEndX, y, paint)
            paint.color = withAlphaArgb(0xFFFFF3DA.toInt(), (0.9f * dim * 1.28f).coerceIn(0.1f, 1f))
            canvas.drawCircle(headX, y, minDim * 0.006f, paint)
        }
    }

    private data class TrailSeed(
        val yFrac: Float,
        val fromLeft: Boolean,
        val lengthFrac: Float,
    )
}

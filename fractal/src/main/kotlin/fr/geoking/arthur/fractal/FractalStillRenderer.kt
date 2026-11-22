package fr.geoking.arthur.fractal

import android.graphics.Bitmap
import android.graphics.Canvas
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.sin

/**
 * android.graphics still baker for Fractal Presets (Auto album art, browse thumbs).
 * No Compose / no animation — [generation] shifts seed so Ambient Rotation looks distinct.
 */
object FractalStillRenderer {
    private const val InsideRgb = 0xFF020617.toInt()

    fun draw(
        canvas: Canvas,
        type: FractalType,
        size: Int,
        generation: Long = 0L,
        maxIter: Int = 220,
    ) {
        val bitmap = render(type, size, generation, maxIter)
        canvas.drawBitmap(bitmap, 0f, 0f, null)
        bitmap.recycle()
    }

    fun render(
        type: FractalType,
        size: Int,
        generation: Long = 0L,
        maxIter: Int = 220,
    ): Bitmap {
        val side = size.coerceAtLeast(64)
        val seed = type.ordinal.toLong() xor (generation * 0x9E3779B9L)
        val pixels = IntArray(side * side)
        val (cx, cy) = fractalCenter(type)
        val span = stillSpan(type)
        val scale = span / side
        val (juliaCx, juliaCy) = juliaConstant(type, generation)
        val hueSeed = seed.toInt()

        for (py in 0 until side) {
            for (px in 0 until side) {
                val re = ((px - side / 2.0) * scale + cx).toFloat()
                val im = ((py - side / 2.0) * scale + cy).toFloat()
                val continuous = escape(type, re, im, maxIter, juliaCx, juliaCy)
                pixels[py * side + px] = if (continuous < 0f) {
                    InsideRgb
                } else {
                    val t = (ln(1.0 + continuous) / ln(1.0 + maxIter)).toFloat()
                    val cycles = 2.6f
                    val u = ((t * cycles) + (hueSeed and 0xFF) / 360f).mod(1f)
                    FractalCoherentPalette.sampleArgb(hueSeed, u)
                }
            }
        }
        return Bitmap.createBitmap(pixels, side, side, Bitmap.Config.ARGB_8888)
    }

    fun typeForArtworkId(artworkId: String): FractalType? {
        if (!artworkId.startsWith("fractal.")) return null
        val key = artworkId.removePrefix("fractal.").lowercase()
        return typeForKey(key)
    }

    fun typeForKey(key: String): FractalType? = when (key.lowercase()) {
        "mandelbrot" -> FractalType.Mandelbrot
        "julia" -> FractalType.Julia
        "burningship" -> FractalType.BurningShip
        "tricorn" -> FractalType.Tricorn
        "multibrot" -> FractalType.Multibrot
        "celtic" -> FractalType.Celtic
        "buffalo" -> FractalType.Buffalo
        "phoenix" -> FractalType.Phoenix
        "nova" -> FractalType.Nova
        "newton" -> FractalType.Newton
        "mandelbrotglow" -> FractalType.MandelbrotGlow
        "juliatouch" -> FractalType.JuliaTouch
        else -> null
    }

    private fun stillSpan(type: FractalType): Double = when (type) {
        FractalType.Julia, FractalType.JuliaTouch -> 2.6
        FractalType.BurningShip -> 2.2
        FractalType.Tricorn -> 2.6
        FractalType.Multibrot -> 2.4
        FractalType.Celtic -> 2.5
        FractalType.Buffalo -> 2.4
        FractalType.Phoenix -> 2.8
        FractalType.Nova -> 2.5
        FractalType.Newton -> 2.8
        FractalType.MandelbrotGlow -> 2.5
        FractalType.Mandelbrot -> 2.6
    }

    private fun juliaConstant(type: FractalType, generation: Long): Pair<Float, Float> {
        if (type != FractalType.Julia && type != FractalType.JuliaTouch) {
            return 0f to 0f
        }
        val a = ((generation % 240L).toFloat() / 240f) * (2f * PI.toFloat())
        return (-0.7f + 0.12f * cos(a)) to (0.27015f + 0.1f * sin(a * 1.15f))
    }

    private fun escape(
        type: FractalType,
        re: Float,
        im: Float,
        maxIter: Int,
        juliaCx: Float,
        juliaCy: Float,
    ): Float = when (type) {
        FractalType.Mandelbrot, FractalType.MandelbrotGlow -> mandelbrot(re, im, maxIter)
        FractalType.Julia, FractalType.JuliaTouch -> julia(re, im, maxIter, juliaCx, juliaCy)
        FractalType.BurningShip -> burningShip(re, im, maxIter)
        FractalType.Tricorn -> tricorn(re, im, maxIter)
        FractalType.Multibrot -> multibrot(re, im, maxIter)
        FractalType.Celtic -> celtic(re, im, maxIter)
        FractalType.Buffalo -> buffalo(re, im, maxIter)
        FractalType.Phoenix -> phoenix(re, im, maxIter)
        FractalType.Nova -> nova(re, im, maxIter)
        FractalType.Newton -> newton(re, im, maxIter)
    }

    private fun smoothEscape(n: Int, zr2: Double, zi2: Double, power: Double = 2.0): Float {
        val logZn = ln((zr2 + zi2).coerceAtLeast(1e-12)) / 2.0
        val nu = ln((logZn / ln(2.0)).coerceAtLeast(1e-12)) / ln(power)
        return (n + 1.0 - nu).toFloat()
    }

    private fun mandelbrot(cr: Float, ci: Float, maxIter: Int): Float {
        var zr = 0.0
        var zi = 0.0
        var n = 0
        while (n < maxIter) {
            val zr2 = zr * zr
            val zi2 = zi * zi
            if (zr2 + zi2 > 4.0) return smoothEscape(n, zr2, zi2)
            val newZi = 2 * zr * zi + ci
            val newZr = zr2 - zi2 + cr
            zi = newZi
            zr = newZr
            n++
        }
        return -1f
    }

    private fun julia(
        zrStart: Float,
        ziStart: Float,
        maxIter: Int,
        cr: Float,
        ci: Float,
    ): Float {
        var zr = zrStart.toDouble()
        var zi = ziStart.toDouble()
        var n = 0
        while (n < maxIter) {
            val zr2 = zr * zr
            val zi2 = zi * zi
            if (zr2 + zi2 > 4.0) return smoothEscape(n, zr2, zi2)
            val newZi = 2 * zr * zi + ci
            val newZr = zr2 - zi2 + cr
            zi = newZi
            zr = newZr
            n++
        }
        return -1f
    }

    private fun burningShip(cr: Float, ci: Float, maxIter: Int): Float {
        var zr = 0.0
        var zi = 0.0
        var n = 0
        while (n < maxIter) {
            val zr2 = zr * zr
            val zi2 = zi * zi
            if (zr2 + zi2 > 4.0) return smoothEscape(n, zr2, zi2)
            val newZr = zr2 - zi2 + cr
            val newZi = abs(2.0 * zr * zi) + ci
            zr = abs(newZr)
            zi = newZi
            n++
        }
        return -1f
    }

    private fun tricorn(cr: Float, ci: Float, maxIter: Int): Float {
        var zr = 0.0
        var zi = 0.0
        var n = 0
        while (n < maxIter) {
            val zr2 = zr * zr
            val zi2 = zi * zi
            if (zr2 + zi2 > 4.0) return smoothEscape(n, zr2, zi2)
            val nextZr = zr2 - zi2 + cr
            val nextZi = -2.0 * zr * zi + ci
            zr = nextZr
            zi = nextZi
            n++
        }
        return -1f
    }

    private fun multibrot(cr: Float, ci: Float, maxIter: Int): Float {
        var zr = 0.0
        var zi = 0.0
        var n = 0
        while (n < maxIter) {
            val zr2 = zr * zr
            val zi2 = zi * zi
            if (zr2 + zi2 > 4.0) return smoothEscape(n, zr2, zi2, power = 3.0)
            val nextZr = zr * (zr2 - 3.0 * zi2) + cr
            val nextZi = zi * (3.0 * zr2 - zi2) + ci
            zr = nextZr
            zi = nextZi
            n++
        }
        return -1f
    }

    private fun celtic(cr: Float, ci: Float, maxIter: Int): Float {
        var zr = 0.0
        var zi = 0.0
        var n = 0
        while (n < maxIter) {
            val zr2 = zr * zr
            val zi2 = zi * zi
            if (zr2 + zi2 > 4.0) return smoothEscape(n, zr2, zi2)
            val nextZr = abs(zr2 - zi2) + cr
            val nextZi = 2.0 * zr * zi + ci
            zr = nextZr
            zi = nextZi
            n++
        }
        return -1f
    }

    private fun buffalo(cr: Float, ci: Float, maxIter: Int): Float {
        var zr = 0.0
        var zi = 0.0
        var n = 0
        while (n < maxIter) {
            val zr2 = zr * zr
            val zi2 = zi * zi
            if (zr2 + zi2 > 4.0) return smoothEscape(n, zr2, zi2)
            val nextZr = abs(zr2 - zi2) + cr
            val nextZi = -abs(2.0 * zr * zi) + ci
            zr = nextZr
            zi = nextZi
            n++
        }
        return -1f
    }

    private fun phoenix(zrStart: Float, ziStart: Float, maxIter: Int): Float {
        var zr = zrStart.toDouble()
        var zi = ziStart.toDouble()
        var pr = 0.0
        var pi = 0.0
        val cr = 0.5667
        val ci = -0.5
        var n = 0
        while (n < maxIter) {
            val zr2 = zr * zr
            val zi2 = zi * zi
            if (zr2 + zi2 > 4.0) return smoothEscape(n, zr2, zi2)
            val nextZr = zr2 - zi2 + cr + ci * pr
            val nextZi = 2.0 * zr * zi + ci * pi
            pr = zr
            pi = zi
            zr = nextZr
            zi = nextZi
            n++
        }
        return -1f
    }

    private fun nova(cr: Float, ci: Float, maxIter: Int): Float {
        var zr = 0.0
        var zi = 0.0
        var n = 0
        while (n < maxIter) {
            val zr2 = zr * zr
            val zi2 = zi * zi
            if (zr2 + zi2 > 4.0) return smoothEscape(n, zr2, zi2, power = 4.0)
            val z3r = zr * (zr2 - 3.0 * zi2)
            val z3i = zi * (3.0 * zr2 - zi2)
            val nextZr = z3r * zr - z3i * zi + cr
            val nextZi = z3r * zi + z3i * zr + ci
            zr = nextZr
            zi = nextZi
            n++
        }
        return -1f
    }

    private fun newton(zrStart: Float, ziStart: Float, maxIter: Int): Float {
        var zr = zrStart.toDouble()
        var zi = ziStart.toDouble()
        var n = 0
        while (n < maxIter) {
            val zr2 = zr * zr
            val zi2 = zi * zi
            val z2r = zr2 - zi2
            val z2i = 2.0 * zr * zi
            val invDen = 1.0 / (z2r * z2r + z2i * z2i).coerceAtLeast(1e-12)
            val invR = z2r * invDen
            val invI = -z2i * invDen
            val nextZr = (2.0 / 3.0) * zr + (1.0 / 3.0) * invR
            val nextZi = (2.0 / 3.0) * zi + (1.0 / 3.0) * invI
            val dr = nextZr - zr
            val di = nextZi - zi
            zr = nextZr
            zi = nextZi
            if (dr * dr + di * di < 1e-10) {
                val angle = atan2(zi, zr)
                return ((angle / (2.0 * Math.PI) + 0.5 + n * 0.002) * maxIter * 0.15).toFloat()
            }
            n++
        }
        return -1f
    }
}

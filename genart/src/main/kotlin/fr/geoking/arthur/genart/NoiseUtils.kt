package fr.geoking.arthur.genart

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin

/** Smoothstep-interpolated value noise on the integer lattice, hashed via [seededUnit]. */
internal fun valueNoise2D(x: Float, y: Float, seedOffset: Int = 0): Float {
    val xi = floor(x).toInt()
    val yi = floor(y).toInt()
    val xf = x - xi
    val yf = y - yi
    fun hash(ix: Int, iy: Int) = seededUnit(ix * 374761393 + iy * 668265263 + seedOffset)
    val a = hash(xi, yi)
    val b = hash(xi + 1, yi)
    val c = hash(xi, yi + 1)
    val d = hash(xi + 1, yi + 1)
    val u = xf * xf * (3f - 2f * xf)
    val v = yf * yf * (3f - 2f * yf)
    val top = a + (b - a) * u
    val bottom = c + (d - c) * u
    return top + (bottom - top) * v
}

/**
 * Fractal Brownian motion: a few octaves of [valueNoise2D] summed together for an organic,
 * non-repeating-looking drift instead of a single pure sine term. Result is roughly `[0, 1]`.
 */
internal fun fbm2D(x: Float, y: Float, octaves: Int = 3, seedOffset: Int = 0): Float {
    var total = 0f
    var amplitude = 0.5f
    var frequency = 1f
    var maxValue = 0f
    for (i in 0 until octaves) {
        total += valueNoise2D(x * frequency, y * frequency, seedOffset + i * 101) * amplitude
        maxValue += amplitude
        amplitude *= 0.5f
        frequency *= 2f
    }
    return total / maxValue
}

/**
 * A seamlessly-looping fbm value driven by a `[0, 1]` phase: samples a fixed circle in 2D noise
 * space, so `t = 0` and `t = 1` land on the exact same point — safe to drive with the same
 * `RepeatMode.Restart` time driver every other engine uses, with no jump at the loop seam.
 * Result is roughly `[0, 1]`; callers wanting a signed wobble can do `loopedFbm(...) * 2f - 1f`.
 */
internal fun loopedFbm(t: Float, radius: Float = 1.6f, octaves: Int = 3, seedOffset: Int = 0): Float {
    val angle = t * 2f * PI.toFloat()
    val x = cos(angle) * radius
    val y = sin(angle) * radius
    return fbm2D(x, y, octaves, seedOffset)
}

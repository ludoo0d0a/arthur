package fr.geoking.arthur.genart.stills

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import fr.geoking.arthur.genart.AnimationPalette
import fr.geoking.arthur.genart.fbm2D
import fr.geoking.arthur.genart.loopedFbm
import fr.geoking.arthur.genart.phase01
import kotlin.math.abs
import kotlin.math.min

/** Bakes one frozen/live frame of Shady Marbled Paper for Auto/Ambient. */
internal object ShadyMarbledPaperStill {
    const val LOOP_DURATION_MS = 11_000

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)

    private fun smoothstep(edge0: Float, edge1: Float, x: Float): Float {
        val t = ((x - edge0) / (edge1 - edge0)).coerceIn(0f, 1f)
        return t * t * (3f - 2f * t)
    }

    private fun mixChannel(a: Int, b: Int, t: Float): Int =
        (a + (b - a) * t).toInt().coerceIn(0, 255)

    private fun marbleField(u: Float, v: Float, loop: Float): Triple<Float, Float, Float> {
        val flowU = loopedFbm(loop, radius = 1.9f, octaves = 3, seedOffset = 149) * 0.62f
        val flowV = loopedFbm(loop, radius = 1.9f, octaves = 3, seedOffset = 251) * 0.62f
        val px = u * 4.4f + flowU
        val py = v * 4.4f + flowV
        val wx = fbm2D(px, py, octaves = 3, seedOffset = 1493) * 1.15f - 0.575f
        val wy = fbm2D(px + 4.7f, py + 2.3f, octaves = 3, seedOffset = 1494) * 1.15f - 0.575f
        val qx = px + wx
        val qy = py + wy
        val field = fbm2D(qx, qy, octaves = 4, seedOffset = 1495)
        val detail = fbm2D(qx * 2.3f + 1.7f, qy * 2.3f, octaves = 3, seedOffset = 1496)
        return Triple(field, detail, abs(field - detail))
    }

    fun draw(
        canvas: Canvas,
        size: Int,
        generation: Long,
        phase: Float,
        rotationDeg: Float,
        pulse: Float,
        palette: AnimationPalette,
        width: Int = size,
        height: Int = size,
        isActive: Boolean = true,
    ) {
        @Suppress("UNUSED_PARAMETER")
        val _unused = generation to rotationDeg
        val w = width.toFloat().coerceAtLeast(1f)
        val h = height.toFloat().coerceAtLeast(1f)
        val loop = phase01(phase)
        val dim = if (isActive) 1f else 0.55f

        val primary = palette.primary
        val secondary = palette.secondary
        val tertiary = palette.tertiary
        val ink = Color.rgb(
            (Color.red(primary) * 0.12f).toInt(),
            (Color.green(primary) * 0.10f).toInt(),
            (Color.blue(primary) * 0.18f).toInt(),
        )

        val gridSize = 56
        val cellW = w / gridSize
        val cellH = h / gridSize

        paint.style = Paint.Style.FILL

        for (iy in 0 until gridSize) {
            for (ix in 0 until gridSize) {
                val u = (ix + 0.5f) / gridSize
                val v = (iy + 0.5f) / gridSize

                val (field, detail, ridgeGap) = marbleField(u, v, loop)
                val band = (field * field * (3f - 2f * field)).coerceIn(0f, 1f)

                val r0: Int
                val g0: Int
                val b0: Int
                when {
                    band < 0.34f -> {
                        val t = band / 0.34f
                        r0 = mixChannel(Color.red(secondary), Color.red(primary), t)
                        g0 = mixChannel(Color.green(secondary), Color.green(primary), t)
                        b0 = mixChannel(Color.blue(secondary), Color.blue(primary), t)
                    }
                    band < 0.67f -> {
                        val t = (band - 0.34f) / 0.33f
                        r0 = mixChannel(Color.red(primary), Color.red(tertiary), t)
                        g0 = mixChannel(Color.green(primary), Color.green(tertiary), t)
                        b0 = mixChannel(Color.blue(primary), Color.blue(tertiary), t)
                    }
                    else -> {
                        val t = (band - 0.67f) / 0.33f
                        r0 = mixChannel(Color.red(tertiary), Color.red(secondary), t)
                        g0 = mixChannel(Color.green(tertiary), Color.green(secondary), t)
                        b0 = mixChannel(Color.blue(tertiary), Color.blue(secondary), t)
                    }
                }

                val edgeDist = min(min(abs(field - 0.33f), abs(field - 0.5f)), abs(field - 0.67f))
                val inkLine = smoothstep(0.09f, 0.018f, edgeDist)
                val veinInk = smoothstep(0.72f, 0.90f, ridgeGap) * 0.85f
                val inkStrength = (inkLine * 0.75f + veinInk * 0.55f).coerceIn(0f, 1f)

                var r = r0 * (1f - inkStrength) + Color.red(ink) * inkStrength
                var g = g0 * (1f - inkStrength) + Color.green(ink) * inkStrength
                var b = b0 * (1f - inkStrength) + Color.blue(ink) * inkStrength

                val sheen = smoothstep(0.38f, 0.62f, detail) * 0.14f * dim
                r = r * (1f - sheen) + Color.red(tertiary) * sheen
                g = g * (1f - sheen) + Color.green(tertiary) * sheen
                b = b * (1f - sheen) + Color.blue(tertiary) * sheen

                if (pulse > 0.5f) {
                    val tFrac = (pulse - 0.5f) * 0.16f
                    r = r * (1f - tFrac) + Color.red(tertiary) * tFrac
                    g = g * (1f - tFrac) + Color.green(tertiary) * tFrac
                    b = b * (1f - tFrac) + Color.blue(tertiary) * tFrac
                }

                r = (r * dim).coerceIn(0f, 255f)
                g = (g * dim).coerceIn(0f, 255f)
                b = (b * dim).coerceIn(0f, 255f)

                val alpha = if (isActive) (0.92f + 0.08f * pulse) else 0.82f
                paint.color = Color.argb((alpha * 255).toInt(), r.toInt(), g.toInt(), b.toInt())

                canvas.drawRect(
                    ix * cellW,
                    iy * cellH,
                    (ix + 1) * cellW + 1f,
                    (iy + 1) * cellH + 1f,
                    paint,
                )
            }
        }
    }
}
package fr.geoking.arthur.genart.stills

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import fr.geoking.arthur.genart.AnimationPalette
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** One full grain + gradient drift cycle at speed 1.0 (see engine tween). */
internal const val PAPER_GRAIN_GRADIENT_LOOP_SEC = 11f

/** Bakes one frozen/live frame of Paper Grain Gradient for Auto/Ambient. */
internal object PaperGrainGradientStill {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    private fun pseudoNoise(u: Float, v: Float): Float {
        val sinVal = sin(u * 12.9898f + v * 78.233f) * 43758.5453f
        return sinVal - kotlin.math.floor(sinVal)
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

        val loopT = (phase % PAPER_GRAIN_GRADIENT_LOOP_SEC) / PAPER_GRAIN_GRADIENT_LOOP_SEC
        val angle = loopT * 2f * PI.toFloat()

        val primary = palette.primary
        val secondary = palette.secondary
        val tertiary = palette.tertiary

        val p1x = 0.32f + 0.17f * sin(angle)
        val p1y = 0.34f + 0.15f * cos(angle)

        val p2x = 0.68f + 0.16f * cos(angle + 1.05f)
        val p2y = 0.66f + 0.17f * sin(angle + 0.75f)

        val p3x = 0.5f + 0.21f * sin(angle * 2f)
        val p3y = 0.5f + 0.19f * cos(angle * 2f)

        val gridSize = 40
        val cellW = w / gridSize
        val cellH = h / gridSize

        paint.style = Paint.Style.FILL

        for (iy in 0 until gridSize) {
            for (ix in 0 until gridSize) {
                val u = ix.toFloat() / gridSize
                val v = iy.toFloat() / gridSize

                val d1 = sqrt((u - p1x) * (u - p1x) + (v - p1y) * (v - p1y))
                val d2 = sqrt((u - p2x) * (u - p2x) + (v - p2y) * (v - p2y))
                val d3 = sqrt((u - p3x) * (u - p3x) + (v - p3y) * (v - p3y))

                val w1 = 1f / (d1 + 0.12f)
                val w2 = 1f / (d2 + 0.12f)
                val w3 = 1f / (d3 + 0.12f)
                val totalWeight = w1 + w2 + w3

                var r = (
                    Color.red(primary) * (w1 / totalWeight) +
                        Color.red(secondary) * (w2 / totalWeight) +
                        Color.red(tertiary) * (w3 / totalWeight)
                    ) / 255f
                var g = (
                    Color.green(primary) * (w1 / totalWeight) +
                        Color.green(secondary) * (w2 / totalWeight) +
                        Color.green(tertiary) * (w3 / totalWeight)
                    ) / 255f
                var b = (
                    Color.blue(primary) * (w1 / totalWeight) +
                        Color.blue(secondary) * (w2 / totalWeight) +
                        Color.blue(tertiary) * (w3 / totalWeight)
                    ) / 255f

                // Soft vertical + diagonal gradient bands (paper wash)
                val bandV = 0.5f + 0.5f * sin(v * 5f * PI.toFloat() + angle * 0.5f)
                val bandD = 0.5f + 0.5f * cos((u + v) * 4f * PI.toFloat() - angle * 0.35f)
                val bandMix = (bandV * 0.55f + bandD * 0.45f).coerceIn(0f, 1f)
                val lift = 0.88f + 0.24f * bandMix
                val shadow = 0.92f + 0.16f * (1f - bandMix)
                r = (r * lift * shadow).coerceIn(0f, 1f)
                g = (g * lift * shadow).coerceIn(0f, 1f)
                b = (b * lift * shadow).coerceIn(0f, 1f)

                // Multi-scale paper grain (fine speckle + coarser fiber)
                val fineGrain = (pseudoNoise(u * 220f + angle * 0.04f, v * 220f) - 0.5f) * 0.11f
                val fiber = (pseudoNoise(u * 52f + v * 18f + angle * 0.08f, v * 64f) - 0.5f) * 0.065f
                val macroGrain = (pseudoNoise(u * 16f + 3.1f, v * 14f + 2.7f) - 0.5f) * 0.035f
                val grain = fineGrain + fiber + macroGrain
                r = (r + grain).coerceIn(0f, 1f)
                g = (g + grain).coerceIn(0f, 1f)
                b = (b + grain).coerceIn(0f, 1f)

                val alpha = if (isActive) (0.84f + 0.12f * pulse) else 0.72f
                paint.color = Color.argb(
                    (alpha * 255).toInt(),
                    (r * 255).toInt(),
                    (g * 255).toInt(),
                    (b * 255).toInt(),
                )

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
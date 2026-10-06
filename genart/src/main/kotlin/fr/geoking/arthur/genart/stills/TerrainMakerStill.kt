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

/** Bakes one frozen frame of Landscape / Terrain Maker for Auto/Ambient album art. */
internal object TerrainMakerStill {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)

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
        val dim = 0.65f + 0.35f * pulse
        val cycle = ((phase / (2f * PI.toFloat()) + rotationDeg / 360f) % 1f + 1f) % 1f
        val biome = cycle * 3f

        val rock = intArrayOf(0x6B, 0x5A, 0x4A)
        val grass = intArrayOf(0x4A, 0x7A, 0x4E)
        val snow = intArrayOf(0xE8, 0xEE, 0xF4)
        val biomeRgb = when {
            biome < 1f -> lerpRgb(rock, grass, smooth(biome))
            biome < 2f -> lerpRgb(grass, snow, smooth(biome - 1f))
            else -> lerpRgb(snow, rock, smooth(biome - 2f))
        }

        val skyTop = Color.rgb(
            (0x7B + (0xC9 - 0x7B) * (0.5f * ((sin(cycle * 2f * PI) + 1) / 2f).toFloat())).toInt(),
            (0xA3 + (0xD6 - 0xA3) * 0.4f).toInt(),
            (0xC9 + (0xE8 - 0xC9) * 0.4f).toInt(),
        )
        val skyBot = Color.rgb(
            ((0xB8 + biomeRgb[0]) / 2),
            ((0xC9 + biomeRgb[1]) / 2),
            ((0xA8 + biomeRgb[2]) / 2),
        )
        paint.shader = LinearGradient(
            0f, 0f, 0f, h,
            intArrayOf(skyTop, skyBot),
            null,
            Shader.TileMode.CLAMP,
        )
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        val layerCount = 4
        val samples = 40
        for (layer in 0 until layerCount) {
            val distanceT = 1f - layer / (layerCount - 1f)
            val baselineFrac = 0.72f - distanceT * 0.28f
            val amplitudeFrac = 0.08f + distanceT * 0.22f
            val noiseScale = 0.7f + distanceT * 1.4f
            val path = Path()
            path.moveTo(0f, h)
            for (s in 0..samples) {
                val xFrac = s / samples.toFloat()
                val n1 = 0.5f + 0.5f * sin((cycle * 2f * PI + xFrac * noiseScale * 4f + layer).toFloat())
                val n2 = 0.5f + 0.5f * cos((cycle * PI + xFrac * noiseScale * 2.2f + layer * 1.7f).toFloat())
                val rise = 0.55f + 0.45f * n1
                val valley = 0.75f + 0.25f * n2
                val y = h * (baselineFrac - amplitudeFrac * rise * valley)
                val x = xFrac * w
                if (s == 0) path.lineTo(0f, y) else path.lineTo(x, y)
            }
            path.lineTo(w, h)
            path.close()
            val tint = palette.colorAt(layer)
            val r = ((Color.red(tint) + biomeRgb[0]) / 2)
            val g = ((Color.green(tint) + biomeRgb[1]) / 2)
            val b = ((Color.blue(tint) + biomeRgb[2]) / 2)
            val alpha = ((0.35f + distanceT * 0.55f) * dim * 255).toInt().coerceIn(0, 255)
            paint.color = Color.argb(alpha, r, g, b)
            canvas.drawPath(path, paint)
            rnd.nextFloat() // keep generation seed used
        }
        paint.alpha = 255
    }

    private fun smooth(t: Float): Float {
        val x = t.coerceIn(0f, 1f)
        return x * x * (3f - 2f * x)
    }

    private fun lerpRgb(a: IntArray, b: IntArray, t: Float): IntArray = intArrayOf(
        (a[0] + (b[0] - a[0]) * t).toInt(),
        (a[1] + (b[1] - a[1]) * t).toInt(),
        (a[2] + (b[2] - a[2]) * t).toInt(),
    )
}

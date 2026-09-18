package fr.geoking.arthur.genart

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.RadialGradient
import android.graphics.Shader
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * android.graphics still bakers for Auto album art (no Compose / no animation).
 * [generation] selects palette index and phase/rotation so Ambient Rotation URIs look distinct.
 *
 * Near-4K path: callers bake at ~2160px; this applies fill-scale + polish FX so every engine
 * lands high-contrast, anti-aliased, and screen-filling without a pixelated look.
 */
object GenartStillRenderer {
    fun draw(
        canvas: Canvas,
        engineId: GenartEngineId,
        size: Int,
        generation: Long,
        palette: AnimationPalette = AnimationPalettes.fromGeneration(generation),
    ) {
        val phase = ((generation % 360L).toFloat() / 360f) * (2f * PI.toFloat())
        val rotationDeg = ((generation * 37L) % 360L).toFloat()
        val pulse = 0.35f + ((generation % 100L).toFloat() / 100f) * 0.65f

        canvas.save()
        val cx = size * 0.5f
        val cy = size * 0.5f
        canvas.scale(GenartStillFx.FILL_SCALE, GenartStillFx.FILL_SCALE, cx, cy)
        GenartRegistry.descriptorFor(engineId).renderStill(
            canvas,
            size,
            generation,
            phase,
            rotationDeg,
            pulse,
            palette,
        )
        canvas.restore()

        GenartStillFx.polish(canvas, size, generation xor engineId.ordinal.toLong() * 0x9E3779B9L)
    }

    /** Generic particle-scatter fallback for engines with no dedicated still baker. */
    internal fun drawFallback(
        canvas: Canvas,
        size: Int,
        generation: Long,
        palette: AnimationPalette,
        engineId: GenartEngineId,
    ) {
        val rnd = Random(generation xor engineId.ordinal.toLong() * 0x9E3779B9L)
        val paint = GenartStillFx.paint()
        paint.shader = RadialGradient(
            size * 0.5f,
            size * 0.45f,
            size * 0.85f,
            intArrayOf(0xFF0B1220.toInt(), 0xFF020617.toInt()),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), paint)
        paint.shader = null

        GenartStillFx.randomScatter(
            canvas = canvas,
            size = size,
            seed = generation + 17L,
            count = 90,
            minRadiusFrac = 0.0007f,
            maxRadiusFrac = 0.0024f,
            alphaRange = 0.2f..0.7f,
            withGlow = true,
        )

        val count = when (engineId) {
            GenartEngineId.Tunnel -> 36
            GenartEngineId.SoftShadows -> 48
            else -> 56
        }
        for (i in 0 until count) {
            val t = (i + rnd.nextFloat()) / count
            val angle = t * 2f * PI.toFloat() * (1.5f + rnd.nextFloat())
            val radius = size * (0.12f + 0.42f * t)
            val x = size * 0.5f + cos(angle) * radius
            val y = size * 0.48f + sin(angle * 0.9f) * radius * 0.85f
            val c = palette.colorAt(i)
            GenartStillFx.softShadowBlur(
                canvas,
                x,
                y,
                6f + rnd.nextFloat() * 14f,
                blurFrac = 0.55f,
                color = Color.BLACK,
                alpha = 0.25f,
            )
            paint.color = Color.argb(
                110 + rnd.nextInt(130),
                Color.red(c),
                Color.green(c),
                Color.blue(c),
            )
            val blobR = size * (0.018f + rnd.nextFloat() * 0.04f)
            GenartStillFx.softGlow(canvas, x, y, blobR * 2.4f, c, 0.35f)
            canvas.drawCircle(x, y, blobR, paint)
        }
    }
}

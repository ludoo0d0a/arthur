package fr.geoking.arthur.genart

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * android.graphics still bakers for Auto album art (no Compose / no animation).
 * [generation] selects palette index and phase/rotation so Ambient Rotation URIs look distinct.
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
        GenartRegistry.descriptorFor(engineId).renderStill(
            canvas,
            size,
            generation,
            phase,
            rotationDeg,
            pulse,
            palette,
        )
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
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
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

        val count = when (engineId) {
            GenartEngineId.Tunnel -> 28
            GenartEngineId.SoftShadows -> 36
            else -> 40
        }
        for (i in 0 until count) {
            val t = (i + rnd.nextFloat()) / count
            val angle = t * 2f * PI.toFloat() * (1.5f + rnd.nextFloat())
            val radius = size * (0.08f + 0.38f * t)
            val x = size * 0.5f + cos(angle) * radius
            val y = size * 0.48f + sin(angle * 0.9f) * radius * 0.85f
            val c = palette.colorAt(i)
            paint.color = Color.argb(
                90 + rnd.nextInt(120),
                Color.red(c),
                Color.green(c),
                Color.blue(c),
            )
            canvas.drawCircle(x, y, 3f + rnd.nextFloat() * 8f, paint)
        }
    }
}

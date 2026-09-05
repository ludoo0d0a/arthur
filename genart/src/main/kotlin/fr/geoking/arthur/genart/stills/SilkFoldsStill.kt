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

/** Bakes one frozen frame of Silk Folds for Auto/Ambient album art. */
internal object SilkFoldsStill {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()

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
            intArrayOf(0xFF0A0810.toInt(), 0xFF030208.toInt()),
            null,
            Shader.TileMode.CLAMP,
        )
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        val time = ((phase / (2f * PI.toFloat())) + rotationDeg / 360f)
        val loop = ((time % 1f) + 1f) % 1f
        val dim = 0.65f + 0.35f * pulse
        val folds = (0 until 7).map { i ->
            FoldSeed(
                baseY = 0.08f + rnd.nextFloat() * 0.82f,
                amplitude = 0.02f + rnd.nextFloat() * 0.05f,
                cycles = 1.0f + rnd.nextFloat() * 1.4f,
                foldPhase = rnd.nextFloat() * 2f * PI.toFloat(),
                speedMul = 0.4f + rnd.nextFloat() * 0.7f,
                thickness = 0.06f + rnd.nextFloat() * 0.08f,
                colorIndex = i,
            )
        }.sortedBy { it.baseY }

        val segments = 36
        paint.style = Paint.Style.FILL
        folds.forEach { fold ->
            val amp = fold.amplitude * h * dim
            val baseY = fold.baseY * h
            val thick = fold.thickness * h
            val freq = fold.cycles * 2f * PI.toFloat() / w.coerceAtLeast(1f)
            val t = loop * 2f * PI.toFloat()
            path.reset()
            for (k in 0..segments) {
                val x = (k / segments.toFloat()) * w
                val y = baseY + amp * sin(x * freq + t * fold.speedMul + fold.foldPhase)
                if (k == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            for (k in segments downTo 0) {
                val x = (k / segments.toFloat()) * w
                val y = baseY + thick + amp * 0.7f * sin(x * freq + t * fold.speedMul + fold.foldPhase + 0.4f)
                path.lineTo(x, y)
            }
            path.close()
            val tint = palette.colorAt(fold.colorIndex)
            paint.color = Color.argb(
                (0.28f * dim * 255).toInt().coerceIn(0, 255),
                Color.red(tint), Color.green(tint), Color.blue(tint),
            )
            canvas.drawPath(path, paint)
        }
        paint.alpha = 255
    }

    private data class FoldSeed(
        val baseY: Float,
        val amplitude: Float,
        val cycles: Float,
        val foldPhase: Float,
        val speedMul: Float,
        val thickness: Float,
        val colorIndex: Int,
    )
}

package fr.geoking.arthur.genart.stills

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import fr.geoking.arthur.genart.AnimationPalette
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Plasma Flow Still Renderer.
 * Renders multi-path energetic fluid streamlines using dynamic Bézier paths,
 * varied stroke widths, neon plasma brushes (gradients & radial glow), and energy spark particles.
 */
internal object PlasmaFlowStill {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)
    private val path = Path()

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
    ) {
        val rnd = Random(generation)
        val w = width.toFloat().coerceAtLeast(1f)
        val h = height.toFloat().coerceAtLeast(1f)
        val cx = w * 0.5f
        val cy = h * 0.5f
        val maxR = maxOf(w, h) * 0.7f
        val dim = 0.75f + 0.25f * pulse
        val t = phase * 0.8f + rotationDeg * 0.01745f

        // 1. Dark cosmic space background with subtle radial gradient
        paint.reset()
        paint.flags = Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG
        paint.style = Paint.Style.FILL
        paint.shader = RadialGradient(
            cx, cy, maxR,
            intArrayOf(0xFF0D0B20.toInt(), 0xFF05030A.toInt(), 0xFF000002.toInt()),
            floatArrayOf(0f, 0.6f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        // 2. Palette colors setup
        val c0 = if (palette.colors.isNotEmpty()) palette.colors[0] else 0xFF00F0FF.toInt() // Neon Cyan
        val c1 = if (palette.colors.size > 1) palette.colors[1] else 0xFFFF007F.toInt() // Magenta Plasma
        val c2 = if (palette.colors.size > 2) palette.colors[2] else 0xFF7B00FF.toInt() // Electric Purple
        val whiteCore = 0xFFF0FBFF.toInt()

        // 3. Central plasma energy core ambient glow
        paint.shader = RadialGradient(
            cx, cy, (maxR * 0.45f).coerceAtLeast(1f),
            intArrayOf(
                Color.argb((0.4f * dim * 255).toInt(), Color.red(c0), Color.green(c0), Color.blue(c0)),
                Color.argb((0.25f * dim * 255).toInt(), Color.red(c1), Color.green(c1), Color.blue(c1)),
                Color.argb((0.1f * dim * 255).toInt(), Color.red(c2), Color.green(c2), Color.blue(c2)),
                Color.TRANSPARENT,
            ),
            floatArrayOf(0f, 0.4f, 0.75f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(cx, cy, maxR * 0.45f, paint)
        paint.shader = null

        // 4. Generate multi-path plasma streamlines (varying path, stroke, brush)
        val streamlineCount = 28
        for (i in 0 until streamlineCount) {
            val seedOffset = i * 137L
            val pRnd = Random(generation + seedOffset)

            // Dynamic path parameters
            val pathType = i % 3 // 0: S-curve flow, 1: Loop/Spiral twist, 2: Radial wave
            val strokeW = (2.5f + pRnd.nextFloat() * 12f) * (w / 800f)
            val speedFactor = 0.5f + pRnd.nextFloat() * 0.8f
            val localTime = t * speedFactor + pRnd.nextFloat() * 10f

            // Color selection & brush shader
            val primaryColor = when (i % 3) {
                0 -> c0
                1 -> c1
                else -> c2
            }
            val secondaryColor = when ((i + 1) % 3) {
                0 -> c0
                1 -> c1
                else -> c2
            }

            path.reset()

            val startX: Float
            val startY: Float
            val ctrl1X: Float
            val ctrl1Y: Float
            val ctrl2X: Float
            val ctrl2Y: Float
            val endX: Float
            val endY: Float

            when (pathType) {
                0 -> { // S-curve horizontal plasma drift
                    val yNorm = (i.toFloat() / streamlineCount) - 0.5f
                    val yBase = cy + yNorm * h * 0.85f
                    startX = -w * 0.1f
                    startY = yBase + sin(localTime + i) * h * 0.08f

                    ctrl1X = w * 0.3f + cos(localTime * 0.7f + i) * w * 0.15f
                    ctrl1Y = yBase + sin(localTime * 1.2f + i) * h * 0.25f

                    ctrl2X = w * 0.7f + sin(localTime * 0.9f - i) * w * 0.15f
                    ctrl2Y = yBase - cos(localTime * 1.1f + i) * h * 0.25f

                    endX = w * 1.1f
                    endY = yBase + cos(localTime + i * 0.5f) * h * 0.08f
                }
                1 -> { // Vortex/Twist curve through center
                    val angleStart = (i.toFloat() / streamlineCount) * 2f * PI.toFloat() + localTime * 0.2f
                    val rStart = maxR * (0.8f + 0.2f * sin(localTime + i))
                    startX = cx + cos(angleStart) * rStart
                    startY = cy + sin(angleStart) * rStart

                    ctrl1X = cx + cos(angleStart + 1.2f) * (rStart * 0.5f)
                    ctrl1Y = cy + sin(angleStart + 1.2f) * (rStart * 0.5f)

                    ctrl2X = cx + cos(angleStart + 2.5f) * (rStart * 0.3f)
                    ctrl2Y = cy + sin(angleStart + 2.5f) * (rStart * 0.3f)

                    val angleEnd = angleStart + 3.8f
                    val rEnd = maxR * 0.9f
                    endX = cx + cos(angleEnd) * rEnd
                    endY = cy + sin(angleEnd) * rEnd
                }
                else -> { // Diagonal plasma wave strand
                    val startOffset = (pRnd.nextFloat() - 0.5f) * w * 0.8f
                    startX = cx + startOffset - w * 0.4f
                    startY = -h * 0.1f

                    ctrl1X = cx + startOffset + sin(localTime + i) * w * 0.3f
                    ctrl1Y = h * 0.35f

                    ctrl2X = cx + startOffset - cos(localTime * 0.8f + i) * w * 0.3f
                    ctrl2Y = h * 0.65f

                    endX = cx + startOffset + w * 0.4f
                    endY = h * 1.1f
                }
            }

            path.moveTo(startX, startY)
            path.cubicTo(ctrl1X, ctrl1Y, ctrl2X, ctrl2Y, endX, endY)

            // Outer soft glow stroke
            paint.reset()
            paint.flags = Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG
            paint.style = Paint.Style.STROKE
            paint.strokeCap = Paint.Cap.ROUND
            paint.strokeWidth = strokeW * 2.8f
            paint.shader = LinearGradient(
                startX, startY, endX, endY,
                intArrayOf(
                    Color.TRANSPARENT,
                    Color.argb((0.35f * dim * 255).toInt(), Color.red(primaryColor), Color.green(primaryColor), Color.blue(primaryColor)),
                    Color.argb((0.45f * dim * 255).toInt(), Color.red(secondaryColor), Color.green(secondaryColor), Color.blue(secondaryColor)),
                    Color.TRANSPARENT,
                ),
                floatArrayOf(0f, 0.3f, 0.7f, 1f),
                Shader.TileMode.CLAMP
            )
            canvas.drawPath(path, paint)

            // Inner high-intensity core stroke
            paint.strokeWidth = strokeW * 0.8f
            paint.shader = LinearGradient(
                startX, startY, endX, endY,
                intArrayOf(
                    Color.TRANSPARENT,
                    Color.argb((0.85f * dim * 255).toInt(), Color.red(whiteCore), Color.green(whiteCore), Color.blue(whiteCore)),
                    Color.argb((0.9f * dim * 255).toInt(), Color.red(primaryColor), Color.green(primaryColor), Color.blue(primaryColor)),
                    Color.TRANSPARENT,
                ),
                floatArrayOf(0f, 0.4f, 0.8f, 1f),
                Shader.TileMode.CLAMP
            )
            canvas.drawPath(path, paint)
            paint.shader = null
        }

        // 5. Flowing plasma sparks & energy droplets along pathways
        val sparkCount = 60
        paint.style = Paint.Style.FILL
        for (j in 0 until sparkCount) {
            val sparkRnd = Random(generation + j * 999L)
            val progress = ((t * (0.2f + sparkRnd.nextFloat() * 0.5f) + sparkRnd.nextFloat()) % 1f)
            val angle = sparkRnd.nextFloat() * 2f * PI.toFloat()
            val dist = progress * maxR * 0.95f

            // Spiral perturbation
            val sparkX = cx + cos(angle + dist * 0.005f) * dist + (sparkRnd.nextFloat() - 0.5f) * w * 0.1f
            val sparkY = cy + sin(angle + dist * 0.005f) * dist + (sparkRnd.nextFloat() - 0.5f) * h * 0.1f
            val sparkRadius = (1.5f + sparkRnd.nextFloat() * 4.5f) * (w / 800f)

            val colorChoice = when (j % 4) {
                0 -> c0
                1 -> c1
                2 -> c2
                else -> whiteCore
            }

            paint.color = Color.argb(
                ((0.4f + sparkRnd.nextFloat() * 0.5f) * dim * 255).toInt().coerceIn(0, 255),
                Color.red(colorChoice), Color.green(colorChoice), Color.blue(colorChoice)
            )
            canvas.drawCircle(sparkX, sparkY, sparkRadius, paint)
        }
    }
}

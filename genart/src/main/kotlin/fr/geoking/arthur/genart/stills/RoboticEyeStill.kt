package fr.geoking.arthur.genart.stills

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import fr.geoking.arthur.genart.AnimationPalette
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Robotic Eye Still Renderer (Terminator / M3GAN 2.0 style).
 * Features a synthetic cybernetic eye:
 * - Human-eye almond sclera/aperture frame with metallic bezel shading
 * - Overlapping mechanical iris aperture blades
 * - Vibrating, micro-jittering pupil core simulating organic ocular micro-saccades & focus
 * - Dynamic HUD tech overlays (telemetry arcs, reticles, crosshairs, digital readouts)
 * - Seed-based random variations (Color Theme: Terminator Red, M3GAN Cyan, Neon Amber; Blade counts; HUD styles)
 */
internal object RoboticEyeStill {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)
    private val path = Path()
    private val rectF = RectF()

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
        val minDim = minOf(w, h)
        val eyeR = minDim * 0.38f
        val t = phase + rotationDeg * 0.01745f

        // Random Variations based on seed/generation
        val themeType = rnd.nextInt(3) // 0: Terminator Crimson, 1: M3GAN Cyan/Sapphire, 2: Cyber Gold/Amber
        val bladeCount = 6 + rnd.nextInt(4) * 2 // 6, 8, 10, or 12 mechanical iris blades
        val hudStyle = rnd.nextInt(3) // 0: Tactical Crosshair, 1: Concentric Telemetry, 2: Hex/Octagonal Reticle

        // Color Palette selection with fallback to theme defaults
        val (coreColor, hudColor, accentColor, bgDark) = when (themeType) {
            0 -> Tuple4(0xFFFF0033.toInt(), 0xFFFF2244.toInt(), 0xFFFF6600.toInt(), 0xFF080103.toInt()) // Terminator Red
            1 -> Tuple4(0xFF00F0FF.toInt(), 0xFF00A2FF.toInt(), 0xFF7000FF.toInt(), 0xFF01080F.toInt()) // M3GAN Cyan
            else -> Tuple4(0xFFFFD700.toInt(), 0xFFFF8C00.toInt(), 0xFFFF4500.toInt(), 0xFF0A0701.toInt()) // Cyber Amber
        }

        val primaryGlow = if (palette.colors.isNotEmpty()) palette.colors[0] else coreColor
        val secondaryGlow = if (palette.colors.size > 1) palette.colors[1] else hudColor

        // 1. Cybernetic Plate / Deep Background
        paint.reset()
        paint.flags = Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG
        paint.style = Paint.Style.FILL
        paint.shader = RadialGradient(
            cx, cy, maxOf(w, h) * 0.75f,
            intArrayOf(0xFF14171F.toInt(), bgDark, 0xFF000000.toInt()),
            floatArrayOf(0f, 0.65f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        // Background Tech Grid / Circuit traces around eye socket
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f * (w / 800f)
        paint.color = Color.argb(40, Color.red(secondaryGlow), Color.green(secondaryGlow), Color.blue(secondaryGlow))
        val gridStep = minDim * 0.1f
        var gx = 0f
        while (gx < w) {
            canvas.drawLine(gx, 0f, gx, h, paint)
            gx += gridStep
        }
        var gy = 0f
        while (gy < h) {
            canvas.drawLine(0f, gy, w, gy, paint)
            gy += gridStep
        }

        // 2. Human Eye Contour (Almond/Aperture Eyelid Sclera Outline)
        val eyeWidth = eyeR * 1.95f
        val eyeHeight = eyeR * 1.15f

        path.reset()
        path.moveTo(cx - eyeWidth, cy)
        path.cubicTo(
            cx - eyeWidth * 0.5f, cy - eyeHeight * 1.3f,
            cx + eyeWidth * 0.5f, cy - eyeHeight * 1.3f,
            cx + eyeWidth, cy
        )
        path.cubicTo(
            cx + eyeWidth * 0.5f, cy + eyeHeight * 1.3f,
            cx - eyeWidth * 0.5f, cy + eyeHeight * 1.3f,
            cx - eyeWidth, cy
        )
        path.close()

        // Sclera dark metallic inner shadow fill
        paint.style = Paint.Style.FILL
        paint.shader = RadialGradient(
            cx, cy, eyeWidth,
            intArrayOf(0xFF0F121A.toInt(), 0xFF05070A.toInt(), Color.BLACK),
            floatArrayOf(0f, 0.7f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawPath(path, paint)
        paint.shader = null

        // Metallic eyelid bezel stroke
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 4f * (w / 800f)
        paint.shader = LinearGradient(
            cx - eyeWidth, cy, cx + eyeWidth, cy,
            intArrayOf(0xFF4A5568.toInt(), 0xFFA0AEC0.toInt(), 0xFF2D3748.toInt(), 0xFF718096.toInt()),
            null,
            Shader.TileMode.CLAMP
        )
        canvas.drawPath(path, paint)
        paint.shader = null

        // 3. Iris Mechanical Aperture Blades
        val irisR = eyeR * 0.72f
        val bladeAngle = 360f / bladeCount
        val irisRotation = t * 15f

        canvas.save()
        canvas.rotate(irisRotation, cx, cy)

        for (i in 0 until bladeCount) {
            val aRad = Math.toRadians((i * bladeAngle).toDouble()).toFloat()
            val nextARad = Math.toRadians(((i + 1) * bladeAngle).toDouble()).toFloat()

            path.reset()
            val rOuter = irisR
            val rInner = irisR * 0.35f

            val bx1 = cx + cos(aRad) * rOuter
            val by1 = cy + sin(aRad) * rOuter
            val bx2 = cx + cos(nextARad) * rOuter
            val by2 = cy + sin(nextARad) * rOuter
            val bx3 = cx + cos(nextARad + 0.2f) * rInner
            val by3 = cy + sin(nextARad + 0.2f) * rInner
            val bx4 = cx + cos(aRad + 0.1f) * rInner
            val by4 = cy + sin(aRad + 0.1f) * rInner

            path.moveTo(bx1, by1)
            path.lineTo(bx2, by2)
            path.lineTo(bx3, by3)
            path.lineTo(bx4, by4)
            path.close()

            // Blade metallic gradient
            paint.style = Paint.Style.FILL
            paint.shader = LinearGradient(
                bx1, by1, bx3, by3,
                intArrayOf(0xFF2A2E3D.toInt(), 0xFF12151E.toInt(), 0xFF4A5166.toInt()),
                floatArrayOf(0f, 0.6f, 1f),
                Shader.TileMode.CLAMP
            )
            canvas.drawPath(path, paint)

            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 1.2f * (w / 800f)
            paint.color = Color.argb(120, 160, 175, 200)
            canvas.drawPath(path, paint)
        }
        canvas.restore()

        // 4. Vibrating Pupil Core (Organically reactive with micro-jitter micro-saccades)
        // Pupil vibration calculation (combines pulse, sine time, and high-frequency micro-jitter)
        val jitterX = sin(t * 43f) * (minDim * 0.008f) * (0.5f + rnd.nextFloat() * 0.5f)
        val jitterY = cos(t * 37f) * (minDim * 0.008f) * (0.5f + rnd.nextFloat() * 0.5f)
        val pupilCx = cx + jitterX
        val pupilCy = cy + jitterY

        val basePupilR = irisR * 0.42f
        val pupilDilate = basePupilR * (0.85f + 0.25f * sin(t * 2.5f) + 0.15f * pulse)

        // Outer Iris Glow Halo
        paint.style = Paint.Style.FILL
        paint.shader = RadialGradient(
            pupilCx, pupilCy, (pupilDilate * 2.2f).coerceAtLeast(1f),
            intArrayOf(
                Color.argb(220, Color.red(primaryGlow), Color.green(primaryGlow), Color.blue(primaryGlow)),
                Color.argb(120, Color.red(secondaryGlow), Color.green(secondaryGlow), Color.blue(secondaryGlow)),
                Color.TRANSPARENT
            ),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(pupilCx, pupilCy, pupilDilate * 2.2f, paint)

        // Concentric Optical Sensor Rings
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f * (w / 800f)
        paint.color = Color.argb(200, Color.red(primaryGlow), Color.green(primaryGlow), Color.blue(primaryGlow))
        canvas.drawCircle(pupilCx, pupilCy, pupilDilate * 1.35f, paint)
        canvas.drawCircle(pupilCx, pupilCy, pupilDilate * 1.7f, paint)

        // Hot Core Pupil Lens
        paint.style = Paint.Style.FILL
        paint.shader = RadialGradient(
            pupilCx, pupilCy, pupilDilate.coerceAtLeast(1f),
            intArrayOf(
                0xFFFFFFFF.toInt(),
                Color.argb(255, Color.red(primaryGlow), Color.green(primaryGlow), Color.blue(primaryGlow)),
                0xFF000000.toInt()
            ),
            floatArrayOf(0f, 0.45f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(pupilCx, pupilCy, pupilDilate, paint)
        paint.shader = null

        // High-tech lens highlight reflection specular spot
        paint.style = Paint.Style.FILL
        paint.color = Color.argb(220, 255, 255, 255)
        canvas.drawCircle(pupilCx - pupilDilate * 0.35f, pupilCy - pupilDilate * 0.35f, pupilDilate * 0.22f, paint)

        // 5. Tech Animation HUD Overlay (Targeting reticles, telemetry arcs, readouts)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1.8f * (w / 800f)
        paint.color = Color.argb(180, Color.red(hudColor), Color.green(hudColor), Color.blue(hudColor))

        val hudR = eyeR * 1.25f

        // Rotating outer HUD arc segments
        val hudRot = t * 25f
        rectF.set(cx - hudR, cy - hudR, cx + hudR, cy + hudR)
        canvas.drawArc(rectF, hudRot, 60f, false, paint)
        canvas.drawArc(rectF, hudRot + 120f, 80f, false, paint)
        canvas.drawArc(rectF, hudRot + 260f, 40f, false, paint)

        // HUD Reticle Crosshair elements
        when (hudStyle) {
            0 -> { // Tactical Crosshair with corner brackets
                val bracketL = hudR * 0.25f
                val offset = hudR * 0.95f
                // Top-Left
                canvas.drawLine(cx - offset, cy - offset, cx - offset + bracketL, cy - offset, paint)
                canvas.drawLine(cx - offset, cy - offset, cx - offset, cy - offset + bracketL, paint)
                // Top-Right
                canvas.drawLine(cx + offset, cy - offset, cx + offset - bracketL, cy - offset, paint)
                canvas.drawLine(cx + offset, cy - offset, cx + offset, cy - offset + bracketL, paint)
                // Bottom-Left
                canvas.drawLine(cx - offset, cy + offset, cx - offset + bracketL, cy + offset, paint)
                canvas.drawLine(cx - offset, cy + offset, cx - offset, cy + offset - bracketL, paint)
                // Bottom-Right
                canvas.drawLine(cx + offset, cy + offset, cx + offset - bracketL, cy + offset, paint)
                canvas.drawLine(cx + offset, cy + offset, cx + offset, cy + offset - bracketL, paint)
            }
            1 -> { // Concentric Telemetry Pitch Ticks
                val tickR = hudR * 1.15f
                for (angle in 0 until 360 step 30) {
                    val rad = Math.toRadians(angle.toDouble()).toFloat()
                    val x1 = cx + cos(rad) * tickR
                    val y1 = cy + sin(rad) * tickR
                    val x2 = cx + cos(rad) * (tickR + minDim * 0.025f)
                    val y2 = cy + sin(rad) * (tickR + minDim * 0.025f)
                    canvas.drawLine(x1, y1, x2, y2, paint)
                }
            }
            else -> { // Octagonal Framing Reticle
                path.reset()
                val octR = hudR * 1.08f
                for (i in 0 until 8) {
                    val angle = i * 45f + 22.5f
                    val rad = Math.toRadians(angle.toDouble()).toFloat()
                    val px = cx + cos(rad) * octR
                    val py = cy + sin(rad) * octR
                    if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
                }
                path.close()
                canvas.drawPath(path, paint)
            }
        }

        // Digital telemetry text overlays
        paint.style = Paint.Style.FILL
        paint.textSize = minDim * 0.028f
        paint.color = Color.argb(160, Color.red(hudColor), Color.green(hudColor), Color.blue(hudColor))
        canvas.drawText("OPTIC_V2.0", cx - hudR * 0.85f, cy - hudR * 0.92f, paint)
        canvas.drawText("SYS: ONLINE", cx + hudR * 0.35f, cy - hudR * 0.92f, paint)
        val scanPct = ((t * 15f) % 100).toInt()
        canvas.drawText("TARGET_LOCK: $scanPct%", cx - hudR * 0.85f, cy + hudR * 1.05f, paint)
    }

    private data class Tuple4<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)
}

package fr.geoking.arthur.genart.stills

import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import fr.geoking.arthur.genart.AnimationPalette
import fr.geoking.arthur.genart.GenartQuality
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

/**
 * Bakes one frame of the Milky Way galaxy renderer (inspired by GPU point-sprite / dust ray-marching).
 *
 * Renders logarithmic spiral arms, stellar spectrum point sprites, glowing central bulge,
 * ray-marched / layered interstellar dark dust lanes, Gaia disk S-warp, glowing emission nebulae,
 * and the Sun location marker at 8.2 kpc on the Local Spur.
 */
internal object MilkyWayStill {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)
    private val path = Path()

    // Stellar spectral classification colors (O, B, A, F, G, K, M)
    private val SPECTRAL_COLORS = intArrayOf(
        Color.rgb(155, 176, 255), // Class O - Deep Blue
        Color.rgb(170, 191, 255), // Class B - Blue-White
        Color.rgb(202, 215, 255), // Class A - Bright White
        Color.rgb(248, 247, 255), // Class F - Yellow-White
        Color.rgb(255, 244, 234), // Class G - Sun-like Yellow
        Color.rgb(255, 210, 161), // Class K - Light Orange
        Color.rgb(255, 115, 80),  // Class M - Cool Red Dwarf
    )

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
        quality: GenartQuality = GenartQuality.High,
        isActive: Boolean = true,
    ) {
        val rnd = Random(generation)
        val w = width.toFloat().coerceAtLeast(1f)
        val h = height.toFloat().coerceAtLeast(1f)
        val minDim = minOf(w, h)
        val cx = w / 2f
        val cy = h / 2f

        // Seeded randomized parameters
        val inclinationDeg = 35f + rnd.nextFloat() * 25f // Perspective tilt
        val armCount = 4
        val spiralB = 0.22f + rnd.nextFloat() * 0.08f   // Tightness of logarithmic spiral arms
        val warpAmp = 0.04f + rnd.nextFloat() * 0.04f     // Gaia S-warp disk distortion
        val dustNoiseSeed = rnd.nextFloat() * 100f

        val inclinationRad = Math.toRadians(inclinationDeg.toDouble()).toFloat()
        val cosInc = cos(inclinationRad)
        val sinInc = sin(inclinationRad)

        // Palette tints
        val palettePrimary = palette.primary
        val paletteSecondary = palette.secondary
        val paletteAccent = palette.colorAt(2)

        // Animation timing
        val speedFactor = if (isActive) 1f else 0.5f
        val galRotRad = (phase * 0.08f * speedFactor) + Math.toRadians(rotationDeg.toDouble()).toFloat()
        val pulseFactor = 0.92f + 0.08f * pulse
        val breathe = 1f + 0.05f * sin(phase * 0.5f)

        // 1. Cosmic Background
        paint.reset()
        paint.flags = Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG
        paint.style = Paint.Style.FILL
        paint.color = Color.rgb(3, 2, 10)
        canvas.drawRect(0f, 0f, w, h, paint)

        // Deep galactic halo glow
        val haloRadius = minDim * 0.48f * breathe
        paint.shader = RadialGradient(
            cx, cy, haloRadius.coerceAtLeast(1f),
            intArrayOf(
                Color.argb(60, Color.red(palettePrimary), Color.green(palettePrimary), Color.blue(palettePrimary)),
                Color.argb(30, Color.red(paletteSecondary), Color.green(paletteSecondary), Color.blue(paletteSecondary)),
                Color.TRANSPARENT,
            ),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(cx, cy, haloRadius, paint)
        paint.shader = null

        // Distant background starfield
        val bgStarCount = when (quality) {
            GenartQuality.Low -> 80
            GenartQuality.Medium -> 150
            GenartQuality.High -> 250
        }
        val bgRnd = Random(generation xor 0x5A5A5A5AL)
        for (i in 0 until bgStarCount) {
            val bx = bgRnd.nextFloat() * w
            val by = bgRnd.nextFloat() * h
            val bSize = 0.5f + bgRnd.nextFloat() * 1.2f
            val twinkle = (sin(phase * 2f + bgRnd.nextFloat() * 10f) * 0.3f + 0.7f).coerceIn(0.2f, 1f)
            val bAlpha = (bgRnd.nextFloat() * 180 * twinkle).toInt().coerceIn(10, 255)
            val bColor = SPECTRAL_COLORS[bgRnd.nextInt(SPECTRAL_COLORS.size)]
            paint.color = Color.argb(bAlpha, Color.red(bColor), Color.green(bColor), Color.blue(bColor))
            canvas.drawCircle(bx, by, bSize, paint)
        }

        // 2. Emission Nebulae along Spiral Arms
        val nebulaCount = when (quality) {
            GenartQuality.Low -> 8
            GenartQuality.Medium -> 16
            GenartQuality.High -> 24
        }
        val maxGalaxyRadius = minDim * 0.42f
        paint.maskFilter = BlurMaskFilter(minDim * 0.05f, BlurMaskFilter.Blur.NORMAL)
        for (n in 0 until nebulaCount) {
            val armIdx = n % armCount
            val armThetaOffset = armIdx * (2f * PI.toFloat() / armCount)
            val tNorm = 0.2f + (n / nebulaCount.toFloat()) * 0.75f
            val theta = tNorm * 3.5f * PI.toFloat()
            val angle = theta + armThetaOffset + galRotRad
            val rNorm = exp(spiralB * theta) / exp(spiralB * 3.5f * PI.toFloat())
            val r = rNorm * maxGalaxyRadius

            // Galactic plane 3D coordinates
            val rx = r * cos(angle)
            val ry = r * sin(angle)
            val rz = sin(theta * 2f) * r * warpAmp // Gaia S-warp

            // Project 3D -> 2D
            val px = cx + rx
            val py = cy + ry * cosInc - rz * sinInc
            val nebRadius = (0.04f + rNorm * 0.08f) * minDim

            val nebColor = if (n % 2 == 0) palettePrimary else paletteAccent
            paint.shader = RadialGradient(
                px, py, nebRadius.coerceAtLeast(1f),
                intArrayOf(
                    Color.argb(45, Color.red(nebColor), Color.green(nebColor), Color.blue(nebColor)),
                    Color.argb(15, Color.red(paletteSecondary), Color.green(paletteSecondary), Color.blue(paletteSecondary)),
                    Color.TRANSPARENT,
                ),
                floatArrayOf(0f, 0.6f, 1f),
                Shader.TileMode.CLAMP,
            )
            canvas.drawCircle(px, py, nebRadius, paint)
            paint.shader = null
        }
        paint.maskFilter = null

        // 3. Central Galactic Bulge & Core Nucleus
        val coreRadius = minDim * 0.16f * pulseFactor
        paint.maskFilter = BlurMaskFilter(minDim * 0.03f, BlurMaskFilter.Blur.NORMAL)
        paint.shader = RadialGradient(
            cx, cy, coreRadius.coerceAtLeast(1f),
            intArrayOf(
                Color.argb(255, 255, 250, 235),
                Color.argb(220, 255, 220, 160),
                Color.argb(100, Color.red(paletteSecondary), Color.green(paletteSecondary), Color.blue(paletteSecondary)),
                Color.TRANSPARENT,
            ),
            floatArrayOf(0f, 0.25f, 0.65f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(cx, cy, coreRadius, paint)
        paint.shader = null
        paint.maskFilter = null

        // 4. Star Particles (Point Sprites distributed along Spiral Arms & Bulge)
        val starCount = when (quality) {
            GenartQuality.Low -> 600
            GenartQuality.Medium -> 1400
            GenartQuality.High -> 2600
        }
        val starRnd = Random(generation xor 0x12345678L)

        for (s in 0 until starCount) {
            val isBulgeStar = s < (starCount * 0.25f)
            val armIdx = s % armCount
            val armThetaOffset = armIdx * (2f * PI.toFloat() / armCount)

            val theta: Float
            val r: Float
            val angle: Float

            if (isBulgeStar) {
                val u = starRnd.nextFloat()
                r = u * u * (maxGalaxyRadius * 0.28f)
                angle = starRnd.nextFloat() * 2f * PI.toFloat() + galRotRad
                theta = angle
            } else {
                val u = starRnd.nextFloat()
                theta = u * 3.8f * PI.toFloat()
                val rNorm = exp(spiralB * theta) / exp(spiralB * 3.8f * PI.toFloat())
                val radialJitter = (starRnd.nextFloat() - 0.5f) * 0.12f * maxGalaxyRadius
                val angleJitter = (starRnd.nextFloat() - 0.5f) * 0.25f
                r = (rNorm * maxGalaxyRadius + radialJitter).coerceAtLeast(0f)
                angle = theta + armThetaOffset + angleJitter + galRotRad
            }

            // Gaia S-warp along z axis
            val warp = sin(angle * 1.5f) * r * warpAmp
            val rx = r * cos(angle)
            val ry = r * sin(angle)
            val rz = (starRnd.nextFloat() - 0.5f) * 0.03f * maxGalaxyRadius + warp

            // Projection
            val px = cx + rx
            val py = cy + ry * cosInc - rz * sinInc

            // Spectral classification color distribution
            val specIdx = when {
                starRnd.nextFloat() < 0.08f -> 0 // Class O (rare blue)
                starRnd.nextFloat() < 0.22f -> 1 // Class B
                starRnd.nextFloat() < 0.40f -> 2 // Class A
                starRnd.nextFloat() < 0.60f -> 3 // Class F
                starRnd.nextFloat() < 0.80f -> 4 // Class G
                starRnd.nextFloat() < 0.92f -> 5 // Class K
                else -> 6                         // Class M (red dwarf)
            }
            val specColor = SPECTRAL_COLORS[specIdx]

            // Size and alpha
            val starBaseSize = if (specIdx <= 1) 1.8f + starRnd.nextFloat() * 1.5f else 0.8f + starRnd.nextFloat() * 1.2f
            val alphaMod = (0.5f + 0.5f * sin(phase * 3f + s * 1.1f)).coerceIn(0.3f, 1f)
            val alphaInt = ((0.4f + starRnd.nextFloat() * 0.6f) * alphaMod * 255).toInt().coerceIn(30, 255)

            paint.color = Color.argb(alphaInt, Color.red(specColor), Color.green(specColor), Color.blue(specColor))
            canvas.drawCircle(px, py, starBaseSize, paint)

            // Bright star flare/glow for top-tier blue/white stars
            if (specIdx <= 1 && starRnd.nextFloat() < 0.15f) {
                paint.color = Color.argb(alphaInt / 3, Color.red(specColor), Color.green(specColor), Color.blue(specColor))
                canvas.drawCircle(px, py, starBaseSize * 2.5f, paint)
            }
        }

        // 5. Interstellar Dark Dust Lanes (Great Rift Absorption Ray-Marching)
        val dustStepCount = when (quality) {
            GenartQuality.Low -> 12
            GenartQuality.Medium -> 20
            GenartQuality.High -> 32
        }
        paint.style = Paint.Style.FILL

        for (d in 0 until dustStepCount) {
            val dArmOffset = (d % armCount) * (2f * PI.toFloat() / armCount)
            val dTheta = 0.4f + (d / dustStepCount.toFloat()) * 3.0f * PI.toFloat()
            val dNorm = exp(spiralB * dTheta) / exp(spiralB * 3.0f * PI.toFloat())
            val dRadius = dNorm * maxGalaxyRadius * 0.85f

            val dAngle = dTheta + dArmOffset + galRotRad + 0.18f // Offset slightly ahead of stellar arm
            val dx = cx + dRadius * cos(dAngle)
            val dy = cy + dRadius * sin(dAngle) * cosInc

            val dSize = (0.05f + dNorm * 0.09f) * minDim
            val noiseOffset = sin(dTheta * 3f + dustNoiseSeed + phase * 0.1f) * 0.02f * minDim

            val dustAlpha = (80 + (100 * (1f - dNorm)).toInt()).coerceIn(40, 160)
            paint.shader = RadialGradient(
                dx + noiseOffset, dy, dSize.coerceAtLeast(1f),
                intArrayOf(
                    Color.argb(dustAlpha, 8, 5, 12),
                    Color.argb((dustAlpha * 0.6f).toInt(), 15, 10, 22),
                    Color.TRANSPARENT,
                ),
                floatArrayOf(0f, 0.6f, 1f),
                Shader.TileMode.CLAMP,
            )
            canvas.drawCircle(dx + noiseOffset, dy, dSize, paint)
            paint.shader = null
        }

        // 6. Solar Marker (⊙) at 8.2 kpc on the Local Spur
        val sunDistanceNorm = 0.42f // ~8.2 kpc relative to disk radius
        val sunRadius = maxGalaxyRadius * sunDistanceNorm
        val sunAngle = galRotRad + 1.25f // Local Spur angle
        val sunX = cx + sunRadius * cos(sunAngle)
        val sunY = cy + sunRadius * sin(sunAngle) * cosInc

        // Solar location glow ring
        paint.reset()
        paint.flags = Paint.ANTI_ALIAS_FLAG
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1.5f
        paint.color = Color.argb(200, 255, 230, 120)
        canvas.drawCircle(sunX, sunY, minDim * 0.018f, paint)

        // Solar dot (⊙)
        paint.style = Paint.Style.FILL
        paint.color = Color.rgb(255, 240, 150)
        canvas.drawCircle(sunX, sunY, minDim * 0.006f, paint)

        // Faint Local Spur label crosshair
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        paint.color = Color.argb(120, 255, 240, 150)
        val cs = minDim * 0.01f
        canvas.drawLine(sunX - cs, sunY, sunX + cs, sunY, paint)
        canvas.drawLine(sunX, sunY - cs, sunX, sunY + cs, paint)

        // Faint Habitable Zone Annulus (6.5 - 9.8 kpc)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        paint.color = Color.argb(35, 100, 220, 255)
        val hzRadiusInner = maxGalaxyRadius * 0.33f
        val hzRadiusOuter = maxGalaxyRadius * 0.50f

        path.reset()
        path.addOval(
            cx - hzRadiusInner, cy - hzRadiusInner * cosInc,
            cx + hzRadiusInner, cy + hzRadiusInner * cosInc,
            Path.Direction.CW,
        )
        canvas.drawPath(path, paint)

        path.reset()
        path.addOval(
            cx - hzRadiusOuter, cy - hzRadiusOuter * cosInc,
            cx + hzRadiusOuter, cy + hzRadiusOuter * cosInc,
            Path.Direction.CW,
        )
        canvas.drawPath(path, paint)
    }
}

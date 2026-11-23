package fr.geoking.arthur.genart.engines

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import fr.geoking.arthur.genart.GenartQuality
import fr.geoking.arthur.genart.TonalPalette
import fr.geoking.arthur.genart.lerp
import fr.geoking.arthur.genart.loopedFbm
import fr.geoking.arthur.genart.phase01
import fr.geoking.arthur.genart.positiveRadius
import fr.geoking.arthur.genart.qualityCount
import fr.geoking.arthur.genart.seededRange
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

private data class HearthSpark(
    val xOffsetFrac: Float,
    val riseSpeed: Float,
    val phaseOffset: Float,
    val swayAmp: Float,
    val swayFreq: Float,
    val baseRadius: Float,
    val colorIndex: Int,
)

/**
 * Soft Ambient Hearth Glow — seamless loop via loopedFbm.
 *
 * Deep rustic darkness with a warm glowing hearth/fireplace cavity at the base,
 * softly pulsing ember beds with shifting heat gradients, subtle upward drifting
 * embers/sparks, and warm firelight wash on nearby floor/hearth bricks.
 */
@Composable
internal fun HearthGlowEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val count = qualityCount(quality, low = 4, medium = 7, high = 11)
    val sparks = remember(count) {
        List(count) { i ->
            HearthSpark(
                xOffsetFrac = seededRange(i * 17 + 3, -0.22f, 0.22f),
                riseSpeed = seededRange(i * 29 + 7, 0.7f, 1.6f),
                phaseOffset = seededRange(i * 41 + 11, 0f, 1f),
                swayAmp = seededRange(i * 53 + 13, 0.015f, 0.045f),
                swayFreq = seededRange(i * 67 + 19, 1.0f, 3.0f),
                baseRadius = seededRange(i * 79 + 23, 0.0028f, 0.0062f),
                colorIndex = i,
            )
        }
    }
    val transition = rememberInfiniteTransition(label = "hearthglow")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((72000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "hearthglow_t",
    )
    val dim = if (isActive) 1f else 0.55f

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val minDim = minOf(w, h)
        val cx = w * 0.5f

        val hearthBaseY = h * 0.82f
        val hearthTopY = h * 0.44f
        val hearthWidth = minDim * 0.74f
        val hearthHeight = hearthBaseY - hearthTopY
        val emberY = hearthBaseY - hearthHeight * 0.12f

        val time = phase01(t)
        val wash = loopedFbm(time, radius = 1.35f, octaves = 2, seedOffset = 11)
        val warp1 = loopedFbm(time, radius = 1.25f, octaves = 3, seedOffset = 43)
        val heatPulse = 0.88f + 0.12f * warp1
        val warpX = (loopedFbm(time, radius = 1.35f, octaves = 3, seedOffset = 71) - 0.5f) * minDim * 0.08f
        val warpY = (loopedFbm(time, radius = 1.55f, octaves = 2, seedOffset = 113) - 0.5f) * minDim * 0.025f

        // Themed rustic palette
        val rusticDark = TonalPalette.brightness(Color(0xFF1A0E0A), brightness * dim * (0.92f + 0.08f * wash))
        val hearthBrick = TonalPalette.brightness(Color(0xFF3A1810), brightness * dim)
        val accent = TonalPalette.brightness(
            TonalPalette.mix(Color(0xFFFF7A30), TonalPalette.pick(paletteColors, 0), 0.35f),
            brightness * dim,
        )
        val hotEmber = TonalPalette.brightness(Color(0xFFFF5510), brightness * dim)
        val coreGold = TonalPalette.brightness(Color(0xFFFFC040), brightness * dim)
        val whiteHot = TonalPalette.brightness(Color(0xFFFFE8A0), brightness * dim)
        val deepChar = Color(0xFF0C0503)

        // 1. Deep rustic room background
        drawRect(
            brush = Brush.verticalGradient(
                0f to Color(0xFF0A0403),
                0.45f to rusticDark,
                0.80f to hearthBrick,
                1.0f to Color(0xFF140A06),
            ),
        )

        // 2. Fireplace cavity arch path
        val cavityLeft = cx - hearthWidth * 0.5f
        val cavityRight = cx + hearthWidth * 0.5f
        val cavityShoulder = hearthTopY + hearthHeight * 0.32f
        val archPath = Path().apply {
            moveTo(cavityLeft, hearthBaseY)
            lineTo(cavityLeft, cavityShoulder)
            cubicTo(
                cavityLeft, hearthTopY + hearthHeight * 0.06f,
                cx - hearthWidth * 0.22f, hearthTopY,
                cx, hearthTopY,
            )
            cubicTo(
                cx + hearthWidth * 0.22f, hearthTopY,
                cavityRight, hearthTopY + hearthHeight * 0.06f,
                cavityRight, cavityShoulder,
            )
            lineTo(cavityRight, hearthBaseY)
            close()
        }

        // Fireplace interior darkness
        drawPath(
            path = archPath,
            brush = Brush.verticalGradient(
                0f to deepChar,
                0.68f to Color(0xFF1C0A05),
                1f to Color(0xFF280E07),
                startY = hearthTopY,
                endY = hearthBaseY,
            ),
        )

        // Rustic stone frame / lintel around cavity
        drawPath(
            path = archPath,
            color = Color(0xFF26120B),
            style = Stroke(width = minDim * 0.038f),
        )
        // Firelight grazing rim along the inner arch edge
        drawPath(
            path = archPath,
            color = TonalPalette.withAlpha(accent, (0.22f + 0.08f * heatPulse) * dim),
            style = Stroke(width = minDim * 0.012f),
        )

        // Wooden / stone mantel shelf above arch
        val mantelY = hearthTopY - minDim * 0.018f
        val mantelHalfW = hearthWidth * 0.58f
        drawRoundRect(
            color = Color(0xFF22110B),
            topLeft = Offset(cx - mantelHalfW, mantelY - minDim * 0.015f),
            size = Size(mantelHalfW * 2f, minDim * 0.030f),
            cornerRadius = CornerRadius(minDim * 0.008f),
        )
        drawLine(
            color = TonalPalette.withAlpha(accent, 0.28f * heatPulse * dim),
            start = Offset(cx - mantelHalfW * 0.9f, mantelY + minDim * 0.015f),
            end = Offset(cx + mantelHalfW * 0.9f, mantelY + minDim * 0.015f),
            strokeWidth = minDim * 0.0035f,
        )

        // 3. Firelight wash on floor / hearth bricks
        val floorH = h - hearthBaseY
        drawOval(
            brush = Brush.radialGradient(
                0f to TonalPalette.withAlpha(accent, (0.42f + 0.08f * heatPulse) * dim),
                0.55f to TonalPalette.withAlpha(Color(0xFF5A1C0A), 0.25f * dim),
                1f to Color.Transparent,
                center = Offset(cx, hearthBaseY + minDim * 0.02f),
                radius = (minDim * 0.65f).positiveRadius(),
            ),
            topLeft = Offset(cx - minDim * 0.65f, hearthBaseY - minDim * 0.12f),
            size = Size(minDim * 1.30f, minDim * 0.45f),
        )

        // Brick courses on the hearth floor
        for (row in 1..2) {
            val rowY = hearthBaseY + floorH * (row / 3f)
            drawLine(
                color = TonalPalette.withAlpha(Color(0xFF2A1008), 0.5f * dim),
                start = Offset(0f, rowY),
                end = Offset(w, rowY),
                strokeWidth = minDim * 0.003f,
            )
        }
        val vSeams = listOf(-0.28f, -0.09f, 0.10f, 0.29f, -0.38f, -0.19f, 0f, 0.19f, 0.38f)
        vSeams.forEachIndexed { idx, frac ->
            val row = if (idx < 4) 1 else 2
            val y1 = hearthBaseY + floorH * ((row - 1) / 3f)
            val y2 = hearthBaseY + floorH * (row / 3f)
            val seamX = cx + frac * minDim
            if (seamX in 0f..w) {
                val dist = abs(frac) / 0.45f
                val seamAlpha = (1f - dist).coerceIn(0f, 1f) * 0.32f * dim
                drawLine(
                    color = TonalPalette.withAlpha(accent, seamAlpha),
                    start = Offset(seamX, y1),
                    end = Offset(seamX, y2),
                    strokeWidth = minDim * 0.0025f,
                )
            }
        }

        // 4. Soft radiant ambient room glow
        drawCircle(
            brush = Brush.radialGradient(
                0f to TonalPalette.withAlpha(accent, 0.26f * heatPulse * dim),
                0.6f to TonalPalette.withAlpha(Color(0xFF5A1C0A), 0.12f * dim),
                1f to Color.Transparent,
                center = Offset(cx, emberY - minDim * 0.08f),
                radius = (minDim * 0.85f).positiveRadius(),
            ),
            radius = (minDim * 0.85f).positiveRadius(),
            center = Offset(cx, emberY - minDim * 0.08f),
        )

        // Inner cavity warm radiant bleed
        drawCircle(
            brush = Brush.radialGradient(
                0f to TonalPalette.withAlpha(hotEmber, 0.46f * heatPulse * dim),
                0.55f to TonalPalette.withAlpha(Color(0xFF8A1E06), 0.22f * dim),
                1f to Color.Transparent,
                center = Offset(cx + warpX * 0.4f, emberY),
                radius = (minDim * 0.45f).positiveRadius(),
            ),
            radius = (minDim * 0.45f).positiveRadius(),
            center = Offset(cx + warpX * 0.4f, emberY),
        )

        // 5. Firewood logs
        // Back log
        drawLine(
            color = Color(0xFF140704),
            start = Offset(cx - minDim * 0.24f, emberY + minDim * 0.035f),
            end = Offset(cx + minDim * 0.16f, emberY - minDim * 0.035f),
            strokeWidth = minDim * 0.045f,
            cap = StrokeCap.Round,
        )
        // Front angled log
        drawLine(
            color = Color(0xFF180805),
            start = Offset(cx + minDim * 0.24f, emberY + minDim * 0.045f),
            end = Offset(cx - minDim * 0.18f, emberY - minDim * 0.020f),
            strokeWidth = minDim * 0.052f,
            cap = StrokeCap.Round,
        )
        // Center split log
        drawLine(
            color = Color(0xFF110603),
            start = Offset(cx - minDim * 0.12f, emberY + minDim * 0.012f),
            end = Offset(cx + minDim * 0.13f, emberY + minDim * 0.018f),
            strokeWidth = minDim * 0.038f,
            cap = StrokeCap.Round,
        )

        // Hot glowing cracks in the firewood
        drawLine(
            color = TonalPalette.withAlpha(hotEmber, (0.65f + 0.35f * heatPulse) * dim),
            start = Offset(cx - minDim * 0.15f, emberY + minDim * 0.015f),
            end = Offset(cx + minDim * 0.08f, emberY - minDim * 0.020f),
            strokeWidth = minDim * 0.008f,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = TonalPalette.withAlpha(whiteHot, (0.80f + 0.20f * heatPulse) * dim),
            start = Offset(cx - minDim * 0.09f, emberY + minDim * 0.002f),
            end = Offset(cx + minDim * 0.02f, emberY - minDim * 0.012f),
            strokeWidth = minDim * 0.003f,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = TonalPalette.withAlpha(accent, (0.70f + 0.30f * heatPulse) * dim),
            start = Offset(cx - minDim * 0.06f, emberY - minDim * 0.008f),
            end = Offset(cx + minDim * 0.16f, emberY + minDim * 0.028f),
            strokeWidth = minDim * 0.009f,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = TonalPalette.withAlpha(whiteHot, (0.85f + 0.15f * heatPulse) * dim),
            start = Offset(cx - minDim * 0.01f, emberY),
            end = Offset(cx + minDim * 0.10f, emberY + minDim * 0.018f),
            strokeWidth = minDim * 0.0035f,
            cap = StrokeCap.Round,
        )

        // 6. Pulsing ember beds with shifting heat gradients
        val emberCenter = Offset(cx + warpX, emberY + warpY)
        // Broad deep-coals warmth
        drawOval(
            brush = Brush.radialGradient(
                0f to TonalPalette.withAlpha(Color(0xFFC02A08), 0.82f * heatPulse * dim),
                0.55f to TonalPalette.withAlpha(Color(0xFF6E1404), 0.45f * dim),
                1f to Color.Transparent,
                center = emberCenter,
                radius = (minDim * 0.30f).positiveRadius(),
            ),
            topLeft = Offset(emberCenter.x - minDim * 0.30f, emberCenter.y - minDim * 0.09f),
            size = Size(minDim * 0.60f, minDim * 0.18f),
        )

        // Bright fiery ember core
        val coreRx = minDim * 0.22f * (0.92f + 0.08f * heatPulse)
        val coreRy = minDim * 0.058f * (0.92f + 0.08f * heatPulse)
        drawOval(
            brush = Brush.radialGradient(
                0f to TonalPalette.withAlpha(Color(0xFFFF6A18), 0.88f * heatPulse * dim),
                0.6f to TonalPalette.withAlpha(Color(0xFFD63208), 0.50f * dim),
                1f to Color.Transparent,
                center = emberCenter,
                radius = coreRx.positiveRadius(),
            ),
            topLeft = Offset(emberCenter.x - coreRx, emberCenter.y - coreRy),
            size = Size(coreRx * 2f, coreRy * 2f),
        )

        // Incandescent coal clusters (white-hot spots)
        val hotSpots = listOf(
            Triple(-0.08f, 0.012f, 131),
            Triple(0.06f, 0.005f, 173),
            Triple(-0.01f, -0.015f, 211),
            Triple(0.13f, 0.018f, 257),
        )
        hotSpots.forEach { (xFrac, yFrac, seed) ->
            val spotPulse = 0.82f + 0.18f * loopedFbm(time, radius = 1.8f, seedOffset = seed)
            val spotPos = Offset(cx + xFrac * minDim + warpX * 0.5f, emberY + yFrac * minDim + warpY * 0.5f)
            val spotR = (minDim * 0.048f * spotPulse).positiveRadius()
            drawCircle(
                brush = Brush.radialGradient(
                    0f to TonalPalette.withAlpha(whiteHot, 0.90f * spotPulse * dim),
                    0.45f to TonalPalette.withAlpha(coreGold, 0.65f * spotPulse * dim),
                    1f to Color.Transparent,
                    center = spotPos,
                    radius = spotR,
                ),
                radius = spotR,
                center = spotPos,
            )
        }

        // 7. Ambient flame tongues rising from embers
        val tongueCount = qualityCount(quality, low = 2, medium = 3, high = 4)
        for (i in 0 until tongueCount) {
            val tFrac = when (i) {
                0 -> -0.07f
                1 -> 0.01f
                2 -> 0.08f
                else -> -0.03f
            }
            val tongueBreath = 0.82f + 0.28f * loopedFbm(time, radius = 1.4f + i * 0.3f, seedOffset = 307 + i * 61)
            val tongueSway = (loopedFbm(time, radius = 1.2f + i * 0.25f, seedOffset = 401 + i * 79) - 0.5f) * minDim * 0.06f
            val tongueH = minDim * (0.16f + 0.05f * i) * tongueBreath
            val halfW = minDim * (0.035f + 0.008f * i)
            val baseX = cx + tFrac * minDim
            val baseY = emberY + minDim * 0.01f
            val tipX = baseX + tongueSway
            val tipY = baseY - tongueH

            val flamePath = Path().apply {
                moveTo(baseX - halfW, baseY)
                cubicTo(
                    baseX - halfW * 1.2f, baseY - tongueH * 0.35f,
                    tipX - halfW * 0.5f, tipY + tongueH * 0.30f,
                    tipX, tipY,
                )
                cubicTo(
                    tipX + halfW * 0.5f, tipY + tongueH * 0.30f,
                    baseX + halfW * 1.2f, baseY - tongueH * 0.35f,
                    baseX + halfW, baseY,
                )
                close()
            }
            drawPath(
                path = flamePath,
                brush = Brush.verticalGradient(
                    0f to Color.Transparent,
                    0.25f to TonalPalette.withAlpha(hotEmber, 0.30f * dim),
                    0.70f to TonalPalette.withAlpha(coreGold, 0.75f * dim),
                    1f to TonalPalette.withAlpha(whiteHot, 0.90f * dim),
                    startY = tipY,
                    endY = baseY,
                ),
            )
        }

        // Soft flame bloom overlay
        drawCircle(
            brush = Brush.radialGradient(
                0f to TonalPalette.withAlpha(coreGold, 0.40f * heatPulse * dim),
                0.55f to TonalPalette.withAlpha(accent, 0.20f * dim),
                1f to Color.Transparent,
                center = Offset(cx, emberY - minDim * 0.08f),
                radius = (minDim * 0.22f).positiveRadius(),
            ),
            radius = (minDim * 0.22f).positiveRadius(),
            center = Offset(cx, emberY - minDim * 0.08f),
        )

        // 8. Subtle upward drifting embers / sparks
        sparks.forEach { spark ->
            val life = phase01(time * spark.riseSpeed + spark.phaseOffset)
            val startY = emberY - minDim * 0.01f
            val endY = hearthTopY - minDim * 0.16f
            val sy = lerp(startY, endY, life)
            val lateralDrift = spark.swayAmp * minDim * sin(time * 2f * PI.toFloat() * spark.swayFreq + spark.phaseOffset * 2f * PI.toFloat())
            val noiseSway = (loopedFbm(time, radius = 1.3f + spark.baseRadius * 100f, seedOffset = spark.colorIndex * 89 + 13) - 0.5f) * minDim * 0.035f
            val sx = cx + spark.xOffsetFrac * minDim + lateralDrift + noiseSway

            val fadeIn = (life / 0.12f).coerceIn(0f, 1f)
            val fadeOut = ((1f - life) / 0.35f).coerceIn(0f, 1f)
            val flicker = 0.75f + 0.25f * sin(time * 2f * PI.toFloat() * 4f + spark.phaseOffset * 10f)
            val alpha = (fadeIn * fadeOut * flicker * dim).coerceIn(0f, 1f)

            val sparkCol = when (spark.colorIndex % 3) {
                0 -> whiteHot
                1 -> accent
                else -> TonalPalette.mix(hotEmber, TonalPalette.pick(paletteColors, spark.colorIndex), 0.35f)
            }
            val haloR = (minDim * spark.baseRadius * 3.8f).positiveRadius()
            drawCircle(
                brush = Brush.radialGradient(
                    0f to TonalPalette.withAlpha(sparkCol, 0.48f * alpha),
                    1f to Color.Transparent,
                    center = Offset(sx, sy),
                    radius = haloR,
                ),
                radius = haloR,
                center = Offset(sx, sy),
            )
            drawCircle(
                color = TonalPalette.withAlpha(sparkCol, alpha),
                radius = (minDim * spark.baseRadius).positiveRadius(),
                center = Offset(sx, sy),
            )
        }

        // 9. Soft rustic vignette falloff
        drawRect(
            brush = Brush.radialGradient(
                0f to Color.Transparent,
                0.70f to Color.Transparent,
                1f to Color(0x990A0402),
                center = Offset(cx, emberY),
                radius = (minDim * 1.15f).positiveRadius(),
            ),
        )
    }
}

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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import fr.geoking.arthur.genart.GenartQuality
import fr.geoking.arthur.genart.TonalPalette
import fr.geoking.arthur.genart.phase01
import fr.geoking.arthur.genart.qualityCount
import fr.geoking.arthur.genart.seededRange
import fr.geoking.arthur.genart.seededUnit
import fr.geoking.arthur.genart.sin01
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private data class FlowerSeed(
    val xFrac: Float,
    val yFrac: Float,
    val scale: Float,
    val petalCount: Int,
    val petalShapeType: Int,
    val petalAspect: Float,
    val stamenCount: Int,
    val colorIndex: Int,
    val colorMix: Float,
    val openClosePhase: Float,
    val openCloseSpeed: Float,
    val swayPhase: Float,
    val swayAmp: Float,
    val stemCurveOffset: Float,
)

private data class PollenParticleSeed(
    val x0: Float,
    val y0: Float,
    val size: Float,
    val speedY: Float,
    val swayAmp: Float,
    val swayFreq: Float,
    val alpha: Float,
    val colorIndex: Int,
)

/**
 * A rich field of procedural flowers that sway in the breeze and slowly open and close (bloom and retract).
 * Features procedural petal veining, detailed stamen centers, leaves along stems, and ambient pollen dust.
 */
@Composable
internal fun FlowerEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val count = qualityCount(quality, low = 7, medium = 11, high = 16)
    val flowers = remember(count) {
        List(count) { i ->
            FlowerSeed(
                xFrac = seededRange(i * 17 + 3, 0.08f, 0.92f),
                yFrac = seededRange(i * 29 + 7, 0.22f, 0.68f),
                scale = seededRange(i * 41 + 11, 0.65f, 1.25f),
                petalCount = (5 + seededUnit(i * 53 + 13) * 7.99f).toInt().coerceIn(5, 12),
                petalShapeType = (seededUnit(i * 61 + 17) * 3f).toInt().coerceIn(0, 2),
                petalAspect = seededRange(i * 67 + 19, 1.3f, 2.2f),
                stamenCount = (8 + seededUnit(i * 79 + 23) * 10f).toInt().coerceIn(8, 18),
                colorIndex = i,
                colorMix = seededRange(i * 97 + 31, 0.2f, 0.8f),
                openClosePhase = seededRange(i * 103 + 37, 0f, 2f * PI.toFloat()),
                openCloseSpeed = seededRange(i * 109 + 41, 0.7f, 1.3f),
                swayPhase = seededRange(i * 113 + 43, 0f, 2f * PI.toFloat()),
                swayAmp = seededRange(i * 127 + 47, 0.02f, 0.05f),
                stemCurveOffset = seededRange(i * 131 + 53, -0.08f, 0.08f),
            )
        }.sortedBy { it.scale } // Draw smaller background flowers first
    }

    val pollenCount = qualityCount(quality, low = 16, medium = 28, high = 42)
    val pollens = remember(pollenCount) {
        List(pollenCount) { i ->
            PollenParticleSeed(
                x0 = seededUnit(i * 13 + 5),
                y0 = seededUnit(i * 19 + 7),
                size = seededRange(i * 23 + 11, 1.5f, 4f),
                speedY = seededRange(i * 29 + 13, 0.05f, 0.15f),
                swayAmp = seededRange(i * 31 + 17, 0.02f, 0.06f),
                swayFreq = seededRange(i * 37 + 19, 0.3f, 1.2f),
                alpha = seededRange(i * 41 + 23, 0.25f, 0.65f),
                colorIndex = i,
            )
        }
    }

    // Reusable Path instances to prevent allocations during animation frames
    val reusablePath = remember { Path() }

    val transition = rememberInfiniteTransition(label = "flower_meadow")
    val timeLoop by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((18000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "flower_time",
    )

    val activeScale = if (isActive) 1f else 0.5f
    val activeAlpha = if (isActive) 1f else 0.7f

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val minDim = w.coerceAtMost(h)
        val time = phase01(timeLoop)

        // Background dark garden gradient
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF0F140D), Color(0xFF172115), Color(0xFF0B1209)),
            ),
        )

        // Soft ambient glows behind flower clusters
        flowers.take(4).forEach { f ->
            val glowCenter = Offset(f.xFrac * w, f.yFrac * h)
            val glowRadius = minDim * 0.35f * f.scale
            val glowColor = TonalPalette.brightness(
                TonalPalette.pick(paletteColors, f.colorIndex),
                brightness * 0.5f,
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        TonalPalette.withAlpha(glowColor, 0.18f * activeAlpha),
                        Color.Transparent,
                    ),
                    center = glowCenter,
                    radius = glowRadius,
                ),
                radius = glowRadius,
                center = glowCenter,
            )
        }

        // Floating ambient pollen particles
        pollens.forEach { p ->
            val py = phase01(p.y0 - time * p.speedY) * h
            val px = phase01(p.x0 + sin(time * 2f * PI.toFloat() * p.swayFreq) * p.swayAmp) * w
            val pColor = TonalPalette.brightness(
                TonalPalette.mix(Color(0xFFFFF2A8), TonalPalette.pick(paletteColors, p.colorIndex), 0.4f),
                brightness,
            )
            drawCircle(
                color = TonalPalette.withAlpha(pColor, p.alpha * activeAlpha),
                radius = p.size * minDim * 0.003f * activeScale,
                center = Offset(px, py),
            )
        }

        // Draw flowers
        flowers.forEach { f ->
            // Bloom / opening & closing animation factor (smooth slow cycle)
            val openFactor = sin01(time * 2f * PI.toFloat() * f.openCloseSpeed + f.openClosePhase)
            val bloom = (0.18f + 0.82f * openFactor) * activeScale // 0.18 (bud) to 1.0 (bloomed)

            // Sway movement
            val swayTime = time * 2f * PI.toFloat() + f.swayPhase
            val swayAngle = sin(swayTime) * f.swayAmp * activeScale

            val headX = f.xFrac * w + sin(swayAngle) * (h * 0.2f)
            val headY = f.yFrac * h
            val baseX = f.xFrac * w
            val baseY = h * 1.02f // Extended off bottom

            val stemHeight = baseY - headY
            val controlX = baseX + (headX - baseX) * 0.5f + f.stemCurveOffset * w
            val controlY = headY + stemHeight * 0.55f

            // Stem drawing with dual shading
            val stemColor = TonalPalette.brightness(
                TonalPalette.mix(Color(0xFF2E4D25), Color(0xFF538243), 0.5f),
                brightness,
            )
            val stemStrokeWidth = minDim * 0.008f * f.scale

            reusablePath.reset()
            reusablePath.moveTo(baseX, baseY)
            reusablePath.quadraticTo(controlX, controlY, headX, headY)
            drawPath(
                path = reusablePath,
                color = TonalPalette.withAlpha(stemColor, 0.85f * activeAlpha),
                style = Stroke(width = stemStrokeWidth, cap = StrokeCap.Round),
            )

            // Side leaves along stem
            drawStemLeaves(
                path = reusablePath,
                baseX = baseX, baseY = baseY,
                controlX = controlX, controlY = controlY,
                headX = headX, headY = headY,
                stemColor = stemColor,
                scale = f.scale,
                minDim = minDim,
                alpha = activeAlpha,
                brightness = brightness,
            )

            // Draw Flower Head (Petals, Veins, Center Stamen)
            drawFlowerHead(
                path = reusablePath,
                headX = headX,
                headY = headY,
                bloom = bloom,
                flowerSeed = f,
                paletteColors = paletteColors,
                minDim = minDim,
                brightness = brightness,
                alpha = activeAlpha,
            )
        }

        // Base ground meadow shading
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color.Transparent, Color(0xAA0D170B), Color(0xF1070D06)),
                startY = h * 0.8f,
                endY = h,
            ),
        )
    }
}

private fun DrawScope.drawStemLeaves(
    path: Path,
    baseX: Float,
    baseY: Float,
    controlX: Float,
    controlY: Float,
    headX: Float,
    headY: Float,
    stemColor: Color,
    scale: Float,
    minDim: Float,
    alpha: Float,
    brightness: Float,
) {
    val leafColor = TonalPalette.brightness(stemColor, brightness * 1.15f)
    val leafSize = minDim * 0.045f * scale

    // Leaf 1 (lower stem)
    val t1 = 0.38f
    val lx1 = (1 - t1) * (1 - t1) * baseX + 2 * (1 - t1) * t1 * controlX + t1 * t1 * headX
    val ly1 = (1 - t1) * (1 - t1) * baseY + 2 * (1 - t1) * t1 * controlY + t1 * t1 * headY
    path.reset()
    path.moveTo(lx1, ly1)
    path.quadraticTo(lx1 - leafSize * 1.2f, ly1 - leafSize * 0.5f, lx1 - leafSize * 1.5f, ly1 - leafSize * 1.1f)
    path.quadraticTo(lx1 - leafSize * 0.4f, ly1 - leafSize * 1.2f, lx1, ly1)
    path.close()
    drawPath(
        path = path,
        color = TonalPalette.withAlpha(leafColor, 0.75f * alpha),
    )

    // Leaf 2 (higher stem, opposite side)
    val t2 = 0.65f
    val lx2 = (1 - t2) * (1 - t2) * baseX + 2 * (1 - t2) * t2 * controlX + t2 * t2 * headX
    val ly2 = (1 - t2) * (1 - t2) * baseY + 2 * (1 - t2) * t2 * controlY + t2 * t2 * headY
    path.reset()
    path.moveTo(lx2, ly2)
    path.quadraticTo(lx2 + leafSize * 1.2f, ly2 - leafSize * 0.4f, lx2 + leafSize * 1.4f, ly2 - leafSize * 1.0f)
    path.quadraticTo(lx2 + leafSize * 0.3f, ly2 - leafSize * 1.1f, lx2, ly2)
    path.close()
    drawPath(
        path = path,
        color = TonalPalette.withAlpha(leafColor, 0.75f * alpha),
    )
}

private fun DrawScope.drawFlowerHead(
    path: Path,
    headX: Float,
    headY: Float,
    bloom: Float,
    flowerSeed: FlowerSeed,
    paletteColors: List<Color>,
    minDim: Float,
    brightness: Float,
    alpha: Float,
) {
    val bloomRadius = minDim * 0.12f * flowerSeed.scale * bloom
    val petalLen = bloomRadius * flowerSeed.petalAspect
    val petalWidth = bloomRadius * (1.8f / flowerSeed.petalAspect)

    // Color derivation
    val basePaletteColor = TonalPalette.pick(paletteColors, flowerSeed.colorIndex)
    val pastelAccent = floralPastel(flowerSeed.colorIndex)
    val primaryPetalColor = TonalPalette.brightness(
        TonalPalette.mix(basePaletteColor, pastelAccent, flowerSeed.colorMix),
        brightness,
    )
    val innerPetalColor = TonalPalette.brightness(
        TonalPalette.mix(primaryPetalColor, Color.White, 0.35f),
        brightness,
    )
    val veinColor = TonalPalette.brightness(
        TonalPalette.mix(primaryPetalColor, Color(0xFF3D091B), 0.45f),
        brightness,
    )

    // Render Petals around center head
    for (i in 0 until flowerSeed.petalCount) {
        val baseAngle = (i.toFloat() / flowerSeed.petalCount) * 2f * PI.toFloat()
        // When bloom is low (closing), fold angles upward toward top angle (-PI/2)
        val targetAngle = -PI.toFloat() * 0.5f
        val currentAngle = baseAngle * bloom + targetAngle * (1f - bloom)
        val angleDeg = (currentAngle * 180f / PI.toFloat())

        withTransform({
            translate(left = headX, top = headY)
            rotate(degrees = angleDeg + 90f, pivot = Offset.Zero)
        }) {
            // Build petal shape path
            path.reset()
            buildPetalShapePath(
                path = path,
                length = petalLen,
                width = petalWidth,
                shapeType = flowerSeed.petalShapeType,
            )

            // Petal gradient fill
            val petalGradient = Brush.verticalGradient(
                colors = listOf(
                    TonalPalette.withAlpha(primaryPetalColor, 0.85f * alpha),
                    TonalPalette.withAlpha(innerPetalColor, 0.92f * alpha),
                    TonalPalette.withAlpha(primaryPetalColor, 0.60f * alpha),
                ),
                startY = 0f,
                endY = -petalLen,
            )
            drawPath(path = path, brush = petalGradient)

            // Procedural Vein Texture on Petals
            drawPetalVeins(
                path = path,
                length = petalLen,
                width = petalWidth,
                veinColor = veinColor,
                alpha = alpha * (0.35f + 0.45f * bloom),
            )
        }
    }

    // Flower Center (Disk & Stamens)
    val centerRadius = minDim * 0.038f * flowerSeed.scale * (0.4f + 0.6f * bloom)
    val diskColor = TonalPalette.brightness(Color(0xFFF2C94C), brightness)
    val diskBorderColor = TonalPalette.brightness(Color(0xFFD97706), brightness)

    // Stamens radiating from center when open
    if (bloom > 0.25f) {
        val stamenBloom = ((bloom - 0.25f) / 0.75f).coerceIn(0f, 1f)
        val stamenLen = centerRadius * 1.6f * stamenBloom
        val stamenColor = TonalPalette.brightness(Color(0xFFFFF7ED), brightness)
        val antherColor = TonalPalette.brightness(Color(0xFFFEF08A), brightness)

        for (s in 0 until flowerSeed.stamenCount) {
            val stamenAngle = (s.toFloat() / flowerSeed.stamenCount) * 2f * PI.toFloat()
            val tipX = headX + cos(stamenAngle) * stamenLen
            val tipY = headY + sin(stamenAngle) * stamenLen

            drawLine(
                color = TonalPalette.withAlpha(stamenColor, 0.75f * alpha),
                start = Offset(headX, headY),
                end = Offset(tipX, tipY),
                strokeWidth = minDim * 0.0025f,
                cap = StrokeCap.Round,
            )
            drawCircle(
                color = TonalPalette.withAlpha(antherColor, 0.9f * alpha),
                radius = minDim * 0.0055f * flowerSeed.scale,
                center = Offset(tipX, tipY),
            )
        }
    }

    // Center disk glow and core
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                TonalPalette.withAlpha(diskColor, 0.95f * alpha),
                TonalPalette.withAlpha(diskBorderColor, 0.85f * alpha),
                Color.Transparent,
            ),
            center = Offset(headX, headY),
            radius = centerRadius * 1.2f,
        ),
        radius = centerRadius,
        center = Offset(headX, headY),
    )
}

private fun buildPetalShapePath(path: Path, length: Float, width: Float, shapeType: Int) {
    val halfW = width * 0.5f
    path.moveTo(0f, 0f)
    when (shapeType) {
        1 -> { // Pointed teardrop
            path.quadraticTo(-halfW * 1.1f, -length * 0.45f, 0f, -length)
            path.quadraticTo(halfW * 1.1f, -length * 0.45f, 0f, 0f)
        }
        2 -> { // Flared notched petal
            path.quadraticTo(-halfW * 1.2f, -length * 0.5f, -halfW * 0.7f, -length * 0.9f)
            path.quadraticTo(-halfW * 0.2f, -length * 1.05f, 0f, -length * 0.92f)
            path.quadraticTo(halfW * 0.2f, -length * 1.05f, halfW * 0.7f, -length * 0.9f)
            path.quadraticTo(halfW * 1.2f, -length * 0.5f, 0f, 0f)
        }
        else -> { // Smooth rounded oval
            path.quadraticTo(-halfW, -length * 0.5f, 0f, -length)
            path.quadraticTo(halfW, -length * 0.5f, 0f, 0f)
        }
    }
    path.close()
}

private fun DrawScope.drawPetalVeins(
    path: Path,
    length: Float,
    width: Float,
    veinColor: Color,
    alpha: Float,
) {
    val strokeWidth = length * 0.025f
    val halfW = width * 0.4f
    val vColor = TonalPalette.withAlpha(veinColor, alpha * 0.45f)

    // Center main vein
    path.reset()
    path.moveTo(0f, 0f)
    path.quadraticTo(0f, -length * 0.5f, 0f, -length * 0.85f)
    drawPath(
        path = path,
        color = vColor,
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
    )

    // Left branch vein
    path.reset()
    path.moveTo(0f, -length * 0.25f)
    path.quadraticTo(-halfW * 0.6f, -length * 0.55f, -halfW * 0.8f, -length * 0.75f)
    drawPath(
        path = path,
        color = vColor,
        style = Stroke(width = strokeWidth * 0.7f, cap = StrokeCap.Round),
    )

    // Right branch vein
    path.reset()
    path.moveTo(0f, -length * 0.25f)
    path.quadraticTo(halfW * 0.6f, -length * 0.55f, halfW * 0.8f, -length * 0.75f)
    drawPath(
        path = path,
        color = vColor,
        style = Stroke(width = strokeWidth * 0.7f, cap = StrokeCap.Round),
    )
}

private fun floralPastel(index: Int): Color = when (index % 5) {
    0 -> Color(0xFFF7C6D9) // Soft Rose Pink
    1 -> Color(0xFFD8C6F7) // Soft Lavender
    2 -> Color(0xFFF7EFC6) // Primrose Yellow
    3 -> Color(0xFFFCA5A5) // Soft Peach Coral
    else -> Color(0xFFE9D5FF) // Gentle Orchid
}

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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import fr.geoking.arthur.genart.GenartQuality
import fr.geoking.arthur.genart.TonalPalette
import fr.geoking.arthur.genart.loopedFbm
import fr.geoking.arthur.genart.phase01
import fr.geoking.arthur.genart.positiveRadius
import fr.geoking.arthur.genart.qualityCount
import fr.geoking.arthur.genart.seededRange
import fr.geoking.arthur.genart.seededUnit
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

private data class AutumnLeafData(
    val x0: Float,
    val y0: Float,
    val fallCycles: Int,
    val size: Float,
    val swayAmp: Float,
    val swayCycles: Int,
    val swayPhase: Float,
    val rotCycles: Int,
    val rotOffset: Float,
    val flipCycles: Int,
    val flipPhase: Float,
    val colorIndex: Int,
    val depth: Float,
)

private data class AutumnDustMote(
    val x0: Float,
    val y0: Float,
    val rOffset: Float,
    val radius: Float,
    val pulseFreq: Int,
    val pulsePhase: Float,
    val colorIndex: Int,
    val seed: Int,
)

private fun generateLeaves(count: Int): List<AutumnLeafData> = List(count) { i ->
    val seed = i * 137 + 19
    AutumnLeafData(
        x0 = seededUnit(seed + 1),
        y0 = seededUnit(seed + 3),
        fallCycles = seededRange(seed + 5, 1f, 3f).toInt().coerceIn(1, 3),
        size = seededRange(seed + 7, 0.024f, 0.052f),
        swayAmp = seededRange(seed + 9, 0.035f, 0.085f),
        swayCycles = seededRange(seed + 11, 2f, 5f).toInt().coerceIn(2, 5),
        swayPhase = seededRange(seed + 13, 0f, 2f * PI.toFloat()),
        rotCycles = when (i % 5) {
            0 -> 1
            1 -> -1
            2 -> 2
            3 -> -2
            else -> 1
        },
        rotOffset = seededRange(seed + 17, 0f, 360f),
        flipCycles = seededRange(seed + 19, 3f, 7f).toInt().coerceIn(3, 7),
        flipPhase = seededRange(seed + 21, 0f, 2f * PI.toFloat()),
        colorIndex = i,
        depth = seededRange(seed + 23, 0.45f, 1.0f),
    )
}

private fun generateDustMotes(count: Int): List<AutumnDustMote> = List(count) { i ->
    val seed = i * 43 + 17
    AutumnDustMote(
        x0 = seededUnit(seed + 1),
        y0 = seededUnit(seed + 3),
        rOffset = seededRange(seed + 5, 0.05f, 0.25f),
        radius = seededRange(seed + 7, 0.005f, 0.015f),
        pulseFreq = (i % 3) + 1,
        pulsePhase = seededRange(seed + 9, 0f, 2f * PI.toFloat()),
        colorIndex = i,
        seed = seed,
    )
}

/** Soft Ambient Season Autumn Wash — seamless loop via loopedFbm. */
@Composable
internal fun SeasonAutumnWashEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val washCount = qualityCount(quality, low = 4, medium = 7, high = 11)
    val leafCount = qualityCount(quality, low = 10, medium = 18, high = 28)
    val dustCount = qualityCount(quality, low = 8, medium = 14, high = 22)

    val leaves = remember(leafCount) { generateLeaves(leafCount) }
    val dustMotes = remember(dustCount) { generateDustMotes(dustCount) }

    val baseOutlinePath = remember {
        Path().apply {
            moveTo(0f, -0.55f)
            cubicTo(0.48f, -0.35f, 0.40f, 0.35f, 0f, 0.85f)
            cubicTo(-0.40f, 0.35f, -0.48f, -0.35f, 0f, -0.55f)
            close()
        }
    }
    val baseVeinPath = remember {
        Path().apply {
            moveTo(0f, -0.42f)
            lineTo(0f, 1.08f)
            moveTo(0f, -0.25f)
            lineTo(0.20f, -0.42f)
            moveTo(0f, -0.25f)
            lineTo(-0.20f, -0.42f)
            moveTo(0f, 0.15f)
            lineTo(0.18f, -0.02f)
            moveTo(0f, 0.15f)
            lineTo(-0.18f, -0.02f)
            moveTo(0f, 0.50f)
            lineTo(0.14f, 0.35f)
            moveTo(0f, 0.50f)
            lineTo(-0.14f, 0.35f)
        }
    }
    val washPath = remember { Path() }

    val transition = rememberInfiniteTransition(label = "seasonautumnwash")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((72000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "seasonautumnwash_t",
    )
    val dim = if (isActive) 1f else 0.55f

    val leafPalette = remember {
        listOf(
            Color(0xFFE5A84B), // Amber gold
            Color(0xFF6B2E1A), // Deep russet
            Color(0xFFE8B86D), // Golden ochre
            Color(0xFFA3281E), // Crimson maple
            Color(0xFFC9783A), // Warm amber
            Color(0xFFD96624), // Burnt orange
        )
    }

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val minDim = minOf(w, h)
        val time = phase01(t)

        // Domain-warped atmospheric flow
        val warpX = (loopedFbm(time, radius = 1.3f, octaves = 2, seedOffset = 101) * 2f - 1f) * 0.15f
        val globalWash = loopedFbm(phase01(time + warpX), radius = 1.4f, octaves = 3, seedOffset = 11)

        // 1. Dusk autumn sky backdrop
        drawRect(
            brush = Brush.verticalGradient(
                0.0f to TonalPalette.brightness(Color(0xFF25110E), brightness * dim),
                0.35f to TonalPalette.brightness(Color(0xFF5A2315), brightness * dim),
                0.65f to TonalPalette.brightness(Color(0xFF964320), brightness * dim),
                0.88f to TonalPalette.brightness(Color(0xFFC9783A), brightness * dim * (0.92f + 0.08f * globalWash)),
                1.0f to TonalPalette.brightness(Color(0xFF521C12), brightness * dim),
            ),
        )

        // 2. Warm golden backlight bloom
        val sunDriftX = (loopedFbm(time, radius = 1.15f, seedOffset = 29) * 2f - 1f) * 0.08f
        val sunDriftY = (loopedFbm(time, radius = 1.35f, seedOffset = 47) * 2f - 1f) * 0.05f
        val sunCx = w * (0.52f + sunDriftX)
        val sunCy = h * (0.34f + sunDriftY)
        val sunRadius = (minDim * (0.65f + 0.08f * globalWash)).positiveRadius()

        val goldenCore = TonalPalette.brightness(
            TonalPalette.mix(Color(0xFFFFF0D0), TonalPalette.pick(paletteColors, 0), 0.3f),
            brightness * dim,
        )
        val goldenMid = TonalPalette.brightness(
            TonalPalette.mix(Color(0xFFE8B86D), TonalPalette.pick(paletteColors, 1), 0.25f),
            brightness * dim,
        )
        val amberEdge = TonalPalette.brightness(
            TonalPalette.mix(Color(0xFFC9783A), TonalPalette.pick(paletteColors, 2), 0.2f),
            brightness * dim,
        )

        drawCircle(
            brush = Brush.radialGradient(
                0.0f to TonalPalette.withAlpha(goldenCore, 0.62f * dim),
                0.28f to TonalPalette.withAlpha(goldenMid, 0.38f * dim),
                0.62f to TonalPalette.withAlpha(amberEdge, 0.16f * dim),
                1.0f to Color.Transparent,
                center = Offset(sunCx, sunCy),
                radius = sunRadius,
            ),
            radius = sunRadius,
            center = Offset(sunCx, sunCy),
        )

        // Secondary ground/horizon ambient warmth
        val groundRadius = (minDim * 0.58f).positiveRadius()
        val groundCenter = Offset(w * 0.48f, h * 0.88f)
        drawCircle(
            brush = Brush.radialGradient(
                0.0f to TonalPalette.withAlpha(amberEdge, 0.26f * dim),
                0.55f to TonalPalette.withAlpha(Color(0xFF6B2E1A), 0.12f * dim),
                1.0f to Color.Transparent,
                center = groundCenter,
                radius = groundRadius,
            ),
            radius = groundRadius,
            center = groundCenter,
        )

        // 3. Layered domain-warped atmospheric wash veils
        for (i in 0 until washCount) {
            val u = (i + 1).toFloat() / (washCount + 1).toFloat()
            val baseY = h * (0.28f + 0.62f * u)

            val warp = (loopedFbm(time, radius = 1.2f + i * 0.12f, octaves = 2, seedOffset = 101 + i * 29) * 2f - 1f) * 0.15f
            val n0 = loopedFbm(phase01(time + warp), radius = 1.35f, octaves = 2, seedOffset = i * 47)
            val n1 = loopedFbm(phase01(time + warp + 0.22f), radius = 1.35f, octaves = 2, seedOffset = i * 47 + 23)
            val n2 = loopedFbm(phase01(time + warp + 0.44f), radius = 1.35f, octaves = 2, seedOffset = i * 47 + 46)
            val n3 = loopedFbm(phase01(time + warp + 0.66f), radius = 1.35f, octaves = 2, seedOffset = i * 47 + 69)

            val amp = h * (0.035f + 0.025f * u)
            val y0 = baseY + (n0 * 2f - 1f) * amp
            val y1 = baseY + (n1 * 2f - 1f) * amp
            val y2 = baseY + (n2 * 2f - 1f) * amp
            val y3 = baseY + (n3 * 2f - 1f) * amp

            val x1 = w * 0.33f
            val x2 = w * 0.67f

            washPath.reset()
            washPath.moveTo(0f, h)
            washPath.lineTo(0f, y0)
            washPath.cubicTo(x1 * 0.5f, y0, x1 * 0.5f, y1, x1, y1)
            washPath.cubicTo(x1 + (x2 - x1) * 0.5f, y1, x1 + (x2 - x1) * 0.5f, y2, x2, y2)
            washPath.cubicTo(x2 + (w - x2) * 0.5f, y2, x2 + (w - x2) * 0.5f, y3, w, y3)
            washPath.lineTo(w, h)
            washPath.close()

            val layerColor = when (i % 5) {
                0 -> TonalPalette.mix(Color(0xFF6B2E1A), TonalPalette.pick(paletteColors, 0), 0.2f)
                1 -> TonalPalette.mix(Color(0xFFC9783A), TonalPalette.pick(paletteColors, 1), 0.2f)
                2 -> TonalPalette.mix(Color(0xFFE8B86D), TonalPalette.pick(paletteColors, 2), 0.25f)
                3 -> TonalPalette.mix(Color(0xFF9E4B28), TonalPalette.pick(paletteColors, 3), 0.2f)
                else -> TonalPalette.mix(Color(0xFF82321C), TonalPalette.pick(paletteColors, 4), 0.2f)
            }

            val layerAlpha = (0.13f + 0.15f * (1f - u * 0.45f)) * dim
            drawPath(
                path = washPath,
                brush = Brush.verticalGradient(
                    0f to TonalPalette.withAlpha(TonalPalette.brightness(layerColor, brightness * dim), layerAlpha),
                    1f to TonalPalette.withAlpha(TonalPalette.brightness(Color(0xFF38140E), brightness * dim), 0.04f * dim),
                    startY = (baseY - h * 0.05f).coerceAtLeast(0f),
                    endY = h,
                ),
            )
        }

        // 4. Shimmering dust motes in backlight
        dustMotes.forEach { mote ->
            val dx = (loopedFbm(time, radius = 1.1f + mote.rOffset, seedOffset = mote.seed + 19) * 2f - 1f) * 0.05f * w
            val dy = (loopedFbm(time, radius = 1.25f + mote.rOffset, seedOffset = mote.seed + 31) * 2f - 1f) * 0.05f * h
            val mx = phase01(mote.x0 + dx / w) * w
            val my = phase01(mote.y0 + dy / h) * h
            val pulse = sin(time * 2f * PI.toFloat() * mote.pulseFreq + mote.pulsePhase) * 0.5f + 0.5f
            val moteAlpha = (0.15f + 0.35f * pulse) * dim
            val moteColor = TonalPalette.withAlpha(
                TonalPalette.mix(Color(0xFFFFE8B0), TonalPalette.pick(paletteColors, mote.colorIndex), 0.25f),
                moteAlpha,
            )
            drawCircle(
                color = moteColor,
                radius = mote.radius * minDim,
                center = Offset(mx, my),
            )
        }

        // 5. Tumbling autumn leaf particles with wind drift and 3D flutter
        leaves.forEach { leaf ->
            val fallProgress = phase01(leaf.y0 + leaf.fallCycles * time)
            val margin = minDim * 0.08f
            val leafY = -margin + fallProgress * (h + margin * 2f)

            val windDrift = (loopedFbm(time, radius = 1.35f, octaves = 2, seedOffset = 77 + leaf.colorIndex * 19) * 2f - 1f) * 0.08f
            val sway = sin(time * 2f * PI.toFloat() * leaf.swayCycles + leaf.swayPhase)
            val leafX = phase01(leaf.x0 + sway * leaf.swayAmp + windDrift) * w

            val rotation = leaf.rotOffset + leaf.rotCycles * 360f * time
            val flip = cos(time * 2f * PI.toFloat() * leaf.flipCycles + leaf.flipPhase)
            val leafWidthScale = (0.2f + 0.8f * abs(flip)) * (if (flip < 0) -1f else 1f)
            val leafScale = minDim * leaf.size * leaf.depth

            val baseCol = leafPalette[leaf.colorIndex % leafPalette.size]
            val mixedCol = TonalPalette.mix(baseCol, TonalPalette.pick(paletteColors, leaf.colorIndex), 0.25f)
            val leafColor = TonalPalette.withAlpha(
                TonalPalette.brightness(mixedCol, brightness * dim),
                (0.68f + 0.28f * leaf.depth) * dim,
            )
            val veinColor = TonalPalette.withAlpha(
                TonalPalette.brightness(Color(0xFF38140D), brightness * dim),
                (0.55f + 0.35f * leaf.depth) * dim,
            )

            val strokeWidth = (minDim * 0.003f).coerceAtLeast(1f)

            withTransform({
                translate(left = leafX, top = leafY)
                rotate(degrees = rotation, pivot = Offset.Zero)
                scale(scaleX = leafScale * leafWidthScale, scaleY = leafScale, pivot = Offset.Zero)
            }) {
                drawPath(path = baseOutlinePath, color = leafColor)
                drawPath(
                    path = baseVeinPath,
                    color = veinColor,
                    style = Stroke(width = strokeWidth / leafScale, cap = StrokeCap.Round),
                )
            }
        }

        // 6. Soft warm vignette edge
        val vignetteRadius = (minDim * 0.95f).positiveRadius()
        drawRect(
            brush = Brush.radialGradient(
                0.65f to Color.Transparent,
                1.0f to TonalPalette.withAlpha(Color(0xFF220E0A), 0.38f * dim),
                center = Offset(w * 0.5f, h * 0.5f),
                radius = vignetteRadius,
            ),
        )
    }
}

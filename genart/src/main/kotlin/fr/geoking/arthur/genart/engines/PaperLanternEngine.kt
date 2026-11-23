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
import fr.geoking.arthur.genart.loopedFbm
import fr.geoking.arthur.genart.phase01
import fr.geoking.arthur.genart.positiveRadius
import fr.geoking.arthur.genart.qualityCount
import fr.geoking.arthur.genart.seededRange
import fr.geoking.arthur.genart.seededUnit
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import kotlin.math.sqrt

private data class PaperLanternMote(
    val x0: Float,
    val y0: Float,
    val radius: Float,
    val driftX: Float,
    val driftY: Float,
    val phaseOffset: Float,
    val colorIndex: Int,
)

/** Soft Ambient Paper Lantern Glow — seamless loop via loopedFbm. */
@Composable
internal fun PaperLanternEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val count = qualityCount(quality, low = 4, medium = 7, high = 11)
    val motes = remember(count) {
        List(count) { i ->
            PaperLanternMote(
                x0 = seededUnit(i * 17 + 3),
                y0 = seededUnit(i * 29 + 7),
                radius = seededRange(i * 41 + 11, 0.025f, 0.065f),
                driftX = seededRange(i * 53 + 13, 0.025f, 0.06f),
                driftY = seededRange(i * 67 + 19, 0.035f, 0.08f),
                phaseOffset = seededRange(i * 71 + 23, 0f, 2f * PI.toFloat()),
                colorIndex = i,
            )
        }
    }
    val transition = rememberInfiniteTransition(label = "paperlantern")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((72000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "paperlantern_t",
    )
    val dim = if (isActive) 1f else 0.55f

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val minDim = minOf(w, h)
        val time = phase01(t)

        // Color definitions
        val baseDark = TonalPalette.brightness(Color(0xFF1A1210), brightness * dim)
        val midDark = TonalPalette.brightness(Color(0xFF281812), brightness * dim)
        val deepDark = TonalPalette.brightness(Color(0xFF3A2218), brightness * dim)
        val accent = TonalPalette.brightness(
            TonalPalette.mix(Color(0xFFFFB060), TonalPalette.pick(paletteColors, 0), 0.35f),
            brightness * dim,
        )

        // Ambient background wash
        val wash = loopedFbm(time, radius = 1.35f, octaves = 2, seedOffset = 11)
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    TonalPalette.brightness(baseDark, 0.90f + 0.10f * wash),
                    midDark,
                    deepDark,
                ),
            ),
        )

        // Subtle upper architecture silhouette (eave / beam)
        val eaveH = h * 0.05f
        val woodDark = TonalPalette.brightness(Color(0xFF120B08), brightness * dim)
        drawRect(
            color = woodDark,
            topLeft = Offset(0f, 0f),
            size = Size(w, eaveH),
        )
        drawRect(
            color = TonalPalette.brightness(Color(0xFF24150F), brightness * dim),
            topLeft = Offset(0f, eaveH - minDim * 0.006f),
            size = Size(w, minDim * 0.006f),
        )

        // Gentle swaying motion driven seamlessly by loopedFbm
        val sway = (loopedFbm(time, radius = 1.25f, octaves = 2, seedOffset = 19) * 2f - 1f) * 0.032f
        val bob = (loopedFbm(time, radius = 1.1f, octaves = 2, seedOffset = 47) * 2f - 1f) * 0.008f
        val cx = w * 0.5f + minDim * sway
        val cy = h * 0.44f + minDim * bob

        val rx = minDim * 0.21f
        val ry = minDim * 0.26f

        // Candle flicker
        val flicker = loopedFbm(time, radius = 2.1f, octaves = 3, seedOffset = 91)
        val flickerScale = 0.94f + 0.12f * flicker
        val flickerAlpha = 0.88f + 0.22f * flicker

        // 1. Ambient warm radial lantern light pool illuminating surrounding darkness
        val lightPoolRadius = (minDim * 0.82f).positiveRadius()
        drawCircle(
            brush = Brush.radialGradient(
                0f to TonalPalette.withAlpha(accent, 0.42f * dim * flickerAlpha),
                0.32f to TonalPalette.withAlpha(Color(0xFF9E4218), 0.22f * dim),
                0.68f to TonalPalette.withAlpha(Color(0xFF481A10), 0.08f * dim),
                1f to Color.Transparent,
                center = Offset(cx, cy),
                radius = lightPoolRadius,
            ),
            radius = lightPoolRadius,
            center = Offset(cx, cy),
        )

        // Soft floor reflection pool
        val floorGlowRadius = (minDim * 0.55f).positiveRadius()
        val floorGlowCenter = Offset(cx, h * 0.90f)
        drawOval(
            brush = Brush.radialGradient(
                0f to TonalPalette.withAlpha(accent, 0.20f * dim * flickerAlpha),
                0.55f to TonalPalette.withAlpha(Color(0xFF5A2214), 0.08f * dim),
                1f to Color.Transparent,
                center = floorGlowCenter,
                radius = floorGlowRadius,
            ),
            topLeft = Offset(cx - minDim * 0.40f, h * 0.80f),
            size = Size(minDim * 0.80f, minDim * 0.20f),
        )

        // 2. Suspension wire / cord
        val cordTop = Offset(w * 0.5f, eaveH)
        val topCapH = minDim * 0.032f
        val topCapW = rx * 0.70f
        val cordBottom = Offset(cx, cy - ry - topCapH)
        drawLine(
            color = woodDark,
            start = cordTop,
            end = cordBottom,
            strokeWidth = minDim * 0.005f,
            cap = StrokeCap.Round,
        )

        // 3. Floating atmospheric motes behind and around
        motes.forEach { mote ->
            val n = loopedFbm(time, radius = 1.3f + mote.radius, seedOffset = mote.colorIndex * 97 + 5)
            val motX = (mote.x0 + mote.driftX * (n * 2f - 1f) + 1f) % 1f * w
            val motY = ((mote.y0 - time * mote.driftY + mote.driftY * sin(time * 2f * PI.toFloat() + mote.phaseOffset) * 0.25f) % 1f + 1f) % 1f * h
            val r = (minDim * mote.radius * (0.8f + 0.35f * n)).positiveRadius()

            val dx = motX - cx
            val dy = motY - cy
            val dist = sqrt(dx * dx + dy * dy)
            val glowProx = (1f - (dist / (minDim * 0.7f))).coerceIn(0.15f, 1f)

            val moteColor = TonalPalette.brightness(
                TonalPalette.mix(accent, TonalPalette.pick(paletteColors, mote.colorIndex), 0.4f),
                brightness * dim,
            )
            // Soft halo
            drawCircle(
                brush = Brush.radialGradient(
                    0f to TonalPalette.withAlpha(moteColor, 0.45f * dim * glowProx),
                    1f to Color.Transparent,
                    center = Offset(motX, motY),
                    radius = r,
                ),
                radius = r,
                center = Offset(motX, motY),
            )
            // Core spark
            drawCircle(
                color = TonalPalette.withAlpha(Color(0xFFFFF2D8), (0.4f + 0.5f * glowProx) * dim),
                radius = (r * 0.25f).positiveRadius(0.5f),
                center = Offset(motX, motY),
            )
        }

        // 4. Paper Lantern Translucent Body
        val lanternTopLeft = Offset(cx - rx, cy - ry)
        val lanternSize = Size(rx * 2f, ry * 2f)

        // Translucent paper body glow
        val paperGlowRadius = (maxOf(rx, ry) * 1.05f).positiveRadius()
        val candleCoreOffset = Offset(cx, cy + ry * 0.05f)
        drawOval(
            brush = Brush.radialGradient(
                0f to TonalPalette.mix(Color(0xFFFFF6D8), accent, 0.22f),
                0.28f to TonalPalette.mix(Color(0xFFFFB458), accent, 0.45f),
                0.62f to Color(0xFFD85E20),
                0.86f to Color(0xFF88280E),
                1f to Color(0xFF451208),
                center = candleCoreOffset,
                radius = paperGlowRadius,
            ),
            topLeft = lanternTopLeft,
            size = lanternSize,
        )

        // 5. Candle flame core inside
        val flameRadius = (minDim * 0.12f * flickerScale).positiveRadius()
        drawCircle(
            brush = Brush.radialGradient(
                0f to TonalPalette.withAlpha(Color.White, 0.95f * dim),
                0.32f to TonalPalette.withAlpha(Color(0xFFFFE88A), 0.85f * dim),
                0.68f to TonalPalette.withAlpha(Color(0xFFFF9426), 0.45f * dim),
                1f to Color.Transparent,
                center = candleCoreOffset,
                radius = flameRadius,
            ),
            radius = flameRadius,
            center = candleCoreOffset,
        )

        // 6. Bamboo Ribs & Struts
        // Vertical struts (curved side arcs)
        val strutColor = TonalPalette.withAlpha(Color(0xFF381408), 0.35f * dim)
        val strutWidth = minDim * 0.0035f
        val vertPath = Path()
        // Left vertical strut
        vertPath.reset()
        vertPath.moveTo(cx - topCapW * 0.35f, cy - ry)
        vertPath.quadraticTo(cx - rx * 0.62f, cy, cx - topCapW * 0.32f, cy + ry)
        drawPath(vertPath, color = strutColor, style = Stroke(width = strutWidth))
        // Center vertical strut
        drawLine(
            color = strutColor,
            start = Offset(cx, cy - ry),
            end = Offset(cx, cy + ry),
            strokeWidth = strutWidth,
        )
        // Right vertical strut
        vertPath.reset()
        vertPath.moveTo(cx + topCapW * 0.35f, cy - ry)
        vertPath.quadraticTo(cx + rx * 0.62f, cy, cx + topCapW * 0.32f, cy + ry)
        drawPath(vertPath, color = strutColor, style = Stroke(width = strutWidth))

        // Horizontal bamboo hoops (curved ribs)
        val ribCount = 9
        val ribPath = Path()
        for (i in 0 until ribCount) {
            val v = -0.74f + i * (1.48f / (ribCount - 1))
            val ribY = cy + ry * v
            val halfW = rx * sqrt((1f - v * v).coerceAtLeast(0f))
            val sag = minDim * 0.014f * (1f - 0.25f * v)

            ribPath.reset()
            ribPath.moveTo(cx - halfW, ribY)
            ribPath.quadraticTo(cx, ribY + sag, cx + halfW, ribY)

            val ribAlpha = (0.32f + 0.14f * (1f - abs(v))) * dim
            val ribShadowColor = TonalPalette.withAlpha(Color(0xFF280E06), ribAlpha)
            drawPath(ribPath, color = ribShadowColor, style = Stroke(width = minDim * 0.004f))

            // Delicate warm highlight below rib
            val ribHighlightPath = Path()
            ribHighlightPath.moveTo(cx - halfW * 0.95f, ribY + minDim * 0.0025f)
            ribHighlightPath.quadraticTo(cx, ribY + sag + minDim * 0.0025f, cx + halfW * 0.95f, ribY + minDim * 0.0025f)
            drawPath(
                ribHighlightPath,
                color = TonalPalette.withAlpha(accent, 0.16f * dim * (1f - abs(v))),
                style = Stroke(width = minDim * 0.002f),
            )
        }

        // Delicate paper lantern silhouette rim
        drawOval(
            color = TonalPalette.withAlpha(Color(0xFF381206), 0.55f * dim),
            topLeft = lanternTopLeft,
            size = lanternSize,
            style = Stroke(width = minDim * 0.004f),
        )

        // 7. Wooden Top Collar / Cap
        val topCapY = cy - ry - topCapH * 0.5f
        val capWood = TonalPalette.brightness(Color(0xFF22130C), brightness * dim)
        val capHighlight = TonalPalette.brightness(Color(0xFF4C2A1A), brightness * dim)
        drawRoundRect(
            color = capWood,
            topLeft = Offset(cx - topCapW * 0.5f, topCapY),
            size = Size(topCapW, topCapH),
            cornerRadius = CornerRadius(minDim * 0.008f, minDim * 0.008f),
        )
        drawRect(
            color = capHighlight,
            topLeft = Offset(cx - topCapW * 0.45f, topCapY),
            size = Size(topCapW * 0.90f, topCapH * 0.25f),
        )

        // Top loop
        drawCircle(
            color = capWood,
            radius = minDim * 0.012f,
            center = Offset(cx, topCapY),
            style = Stroke(width = minDim * 0.004f),
        )

        // 8. Wooden Bottom Collar / Rim
        val botCapH = minDim * 0.026f
        val botCapW = rx * 0.62f
        val botCapY = cy + ry - botCapH * 0.5f
        drawRoundRect(
            color = capWood,
            topLeft = Offset(cx - botCapW * 0.5f, botCapY),
            size = Size(botCapW, botCapH),
            cornerRadius = CornerRadius(minDim * 0.008f, minDim * 0.008f),
        )

        // Bottom finial bead
        val finialY = botCapY + botCapH + minDim * 0.006f
        drawCircle(
            color = capWood,
            radius = minDim * 0.009f,
            center = Offset(cx, finialY),
        )

        // 9. Hanging Silk Tassel
        val tasselLagSway = (loopedFbm(time, radius = 1.2f, octaves = 2, seedOffset = 23) * 2f - 1f) * 0.050f
        val tasselLen = minDim * 0.13f
        val tasselTop = Offset(cx, finialY + minDim * 0.006f)
        val tasselTipX = cx + minDim * tasselLagSway
        val tasselTipY = tasselTop.y + tasselLen

        // Knot
        drawCircle(
            color = TonalPalette.brightness(Color(0xFF6B1A0E), brightness * dim),
            radius = minDim * 0.007f,
            center = tasselTop,
        )

        // Silk tassel fringe strands
        val tasselColors = listOf(
            TonalPalette.brightness(Color(0xFF8C2212), brightness * dim),
            TonalPalette.brightness(Color(0xFFB83A1C), brightness * dim),
            TonalPalette.brightness(Color(0xFF6E180C), brightness * dim),
        )
        for (k in -2..2) {
            val strandX = tasselTipX + k * minDim * 0.005f
            drawLine(
                color = tasselColors[abs(k) % tasselColors.size],
                start = tasselTop,
                end = Offset(strandX, tasselTipY),
                strokeWidth = minDim * 0.003f,
                cap = StrokeCap.Round,
            )
        }

        // 10. Soft atmospheric night vignette
        val vignetteRadius = (maxOf(w, h) * 0.85f).positiveRadius()
        drawRect(
            brush = Brush.radialGradient(
                0f to Color.Transparent,
                0.62f to Color.Transparent,
                1f to TonalPalette.withAlpha(Color(0xFF0A0605), 0.65f * dim),
                center = Offset(cx, cy),
                radius = vignetteRadius,
            ),
        )
    }
}

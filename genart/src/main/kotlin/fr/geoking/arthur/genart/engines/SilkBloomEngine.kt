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
import androidx.compose.ui.graphics.drawscope.DrawScope
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

/**
 * Wear-face silk bloom: a few large overlapping translucent sheets (peach / mint / cream)
 * that open and slowly twist on pure black, with dense dew-mist spray on pale petals.
 */
@Composable
internal fun SilkBloomEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val petalCount = qualityCount(quality, low = 5, medium = 6, high = 8)
    val grainPerPetal = qualityCount(quality, low = 90, medium = 140, high = 200)
    val mistPerPale = qualityCount(quality, low = 55, medium = 90, high = 130)

    val petals = remember(petalCount) {
        List(petalCount) { i ->
            val tone = i % 3 // 0 peach, 1 mint, 2 cream/lavender
            SilkBloomPetal(
                angle0 = seededRange(i * 17 + 3, -PI.toFloat(), PI.toFloat()),
                lengthFrac = seededRange(i * 29 + 7, 0.38f, 0.58f),
                widthFrac = seededRange(i * 41 + 11, 0.28f, 0.48f),
                twist = seededRange(i * 53 + 13, -0.85f, 0.85f),
                bend = seededRange(i * 67 + 19, 0.35f, 0.95f),
                openPhase = seededRange(i * 79 + 23, 0f, 2f * PI.toFloat()),
                openSpeed = seededRange(i * 89 + 29, 0.45f, 0.95f),
                spinMul = seededRange(i * 97 + 31, 0.55f, 1.15f),
                tone = tone,
                colorMix = seededUnit(i * 103 + 37),
                alpha = when (tone) {
                    2 -> seededRange(i * 109 + 43, 0.35f, 0.58f) // pale translucent
                    else -> seededRange(i * 109 + 43, 0.62f, 0.88f)
                },
                misty = tone == 2 || seededUnit(i * 113 + 47) > 0.72f,
                layer = seededUnit(i * 127 + 53),
                grainSeed = i * 131 + 59,
            )
        }.sortedBy { it.layer }
    }

    val mist = remember(petalCount, mistPerPale) {
        petals.mapIndexed { idx, petal ->
            if (!petal.misty) emptyList()
            else List(mistPerPale) { j ->
                val s = idx * 1000 + j * 17
                SilkBloomMist(
                    petalIndex = idx,
                    along = seededRange(s + 3, 0.15f, 0.92f),
                    side = seededRange(s + 7, -0.95f, 0.95f),
                    size = seededRange(s + 11, 0.7f, 2.8f),
                    drift = seededRange(s + 13, 0.04f, 0.14f),
                    phase = seededUnit(s + 19),
                    alpha = seededRange(s + 23, 0.25f, 0.7f),
                )
            }
        }.flatten()
    }

    val path = remember { Path() }
    val transition = rememberInfiniteTransition(label = "silk_bloom")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((26000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "silk_bloom_t",
    )

    val dim = if (isActive) 1f else 0.55f

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val minDim = w.coerceAtMost(h)
        val time = phase01(t)
        val spin = time * 2f * PI.toFloat() * 0.28f
        // Center-weighted like the watch face (clock sits in lower third).
        val origin = Offset(w * 0.5f, h * 0.48f)

        drawRect(Color(0xFF000000))

        petals.forEach { petal ->
            val open = 0.35f + 0.65f * sin01(time * 2f * PI.toFloat() * petal.openSpeed + petal.openPhase)
            val angle = petal.angle0 + spin * petal.spinMul + petal.twist * (1.15f - open) * 0.55f
            val len = minDim * petal.lengthFrac * (0.7f + 0.3f * open)
            val wid = minDim * petal.widthFrac * (0.55f + 0.45f * open)

            val (cA, cB, cC) = petalColors(petal, paletteColors, brightness)

            withTransform({
                translate(left = origin.x, top = origin.y)
                rotate(degrees = Math.toDegrees(angle.toDouble()).toFloat(), pivot = Offset.Zero)
            }) {
                path.reset()
                buildRibbonPetal(path, len, wid, petal.twist, petal.bend)
                drawPath(
                    path = path,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            TonalPalette.withAlpha(cA, petal.alpha * dim),
                            TonalPalette.withAlpha(cB, petal.alpha * 0.95f * dim),
                            TonalPalette.withAlpha(cC, petal.alpha * 0.72f * dim),
                        ),
                        start = Offset(-wid * 0.35f, len * 0.15f),
                        end = Offset(wid * 0.4f, -len * 0.85f),
                    ),
                )
                // Soft rim highlight (velvety / frosted edge).
                path.reset()
                buildRibbonPetal(path, len * 0.92f, wid * 0.78f, petal.twist * 0.85f, petal.bend)
                drawPath(
                    path = path,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            TonalPalette.withAlpha(Color.White, 0.14f * dim),
                            TonalPalette.withAlpha(cC, 0.08f * dim),
                            Color.Transparent,
                        ),
                        start = Offset(-wid * 0.2f, 0f),
                        end = Offset(wid * 0.35f, -len * 0.9f),
                    ),
                )
                drawPetalGrain(
                    length = len,
                    width = wid,
                    twist = petal.twist,
                    bend = petal.bend,
                    seed = petal.grainSeed,
                    count = grainPerPetal,
                    light = TonalPalette.withAlpha(
                        TonalPalette.mix(cC, Color.White, 0.65f),
                        if (petal.misty) 0.28f * dim else 0.16f * dim,
                    ),
                    shade = TonalPalette.withAlpha(
                        TonalPalette.mix(cA, Color(0xFF2A1C18), 0.35f),
                        0.10f * dim,
                    ),
                )
            }
        }

        // Dew / vapor spray — dense on pale translucent sheets (watch-face left mist).
        mist.forEach { m ->
            val petal = petals.getOrNull(m.petalIndex) ?: return@forEach
            val open = 0.35f + 0.65f * sin01(time * 2f * PI.toFloat() * petal.openSpeed + petal.openPhase)
            val angle = petal.angle0 + spin * petal.spinMul + petal.twist * (1.15f - open) * 0.55f
            val len = minDim * petal.lengthFrac * (0.7f + 0.3f * open)
            val wid = minDim * petal.widthFrac * (0.55f + 0.45f * open)
            val life = phase01(m.phase + time * m.drift)
            val along = m.along
            val side = m.side + sin(life * 2f * PI.toFloat() + m.phase * 8f) * 0.12f
            val local = ribbonPoint(along, side, len, wid, petal.twist, petal.bend)
            val driftOut = life * minDim * 0.06f
            val cosA = cos(angle)
            val sinA = sin(angle)
            val lx = local.x + driftOut * side * 0.35f
            val ly = local.y - driftOut * 0.55f
            val px = origin.x + lx * cosA - ly * sinA
            val py = origin.y + lx * sinA + ly * cosA
            val fade = (1f - life * 0.55f).coerceIn(0.2f, 1f) * open
            val mistColor = TonalPalette.mix(Color(0xFFE8EEF8), Color(0xFFB8C4DC), 0.35f)
            drawCircle(
                color = TonalPalette.withAlpha(mistColor, m.alpha * fade * dim),
                radius = m.size * minDim * 0.0024f,
                center = Offset(px, py),
            )
        }
    }
}

/** Broad curling ribbon / calla sheet — not a classic pointed petal. */
private fun buildRibbonPetal(path: Path, length: Float, width: Float, twist: Float, bend: Float) {
    val half = width * 0.5f
    val lean = twist * half * 0.7f
    val arch = bend * length * 0.22f
    // Root near origin, body arcs outward then folds back.
    path.moveTo(-half * 0.15f, length * 0.12f)
    path.cubicTo(
        -half * 0.95f - lean * 0.2f, length * 0.05f - arch * 0.2f,
        -half * 1.15f - lean, -length * 0.35f - arch * 0.3f,
        -half * 0.25f + lean * 0.4f, -length * 0.92f,
    )
    path.cubicTo(
        half * 0.15f + lean * 0.6f, -length * 1.02f,
        half * 1.1f + lean * 0.35f, -length * 0.45f + arch * 0.25f,
        half * 0.85f + lean * 0.15f, -length * 0.05f,
    )
    path.cubicTo(
        half * 0.45f, length * 0.18f,
        half * 0.05f, length * 0.22f,
        -half * 0.15f, length * 0.12f,
    )
    path.close()
}

private fun ribbonPoint(
    along: Float,
    side: Float,
    length: Float,
    width: Float,
    twist: Float,
    bend: Float,
): Offset {
    val half = width * 0.5f
    val lean = twist * half * 0.55f * along
    val arch = bend * length * 0.18f * sin(along * PI.toFloat())
    val x = side * half * (0.35f + 0.65f * along) * (1f - along * 0.15f) + lean
    val y = length * 0.1f - along * length * 1.05f - arch
    return Offset(x, y)
}

private fun DrawScope.drawPetalGrain(
    length: Float,
    width: Float,
    twist: Float,
    bend: Float,
    seed: Int,
    count: Int,
    light: Color,
    shade: Color,
) {
    for (i in 0 until count) {
        val along = 0.05f + seededUnit(seed + i * 17) * 0.9f
        val side = (seededUnit(seed + i * 29 + 3) * 2f - 1f)
        val p = ribbonPoint(along, side, length, width, twist, bend)
        val r = (0.5f + seededUnit(seed + i * 41) * 1.6f) * length * 0.0038f
        drawCircle(
            color = if (i % 4 == 0) shade else light,
            radius = r,
            center = p,
        )
    }
}

private fun petalColors(
    petal: SilkBloomPetal,
    palette: List<Color>,
    brightness: Float,
): Triple<Color, Color, Color> {
    val accents = when (petal.tone) {
        0 -> Triple(Color(0xFFE8876A), Color(0xFFF0A888), Color(0xFFF5C4B0)) // peach/coral
        1 -> Triple(Color(0xFF8FB892), Color(0xFFA8C9A4), Color(0xFFC5DCC4)) // mint
        else -> Triple(Color(0xFFC8C0D0), Color(0xFFE0D8E4), Color(0xFFF0ECF4)) // cream/lavender
    }
    val p = TonalPalette.pick(palette, petal.tone)
    fun mix(c: Color) = TonalPalette.brightness(TonalPalette.mix(c, p, 0.22f), brightness)
    return Triple(
        mix(TonalPalette.mix(accents.first, accents.second, petal.colorMix)),
        mix(accents.second),
        mix(accents.third),
    )
}

private data class SilkBloomPetal(
    val angle0: Float,
    val lengthFrac: Float,
    val widthFrac: Float,
    val twist: Float,
    val bend: Float,
    val openPhase: Float,
    val openSpeed: Float,
    val spinMul: Float,
    val tone: Int,
    val colorMix: Float,
    val alpha: Float,
    val misty: Boolean,
    val layer: Float,
    val grainSeed: Int,
)

private data class SilkBloomMist(
    val petalIndex: Int,
    val along: Float,
    val side: Float,
    val size: Float,
    val drift: Float,
    val phase: Float,
    val alpha: Float,
)

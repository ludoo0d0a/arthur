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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import fr.geoking.arthur.genart.GenartQuality
import fr.geoking.arthur.genart.TonalPalette
import fr.geoking.arthur.genart.phase01
import fr.geoking.arthur.genart.qualityCount
import fr.geoking.arthur.genart.seededRange
import fr.geoking.arthur.genart.seededUnit
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Drifting wireframe polygons on a dark field — calm vector rocks.
 */
@Composable
internal fun VectorRocksEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val rockCount = qualityCount(quality, low = 5, medium = 8, high = 12)
    val rocks = remember(rockCount) {
        List(rockCount) { i ->
            val sides = 5 + (seededUnit(i * 17 + 3) * 5f).toInt().coerceIn(0, 5)
            VectorRockSeed(
                x0 = seededUnit(i * 29 + 7),
                y0 = seededUnit(i * 41 + 11),
                vx = seededRange(i * 53 + 13, -0.35f, 0.35f),
                vy = seededRange(i * 67 + 19, -0.3f, 0.3f),
                radius = seededRange(i * 79 + 23, 0.04f, 0.11f),
                sides = sides,
                spin = seededRange(i * 89 + 29, 0.1f, 0.45f),
                spinPhase = seededUnit(i * 97 + 31),
                jagged = List(sides) { s -> seededRange(i * 101 + s * 7 + 3, 0.65f, 1.15f) },
                colorIndex = i,
            )
        }
    }

    val transition = rememberInfiniteTransition(label = "vectorrocks")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((36000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "vectorrocks_t",
    )

    val dim = if (isActive) 1f else 0.55f
    val wire = TonalPalette.mix(Color(0xFFE8F0FF), TonalPalette.pick(paletteColors, 0), 0.2f)

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val minDim = minOf(w, h)
        drawRect(color = TonalPalette.brightness(Color(0xFF05070C), brightness * dim))

        val time = phase01(t)
        rocks.forEach { rock ->
            val cx = ((rock.x0 + rock.vx * time) % 1.3f + 1.3f) % 1.3f
            val cy = ((rock.y0 + rock.vy * time) % 1.3f + 1.3f) % 1.3f
            val x = (cx - 0.15f) * w
            val y = (cy - 0.15f) * h
            val angle = time * rock.spin * 2f * PI.toFloat() + rock.spinPhase * 2f * PI.toFloat()
            val r = minDim * rock.radius
            val tint = TonalPalette.brightness(
                TonalPalette.mix(wire, TonalPalette.pick(paletteColors, rock.colorIndex), 0.25f),
                brightness * dim,
            )
            val path = Path()
            for (s in 0..rock.sides) {
                val a = angle + s * 2f * PI.toFloat() / rock.sides
                val jr = r * rock.jagged[s % rock.jagged.size]
                val px = x + cos(a) * jr
                val py = y + sin(a) * jr
                if (s == 0) path.moveTo(px, py) else path.lineTo(px, py)
            }
            path.close()
            drawPath(
                path = path,
                color = TonalPalette.withAlpha(tint, 0.7f * dim),
                style = Stroke(width = 1.6f, cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
            // Soft inner accent
            drawCircle(
                color = TonalPalette.withAlpha(tint, 0.08f * dim),
                radius = r * 0.35f,
                center = Offset(x, y),
            )
        }
    }
}

private data class VectorRockSeed(
    val x0: Float,
    val y0: Float,
    val vx: Float,
    val vy: Float,
    val radius: Float,
    val sides: Int,
    val spin: Float,
    val spinPhase: Float,
    val jagged: List<Float>,
    val colorIndex: Int,
)

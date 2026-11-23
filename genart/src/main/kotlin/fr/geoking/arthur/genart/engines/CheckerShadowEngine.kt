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
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import fr.geoking.arthur.genart.GenartQuality
import fr.geoking.arthur.genart.TonalPalette
import fr.geoking.arthur.genart.phase01
import fr.geoking.arthur.genart.qualityCount
import fr.geoking.arthur.genart.sin01
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.min

/**
 * Adelson checker-shadow — checkerboard + soft cylinder cast-shadow so two same-
 * luminance squares (A in light / B in shade) look different; gentle shadow drift.
 */
@Composable
internal fun CheckerShadowEngine(
    isActive: Boolean,
    paletteColors: List<Color>,
    quality: GenartQuality,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val grid = qualityCount(quality, low = 7, medium = 9, high = 11)
    val transition = rememberInfiniteTransition(label = "checker_shadow")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((46000 / speed.coerceAtLeast(0.2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "checker_shadow_t",
    )
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val minDim = min(w, h)
        drawRect(color = TonalPalette.brightness(Color(0xFF1A1C22), brightness * 0.4f))
        val time = phase01(t) * 2f * PI.toFloat()
        val dim = if (isActive) 1f else 0.65f
        val boardLeft = w * 0.08f
        val boardTop = h * 0.12f
        val boardW = w * 0.84f
        val boardH = h * 0.76f
        val cellW = boardW / grid
        val cellH = boardH / grid
        // Identical mid-gray used for square A (dark-in-light) and B (light-in-shadow).
        val matchedGray = Color(0xFF787878)
        val lightTile = Color(0xFFE8E8E8)
        val darkTile = Color(0xFF404040)
        val accentDark = TonalPalette.mix(darkTile, TonalPalette.pick(paletteColors, 3), 0.1f)
        // Cylinder sits on the right; shadow falls leftward across the board.
        val cylCx = boardLeft + boardW * (0.72f + 0.04f * (sin01(time) * 2f - 1f))
        val cylTop = boardTop + boardH * 0.08f
        val cylBottom = boardTop + boardH * 0.92f
        val cylR = minDim * 0.09f
        val shadowCx = cylCx - boardW * 0.22f + boardW * 0.03f * (sin01(time + 0.7f) * 2f - 1f)
        val shadowCy = boardTop + boardH * 0.5f
        val shadowRx = boardW * 0.28f
        val shadowRy = boardH * 0.42f

        // A / B cell indices (classic Adelson positions, scaled to grid)
        val aCol = (grid * 0.28f).toInt().coerceIn(1, grid - 2)
        val aRow = (grid * 0.42f).toInt().coerceIn(1, grid - 2)
        val bCol = (grid * 0.55f).toInt().coerceIn(1, grid - 2)
        val bRow = aRow

        for (row in 0 until grid) {
            for (col in 0 until grid) {
                val x = boardLeft + col * cellW
                val y = boardTop + row * cellH
                val cx = x + cellW * 0.5f
                val cy = y + cellH * 0.5f
                val nx = (cx - shadowCx) / shadowRx
                val ny = (cy - shadowCy) / shadowRy
                val shadowT = exp(-0.5f * (nx * nx + ny * ny)).coerceIn(0f, 1f)
                val isLightChecker = (row + col) % 2 == 0
                val isA = col == aCol && row == aRow
                val isB = col == bCol && row == bRow
                val base = when {
                    isA || isB -> matchedGray
                    isLightChecker -> lightTile
                    else -> accentDark
                }
                val shaded = when {
                    isA || isB -> matchedGray // keep identical luminance
                    else -> TonalPalette.mix(base, Color(0xFF2A2A2A), shadowT * 0.55f)
                }
                drawRect(
                    color = TonalPalette.withAlpha(shaded, 0.96f * dim),
                    topLeft = Offset(x, y),
                    size = Size(cellW + 0.5f, cellH + 0.5f),
                )
            }
        }

        // Soft elliptical cast shadow overlay (doesn't alter A/B cells' painted match)
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    TonalPalette.withAlpha(Color(0xFF1A1520), 0.42f * dim),
                    TonalPalette.withAlpha(Color(0xFF1A1520), 0.18f * dim),
                    Color.Transparent,
                ),
                center = Offset(shadowCx, shadowCy),
                radius = maxOf(shadowRx, shadowRy),
            ),
            topLeft = Offset(boardLeft, boardTop),
            size = Size(boardW, boardH),
        )

        // Re-paint A and B on top so the overlay cannot break the match.
        fun cellRect(col: Int, row: Int): Pair<Offset, Size> {
            val x = boardLeft + col * cellW
            val y = boardTop + row * cellH
            return Offset(x, y) to Size(cellW + 0.5f, cellH + 0.5f)
        }
        val (aPos, aSize) = cellRect(aCol, aRow)
        val (bPos, bSize) = cellRect(bCol, bRow)
        drawRect(color = TonalPalette.withAlpha(matchedGray, 0.96f * dim), topLeft = aPos, size = aSize)
        drawRect(color = TonalPalette.withAlpha(matchedGray, 0.96f * dim), topLeft = bPos, size = bSize)

        // Cylinder (soft vertical shading)
        val cylLeft = cylCx - cylR
        val cylPath = Path().apply {
            addOval(androidx.compose.ui.geometry.Rect(cylLeft, cylTop, cylCx + cylR, cylTop + cylR * 0.55f))
        }
        drawPath(
            path = cylPath,
            brush = Brush.horizontalGradient(
                colors = listOf(
                    TonalPalette.withAlpha(Color(0xFF4A5568), 0.55f * dim),
                    TonalPalette.withAlpha(Color(0xFFCBD5E1), 0.85f * dim),
                    TonalPalette.withAlpha(Color(0xFF334155), 0.7f * dim),
                ),
                startX = cylLeft,
                endX = cylCx + cylR,
            ),
        )
        drawRect(
            brush = Brush.horizontalGradient(
                colors = listOf(
                    TonalPalette.withAlpha(Color(0xFF3B4554), 0.75f * dim),
                    TonalPalette.withAlpha(Color(0xFF94A3B8), 0.9f * dim),
                    TonalPalette.withAlpha(Color(0xFF1E293B), 0.85f * dim),
                ),
                startX = cylLeft,
                endX = cylCx + cylR,
            ),
            topLeft = Offset(cylLeft, cylTop + cylR * 0.28f),
            size = Size(cylR * 2f, (cylBottom - cylTop) - cylR * 0.55f),
        )
        // Soft contact shadow under cylinder
        drawOval(
            brush = Brush.radialGradient(
                colors = listOf(
                    TonalPalette.withAlpha(Color.Black, 0.35f * dim),
                    Color.Transparent,
                ),
                center = Offset(cylCx - cylR * 0.15f, cylBottom),
                radius = cylR * 1.6f,
            ),
            topLeft = Offset(cylCx - cylR * 1.4f, cylBottom - cylR * 0.25f),
            size = Size(cylR * 2.6f, cylR * 0.55f),
        )
    }
}

package fr.geoking.arthur.genart.stills

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import fr.geoking.arthur.genart.AnimationPalette
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

/** Bakes one frozen frame of Nested Pack for Auto/Ambient album art. */
internal object NestedPackStill {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)
    private val rect = RectF()
    private val path = Path()
    private val shadowPath = Path()

    fun draw(
        canvas: Canvas,
        size: Int,
        generation: Long,
        phase: Float,
        rotationDeg: Float,
        pulse: Float,
        palette: AnimationPalette,
    ) {
        val rnd = Random(generation)
        val w = size.toFloat()
        val h = size.toFloat()
        val minDim = min(w, h)
        val gap = palette.colorAt(2)
        paint.shader = LinearGradient(
            0f, 0f, w, h,
            intArrayOf(
                Color.argb(
                    255,
                    (Color.red(gap) * 0.4f).toInt(),
                    (Color.green(gap) * 0.4f).toInt(),
                    (Color.blue(gap) * 0.4f).toInt(),
                ),
                0xFF08060C.toInt(),
            ),
            null,
            Shader.TileMode.CLAMP,
        )
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        val time = ((phase / (2f * PI.toFloat())) + rotationDeg / 360f)
        val loop = ((time % 1f) + 1f) % 1f
        val dim = 0.65f + 0.35f * pulse
        val cols = 9
        val rows = 13
        val cellW = w / (cols - 0.35f)
        val cellH = h / (rows - 0.35f)
        val shadowPx = minDim * 0.012f
        val pieces = (0 until cols * rows).map { i ->
            NestedPackStillPiece(
                col = i % cols,
                row = i / cols,
                kind = rnd.nextInt(7),
                rot = (rnd.nextFloat() - 0.5f) * 0.7f,
                sizeJitter = 0.88f + rnd.nextFloat() * 0.3f,
                aspect = 0.55f + rnd.nextFloat() * 1.0f,
                corner = 0.22f + rnd.nextFloat() * 0.28f,
                colorMix = rnd.nextFloat(),
                breathPhase = rnd.nextFloat() * 2f * PI.toFloat(),
                breathAmp = 0.012f + rnd.nextFloat() * 0.033f,
                skewA = (rnd.nextFloat() - 0.5f) * 0.44f,
                skewB = (rnd.nextFloat() - 0.5f) * 0.44f,
                tetrisVariant = rnd.nextInt(4),
                alpha = 0.82f + rnd.nextFloat() * 0.18f,
            )
        }.sortedBy { it.row * cols + it.col }

        pieces.forEach { piece ->
            val stagger = if (piece.row % 2 == 0) 0f else cellW * 0.5f
            val cx = piece.col * cellW + stagger + cellW * 0.15f
            val cy = piece.row * cellH * 0.62f + cellH * 0.35f
            val breath = 1f + piece.breathAmp * sin(loop * 2f * PI.toFloat() + piece.breathPhase)
            val bw = cellW * 0.92f * piece.sizeJitter * piece.aspect.coerceIn(0.7f, 1.4f) * breath
            val bh = cellH * 0.95f * piece.sizeJitter * breath
            val bandT = (cx / w).coerceIn(0f, 1f)
            val colorIndex = (bandT * 6f).toInt()
            val tint = mixArgb(
                palette.colorAt(colorIndex),
                palette.colorAt(colorIndex + 1),
                piece.colorMix * 0.45f + bandT * 0.2f,
            )
            val highlight = mixArgb(tint, Color.WHITE, 0.18f)
            val shade = mixArgb(tint, Color.BLACK, 0.22f)
            canvas.save()
            canvas.rotate(Math.toDegrees(piece.rot.toDouble()).toFloat(), cx, cy)
            drawStillShape(
                canvas = canvas,
                kind = piece.kind,
                cx = cx,
                cy = cy,
                bw = bw,
                bh = bh,
                corner = piece.corner,
                skewA = piece.skewA,
                skewB = piece.skewB,
                tetrisVariant = piece.tetrisVariant,
                tint = tint,
                highlight = highlight,
                shade = shade,
                alpha = (piece.alpha * dim * 255).toInt().coerceIn(0, 255),
                shadowAlpha = (0.34f * dim * 255).toInt().coerceIn(0, 255),
                shadowPx = shadowPx,
            )
            canvas.restore()
        }
    }

    private fun drawStillShape(
        canvas: Canvas,
        kind: Int,
        cx: Float,
        cy: Float,
        bw: Float,
        bh: Float,
        corner: Float,
        skewA: Float,
        skewB: Float,
        tetrisVariant: Int,
        tint: Int,
        highlight: Int,
        shade: Int,
        alpha: Int,
        shadowAlpha: Int,
        shadowPx: Float,
    ) {
        when (kind) {
            0 -> {
                paint.color = Color.argb(shadowAlpha, 0, 0, 0)
                rect.set(cx - bw * 0.5f + shadowPx * 0.4f, cy - bh * 0.5f + shadowPx, cx + bw * 0.5f + shadowPx * 0.4f, cy + bh * 0.5f + shadowPx)
                canvas.drawOval(rect, paint)
                paint.shader = LinearGradient(
                    cx - bw * 0.35f, cy - bh * 0.4f, cx + bw * 0.3f, cy + bh * 0.4f,
                    intArrayOf(highlight, tint, shade), null, Shader.TileMode.CLAMP,
                )
                paint.alpha = alpha
                rect.set(cx - bw * 0.5f, cy - bh * 0.5f, cx + bw * 0.5f, cy + bh * 0.5f)
                canvas.drawOval(rect, paint)
            }
            1 -> {
                paint.color = Color.argb(shadowAlpha, 0, 0, 0)
                rect.set(cx - bw * 0.5f + shadowPx * 0.4f, cy - bh * 0.5f + shadowPx, cx + bw * 0.5f + shadowPx * 0.4f, cy + bh * 0.5f + shadowPx)
                canvas.drawRoundRect(rect, bh * 0.5f, bh * 0.5f, paint)
                paint.shader = LinearGradient(
                    cx - bw * 0.4f, cy - bh * 0.4f, cx + bw * 0.35f, cy + bh * 0.4f,
                    intArrayOf(highlight, tint, shade), null, Shader.TileMode.CLAMP,
                )
                paint.alpha = alpha
                rect.set(cx - bw * 0.5f, cy - bh * 0.5f, cx + bw * 0.5f, cy + bh * 0.5f)
                canvas.drawRoundRect(rect, bh * 0.5f, bh * 0.5f, paint)
            }
            2 -> {
                buildTeardrop(path, cx, cy, bw, bh)
                paint.color = Color.argb(shadowAlpha, 0, 0, 0)
                shadowPath.set(path)
                shadowPath.offset(shadowPx * 0.4f, shadowPx)
                canvas.drawPath(shadowPath, paint)
                paint.shader = LinearGradient(
                    cx - bw * 0.25f, cy - bh * 0.45f, cx + bw * 0.25f, cy + bh * 0.4f,
                    intArrayOf(highlight, tint, shade), null, Shader.TileMode.CLAMP,
                )
                paint.alpha = alpha
                canvas.drawPath(path, paint)
            }
            3 -> {
                path.reset()
                path.moveTo(cx + bw * skewA * 0.3f, cy - bh * 0.52f)
                path.lineTo(cx + bw * (0.52f + skewB * 0.2f), cy + bh * (0.42f + skewA * 0.15f))
                path.lineTo(cx - bw * (0.48f + skewA * 0.2f), cy + bh * (0.38f + skewB * 0.15f))
                path.close()
                paint.color = Color.argb(shadowAlpha, 0, 0, 0)
                shadowPath.set(path)
                shadowPath.offset(shadowPx * 0.4f, shadowPx)
                canvas.drawPath(shadowPath, paint)
                paint.shader = LinearGradient(
                    cx - bw * 0.2f, cy - bh * 0.4f, cx + bw * 0.2f, cy + bh * 0.35f,
                    intArrayOf(highlight, tint, shade), null, Shader.TileMode.CLAMP,
                )
                paint.alpha = alpha
                canvas.drawPath(path, paint)
            }
            4 -> {
                path.reset()
                path.moveTo(cx - bw * 0.5f + bw * skewA * 0.15f, cy - bh * 0.5f + bh * skewB * 0.1f)
                path.lineTo(cx + bw * 0.5f + bw * skewB * 0.12f, cy - bh * 0.5f + bh * skewA * 0.08f)
                path.lineTo(cx + bw * 0.5f - bw * skewA * 0.1f, cy + bh * 0.5f + bh * skewB * 0.12f)
                path.lineTo(cx - bw * 0.5f - bw * skewB * 0.08f, cy + bh * 0.5f - bh * skewA * 0.1f)
                path.close()
                paint.color = Color.argb(shadowAlpha, 0, 0, 0)
                shadowPath.set(path)
                shadowPath.offset(shadowPx * 0.4f, shadowPx)
                canvas.drawPath(shadowPath, paint)
                paint.shader = LinearGradient(
                    cx - bw * 0.35f, cy - bh * 0.35f, cx + bw * 0.3f, cy + bh * 0.35f,
                    intArrayOf(highlight, tint, shade), null, Shader.TileMode.CLAMP,
                )
                paint.alpha = alpha
                canvas.drawPath(path, paint)
            }
            5 -> {
                drawStillTetris(
                    canvas, cx, cy, bw, bh, tetrisVariant, corner,
                    tint, highlight, shade, alpha, shadowAlpha, shadowPx,
                )
            }
            else -> {
                val rx = bw * corner
                val ry = bh * corner
                paint.color = Color.argb(shadowAlpha, 0, 0, 0)
                rect.set(cx - bw * 0.5f + shadowPx * 0.4f, cy - bh * 0.5f + shadowPx, cx + bw * 0.5f + shadowPx * 0.4f, cy + bh * 0.5f + shadowPx)
                canvas.drawRoundRect(rect, rx, ry, paint)
                paint.shader = LinearGradient(
                    cx - bw * 0.4f, cy - bh * 0.4f, cx + bw * 0.35f, cy + bh * 0.4f,
                    intArrayOf(highlight, tint, shade), null, Shader.TileMode.CLAMP,
                )
                paint.alpha = alpha
                rect.set(cx - bw * 0.5f, cy - bh * 0.5f, cx + bw * 0.5f, cy + bh * 0.5f)
                canvas.drawRoundRect(rect, rx, ry, paint)
            }
        }
        paint.shader = null
        paint.alpha = 255
    }

    private fun drawStillTetris(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        bw: Float,
        bh: Float,
        variant: Int,
        corner: Float,
        tint: Int,
        highlight: Int,
        shade: Int,
        alpha: Int,
        shadowAlpha: Int,
        shadowPx: Float,
    ) {
        val cell = min(bw, bh) * 0.42f
        val cr = cell * corner.coerceIn(0.2f, 0.45f)
        val offsets = when (variant) {
            0 -> listOf(-0.55f to -0.55f, -0.55f to 0f, -0.55f to 0.55f, 0.55f to 0.55f)
            1 -> listOf(-0.55f to -0.35f, 0f to -0.35f, 0.55f to -0.35f, 0f to 0.55f)
            2 -> listOf(-0.55f to 0.3f, 0f to 0.3f, 0f to -0.4f, 0.55f to -0.4f)
            else -> listOf(-0.35f to -0.35f, 0.35f to -0.35f, -0.35f to 0.35f, 0.35f to 0.35f)
        }
        offsets.forEach { (oxFrac, oyFrac) ->
            val ox = cx + cell * oxFrac
            val oy = cy + cell * oyFrac
            paint.color = Color.argb(shadowAlpha, 0, 0, 0)
            rect.set(
                ox - cell * 0.5f + shadowPx * 0.35f,
                oy - cell * 0.5f + shadowPx,
                ox + cell * 0.5f + shadowPx * 0.35f,
                oy + cell * 0.5f + shadowPx,
            )
            canvas.drawRoundRect(rect, cr, cr, paint)
            paint.shader = LinearGradient(
                ox - cell * 0.35f, oy - cell * 0.35f, ox + cell * 0.3f, oy + cell * 0.3f,
                intArrayOf(highlight, tint, shade), null, Shader.TileMode.CLAMP,
            )
            paint.alpha = alpha
            rect.set(ox - cell * 0.5f, oy - cell * 0.5f, ox + cell * 0.5f, oy + cell * 0.5f)
            canvas.drawRoundRect(rect, cr, cr, paint)
            paint.shader = null
        }
    }

    private fun buildTeardrop(out: Path, cx: Float, cy: Float, bw: Float, bh: Float) {
        val top = cy - bh * 0.5f
        val bottom = cy + bh * 0.5f
        val left = cx - bw * 0.5f
        val right = cx + bw * 0.5f
        out.reset()
        out.moveTo(cx, top)
        out.cubicTo(right, top + bh * 0.15f, right, cy + bh * 0.15f, cx + bw * 0.08f, bottom)
        out.cubicTo(cx + bw * 0.02f, bottom + bh * 0.02f, cx - bw * 0.02f, bottom + bh * 0.02f, cx - bw * 0.08f, bottom)
        out.cubicTo(left, cy + bh * 0.15f, left, top + bh * 0.15f, cx, top)
        out.close()
    }

    private fun mixArgb(a: Int, b: Int, t: Float): Int {
        val u = t.coerceIn(0f, 1f)
        return Color.argb(
            255,
            (Color.red(a) + (Color.red(b) - Color.red(a)) * u).toInt().coerceIn(0, 255),
            (Color.green(a) + (Color.green(b) - Color.green(a)) * u).toInt().coerceIn(0, 255),
            (Color.blue(a) + (Color.blue(b) - Color.blue(a)) * u).toInt().coerceIn(0, 255),
        )
    }

    private data class NestedPackStillPiece(
        val col: Int,
        val row: Int,
        val kind: Int,
        val rot: Float,
        val sizeJitter: Float,
        val aspect: Float,
        val corner: Float,
        val colorMix: Float,
        val breathPhase: Float,
        val breathAmp: Float,
        val skewA: Float,
        val skewB: Float,
        val tetrisVariant: Int,
        val alpha: Float,
    )
}

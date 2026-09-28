package fr.geoking.arthur.genart.stills

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import fr.geoking.arthur.genart.AnimationPalette
import fr.geoking.arthur.genart.phase01
import fr.geoking.arthur.genart.seededRange
import fr.geoking.arthur.genart.seededUnit
import fr.geoking.arthur.genart.sin01
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Bakes one frozen frame of "Flower Meadow" for Android Auto album art — deterministic per [generation]. */
internal object FlowerStill {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)
    private val path = Path()

    fun draw(
        canvas: Canvas,
        size: Int,
        generation: Long,
        phase: Float,
        rotationDeg: Float,
        pulse: Float,
        palette: AnimationPalette,
    ) {
        val w = size.toFloat()
        val h = size.toFloat()
        val minDim = w.coerceAtMost(h)
        val time = phase01(phase + pulse * 0.1f + rotationDeg * 0.001f)
        val genSeed = (generation and 0x7FFFFFFF).toInt()

        // Background dark garden gradient
        paint.shader = LinearGradient(
            0f, 0f, 0f, h,
            intArrayOf(0xFF0F140D.toInt(), 0xFF172115.toInt(), 0xFF0B1209.toInt()),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP,
        )
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        val flowerCount = 10
        val seedList = List(flowerCount) { i ->
            val seed = genSeed + i * 1013
            StillFlowerSeed(
                xFrac = seededRange(seed + 3, 0.08f, 0.92f),
                yFrac = seededRange(seed + 7, 0.22f, 0.68f),
                scale = seededRange(seed + 11, 0.65f, 1.25f),
                petalCount = (5 + seededUnit(seed + 13) * 7.99f).toInt().coerceIn(5, 12),
                petalShapeType = (seededUnit(seed + 17) * 3f).toInt().coerceIn(0, 2),
                petalAspect = seededRange(seed + 19, 1.3f, 2.2f),
                stamenCount = (8 + seededUnit(seed + 23) * 10f).toInt().coerceIn(8, 18),
                colorIndex = i,
                colorMix = seededRange(seed + 31, 0.2f, 0.8f),
                openClosePhase = seededRange(seed + 37, 0f, 2f * PI.toFloat()),
                openCloseSpeed = seededRange(seed + 41, 0.7f, 1.3f),
                swayPhase = seededRange(seed + 43, 0f, 2f * PI.toFloat()),
                swayAmp = seededRange(seed + 47, 0.02f, 0.05f),
                stemCurveOffset = seededRange(seed + 53, -0.08f, 0.08f),
            )
        }.sortedBy { it.scale }

        // Soft ambient glows behind flowers
        seedList.take(4).forEach { f ->
            val glowX = f.xFrac * w
            val glowY = f.yFrac * h
            val glowR = minDim * 0.35f * f.scale
            val tint = palette.colorAt(f.colorIndex)
            paint.shader = RadialGradient(
                glowX, glowY, glowR.coerceAtLeast(1f),
                intArrayOf(
                    Color.argb(46, Color.red(tint), Color.green(tint), Color.blue(tint)),
                    Color.argb(0, Color.red(tint), Color.green(tint), Color.blue(tint)),
                ),
                floatArrayOf(0f, 1f),
                Shader.TileMode.CLAMP,
            )
            canvas.drawCircle(glowX, glowY, glowR, paint)
            paint.shader = null
        }

        // Floating ambient pollen particles
        val pollenCount = 28
        for (p in 0 until pollenCount) {
            val pSeed = genSeed + p * 733
            val x0 = seededUnit(pSeed + 5)
            val y0 = seededUnit(pSeed + 7)
            val pSize = seededRange(pSeed + 11, 1.5f, 4f)
            val speedY = seededRange(pSeed + 13, 0.05f, 0.15f)
            val swayAmp = seededRange(pSeed + 17, 0.02f, 0.06f)
            val swayFreq = seededRange(pSeed + 19, 0.3f, 1.2f)
            val alpha = seededRange(pSeed + 23, 0.25f, 0.65f)

            val py = phase01(y0 - time * speedY) * h
            val px = phase01(x0 + sin(time * 2f * PI.toFloat() * swayFreq) * swayAmp) * w
            val tint = mixColors(0xFFFFF2A8.toInt(), palette.colorAt(p % 5), 0.4f)
            paint.color = Color.argb(
                (alpha * 255).toInt().coerceIn(0, 255),
                Color.red(tint), Color.green(tint), Color.blue(tint),
            )
            paint.style = Paint.Style.FILL
            canvas.drawCircle(px, py, pSize * minDim * 0.003f, paint)
        }

        // Draw Flowers
        seedList.forEach { f ->
            val openFactor = sin01(time * 2f * PI.toFloat() * f.openCloseSpeed + f.openClosePhase)
            val bloom = 0.18f + 0.82f * openFactor

            val swayTime = time * 2f * PI.toFloat() + f.swayPhase
            val swayAngle = sin(swayTime) * f.swayAmp

            val headX = f.xFrac * w + sin(swayAngle) * (h * 0.2f)
            val headY = f.yFrac * h
            val baseX = f.xFrac * w
            val baseY = h * 1.02f

            val stemHeight = baseY - headY
            val controlX = baseX + (headX - baseX) * 0.5f + f.stemCurveOffset * w
            val controlY = headY + stemHeight * 0.55f

            // Stem
            paint.style = Paint.Style.STROKE
            paint.strokeCap = Paint.Cap.ROUND
            paint.strokeWidth = minDim * 0.008f * f.scale
            paint.color = Color.argb(217, 0x48, 0x73, 0x3B)
            path.reset()
            path.moveTo(baseX, baseY)
            path.quadTo(controlX, controlY, headX, headY)
            canvas.drawPath(path, paint)

            // Side leaves
            drawStillStemLeaves(
                canvas = canvas,
                baseX = baseX, baseY = baseY,
                controlX = controlX, controlY = controlY,
                headX = headX, headY = headY,
                scale = f.scale, minDim = minDim,
            )

            // Flower Head
            drawStillFlowerHead(
                canvas = canvas,
                headX = headX, headY = headY,
                bloom = bloom,
                f = f,
                minDim = minDim,
                palette = palette,
            )
        }

        // Base ground meadow shading
        paint.style = Paint.Style.FILL
        paint.shader = LinearGradient(
            0f, h * 0.8f, 0f, h,
            intArrayOf(0x000D170B, 0xAA0D170B.toInt(), 0xF1070D06.toInt()),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, h * 0.8f, w, h, paint)
        paint.shader = null
    }

    private fun drawStillStemLeaves(
        canvas: Canvas,
        baseX: Float, baseY: Float,
        controlX: Float, controlY: Float,
        headX: Float, headY: Float,
        scale: Float, minDim: Float,
    ) {
        val leafSize = minDim * 0.045f * scale
        paint.style = Paint.Style.FILL
        paint.color = Color.argb(191, 0x56, 0x8A, 0x47)

        // Leaf 1
        val t1 = 0.38f
        val lx1 = (1 - t1) * (1 - t1) * baseX + 2 * (1 - t1) * t1 * controlX + t1 * t1 * headX
        val ly1 = (1 - t1) * (1 - t1) * baseY + 2 * (1 - t1) * t1 * controlY + t1 * t1 * headY
        path.reset()
        path.moveTo(lx1, ly1)
        path.quadTo(lx1 - leafSize * 1.2f, ly1 - leafSize * 0.5f, lx1 - leafSize * 1.5f, ly1 - leafSize * 1.1f)
        path.quadTo(lx1 - leafSize * 0.4f, ly1 - leafSize * 1.2f, lx1, ly1)
        path.close()
        canvas.drawPath(path, paint)

        // Leaf 2
        val t2 = 0.65f
        val lx2 = (1 - t2) * (1 - t2) * baseX + 2 * (1 - t2) * t2 * controlX + t2 * t2 * headX
        val ly2 = (1 - t2) * (1 - t2) * baseY + 2 * (1 - t2) * t2 * controlY + t2 * t2 * headY
        path.reset()
        path.moveTo(lx2, ly2)
        path.quadTo(lx2 + leafSize * 1.2f, ly2 - leafSize * 0.4f, lx2 + leafSize * 1.4f, ly2 - leafSize * 1.0f)
        path.quadTo(lx2 + leafSize * 0.3f, ly2 - leafSize * 1.1f, lx2, ly2)
        path.close()
        canvas.drawPath(path, paint)
    }

    private fun drawStillFlowerHead(
        canvas: Canvas,
        headX: Float, headY: Float,
        bloom: Float,
        f: StillFlowerSeed,
        minDim: Float,
        palette: AnimationPalette,
    ) {
        val bloomRadius = minDim * 0.12f * f.scale * bloom
        val petalLen = bloomRadius * f.petalAspect
        val petalWidth = bloomRadius * (1.8f / f.petalAspect)

        val pastels = intArrayOf(
            0xFFF7C6D9.toInt(), 0xFFD8C6F7.toInt(), 0xFFF7EFC6.toInt(),
            0xFFFCA5A5.toInt(), 0xFFE9D5FF.toInt(),
        )
        val baseCol = palette.colorAt(f.colorIndex)
        val pastelAccent = pastels[f.colorIndex % pastels.size]
        val primaryPetal = mixColors(baseCol, pastelAccent, f.colorMix)
        val innerPetal = mixColors(primaryPetal, Color.WHITE, 0.35f)
        val veinColor = mixColors(primaryPetal, 0xFF3D091B.toInt(), 0.45f)

        // Petals
        for (i in 0 until f.petalCount) {
            val baseAngle = (i.toFloat() / f.petalCount) * 2f * PI.toFloat()
            val targetAngle = -PI.toFloat() * 0.5f
            val currentAngle = baseAngle * bloom + targetAngle * (1f - bloom)
            val angleDeg = (currentAngle * 180f / PI.toFloat())

            canvas.save()
            canvas.translate(headX, headY)
            canvas.rotate(angleDeg + 90f)

            // Petal path shape
            path.reset()
            buildStillPetalPath(path, petalLen, petalWidth, f.petalShapeType)

            paint.style = Paint.Style.FILL
            paint.shader = LinearGradient(
                0f, 0f, 0f, -petalLen,
                intArrayOf(
                    Color.argb(217, Color.red(primaryPetal), Color.green(primaryPetal), Color.blue(primaryPetal)),
                    Color.argb(235, Color.red(innerPetal), Color.green(innerPetal), Color.blue(innerPetal)),
                    Color.argb(153, Color.red(primaryPetal), Color.green(primaryPetal), Color.blue(primaryPetal)),
                ),
                floatArrayOf(0f, 0.5f, 1f),
                Shader.TileMode.CLAMP,
            )
            canvas.drawPath(path, paint)
            paint.shader = null

            // Petal Vein Texture
            paint.style = Paint.Style.STROKE
            paint.strokeCap = Paint.Cap.ROUND
            paint.color = Color.argb(
                ((0.35f + 0.45f * bloom) * 115).toInt().coerceIn(0, 255),
                Color.red(veinColor), Color.green(veinColor), Color.blue(veinColor),
            )
            val strokeW = petalLen * 0.025f
            val halfW = petalWidth * 0.4f

            // Center vein
            paint.strokeWidth = strokeW
            path.reset()
            path.moveTo(0f, 0f)
            path.quadTo(0f, -petalLen * 0.5f, 0f, -petalLen * 0.85f)
            canvas.drawPath(path, paint)

            // Side veins
            paint.strokeWidth = strokeW * 0.7f
            path.reset()
            path.moveTo(0f, -petalLen * 0.25f)
            path.quadTo(-halfW * 0.6f, -petalLen * 0.55f, -halfW * 0.8f, -petalLen * 0.75f)
            canvas.drawPath(path, paint)

            path.reset()
            path.moveTo(0f, -petalLen * 0.25f)
            path.quadTo(halfW * 0.6f, -petalLen * 0.55f, halfW * 0.8f, -petalLen * 0.75f)
            canvas.drawPath(path, paint)

            canvas.restore()
        }

        // Flower Center (Disk & Stamens)
        val centerRadius = minDim * 0.038f * f.scale * (0.4f + 0.6f * bloom)
        if (bloom > 0.25f) {
            val stamenBloom = ((bloom - 0.25f) / 0.75f).coerceIn(0f, 1f)
            val stamenLen = centerRadius * 1.6f * stamenBloom

            for (s in 0 until f.stamenCount) {
                val stamenAngle = (s.toFloat() / f.stamenCount) * 2f * PI.toFloat()
                val tipX = headX + cos(stamenAngle) * stamenLen
                val tipY = headY + sin(stamenAngle) * stamenLen

                paint.style = Paint.Style.STROKE
                paint.strokeWidth = minDim * 0.0025f
                paint.color = Color.argb(191, 0xFF, 0xF7, 0xED)
                canvas.drawLine(headX, headY, tipX, tipY, paint)

                paint.style = Paint.Style.FILL
                paint.color = Color.argb(230, 0xFE, 0xF0, 0x8A)
                canvas.drawCircle(tipX, tipY, minDim * 0.0055f * f.scale, paint)
            }
        }

        // Center disk glow
        paint.style = Paint.Style.FILL
        paint.shader = RadialGradient(
            headX, headY, (centerRadius * 1.2f).coerceAtLeast(1f),
            intArrayOf(0xF2F2C94C.toInt(), 0xD9D97706.toInt(), 0x00000000),
            floatArrayOf(0f, 0.7f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(headX, headY, centerRadius, paint)
        paint.shader = null
    }

    private fun buildStillPetalPath(path: Path, length: Float, width: Float, shapeType: Int) {
        val halfW = width * 0.5f
        path.moveTo(0f, 0f)
        when (shapeType) {
            1 -> {
                path.quadTo(-halfW * 1.1f, -length * 0.45f, 0f, -length)
                path.quadTo(halfW * 1.1f, -length * 0.45f, 0f, 0f)
            }
            2 -> {
                path.quadTo(-halfW * 1.2f, -length * 0.5f, -halfW * 0.7f, -length * 0.9f)
                path.quadTo(-halfW * 0.2f, -length * 1.05f, 0f, -length * 0.92f)
                path.quadTo(halfW * 0.2f, -length * 1.05f, halfW * 0.7f, -length * 0.9f)
                path.quadTo(halfW * 1.2f, -length * 0.5f, 0f, 0f)
            }
            else -> {
                path.quadTo(-halfW, -length * 0.5f, 0f, -length)
                path.quadTo(halfW, -length * 0.5f, 0f, 0f)
            }
        }
        path.close()
    }

    private fun mixColors(a: Int, b: Int, t: Float): Int {
        val u = t.coerceIn(0f, 1f)
        val r = (Color.red(a) + (Color.red(b) - Color.red(a)) * u).toInt().coerceIn(0, 255)
        val g = (Color.green(a) + (Color.green(b) - Color.green(a)) * u).toInt().coerceIn(0, 255)
        val bch = (Color.blue(a) + (Color.blue(b) - Color.blue(a)) * u).toInt().coerceIn(0, 255)
        return Color.rgb(r, g, bch)
    }
}

private data class StillFlowerSeed(
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

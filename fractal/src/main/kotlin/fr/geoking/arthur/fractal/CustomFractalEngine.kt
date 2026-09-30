package fr.geoking.arthur.fractal

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** Quality maps to recursion depth and stroke density. */
enum class CustomFractalQuality {
    Low,
    Medium,
    High,
}

data class FractalVec2(val x: Float, val y: Float) {
    operator fun plus(o: FractalVec2) = FractalVec2(x + o.x, y + o.y)
    operator fun minus(o: FractalVec2) = FractalVec2(x - o.x, y - o.y)
    operator fun times(s: Float) = FractalVec2(x * s, y * s)

    fun length(): Float = kotlin.math.sqrt(x * x + y * y)

    fun normalized(): FractalVec2 {
        val len = length().coerceAtLeast(1e-6f)
        return FractalVec2(x / len, y / len)
    }

    fun rotated(radians: Float): FractalVec2 {
        val c = cos(radians)
        val s = sin(radians)
        return FractalVec2(x * c - y * s, x * s + y * c)
    }
}

/** One luminous stroke for Compose or android.graphics bakers. */
data class FractalStroke(
    val points: List<FractalVec2>,
    val alpha: Float,
    val strokeScale: Float,
    val colorIndex: Int,
)

/**
 * Pure Kotlin Bezier / recursive-subdivision engine.
 * Deterministic: identical [CustomFractalParams] + [t] → identical strokes.
 */
object CustomFractalEngine {

    fun seed(params: CustomFractalParams): Long {
        var h = params.colorSeed.toLong() xor (params.morphMode.ordinal.toLong() * 0x9E3779B9L)
        params.points.forEachIndexed { i, p ->
            h = h xor ((p.x * 1_000_003f).toRawBits().toLong() shl (i % 16))
            h = h xor ((p.y * 1_000_033f).toRawBits().toLong() shl ((i + 8) % 24))
            h *= 0xC2B2AE3D5CC1135L
        }
        return h
    }

    fun paletteArgb(colorSeed: Int): List<Int> =
        FractalCoherentPalette.escapeArgb(colorSeed, count = 8)

    fun frame(
        params: CustomFractalParams,
        t: Float,
        quality: CustomFractalQuality = CustomFractalQuality.Medium,
    ): List<FractalStroke> {
        val p = params.normalized()
        val depth = when (quality) {
            CustomFractalQuality.Low -> 3
            CustomFractalQuality.Medium -> 4
            CustomFractalQuality.High -> 5
        }
        val samples = when (quality) {
            CustomFractalQuality.Low -> 16
            CustomFractalQuality.Medium -> 24
            CustomFractalQuality.High -> 36
        }
        val rnd = Random(seed(p))
        val anchors = morphAnchors(p, t.mod(1f), rnd)
        val strokes = mutableListOf<FractalStroke>()
        emitRecursive(
            anchors = anchors,
            depth = depth,
            samples = samples,
            alpha = 0.92f,
            strokeScale = 1f,
            colorIndex = 0,
            branchAngle = (seed(p) % 360).toFloat() * (PI.toFloat() / 180f),
            out = strokes,
            rnd = rnd,
        )
        return strokes
    }

    private fun morphAnchors(
        params: CustomFractalParams,
        t: Float,
        rnd: Random,
    ): List<FractalVec2> {
        val base = params.points.map { FractalVec2(it.x, it.y) }
        val twoPi = 2f * PI.toFloat()
        return when (params.morphMode) {
            CustomFractalMorphMode.Breathe -> base.mapIndexed { i, v ->
                val amp = 0.018f + (i % 3) * 0.006f
                val phase = t * twoPi + i * 0.7f
                FractalVec2(
                    (v.x + cos(phase) * amp).coerceIn(0.02f, 0.98f),
                    (v.y + sin(phase * 0.9f) * amp).coerceIn(0.02f, 0.98f),
                )
            }
            CustomFractalMorphMode.Orbit -> {
                val c = centroid(base)
                val angle = t * twoPi * 0.35f
                base.mapIndexed { i, v ->
                    val rel = v - c
                    val spin = rel.rotated(angle + i * 0.05f)
                    val pulse = 1f + 0.04f * sin(t * twoPi + i)
                    c + spin * pulse
                }
            }
            CustomFractalMorphMode.Unfold -> base.mapIndexed { i, v ->
                val dir = (v - centroid(base)).normalized()
                val expand = 0.5f + 0.5f * sin(t * twoPi - i * 0.4f)
                val jitter = (rnd.nextFloat() - 0.5f) * 0.002f
                FractalVec2(
                    (v.x + dir.x * expand * 0.06f + jitter).coerceIn(0.02f, 0.98f),
                    (v.y + dir.y * expand * 0.06f - jitter).coerceIn(0.02f, 0.98f),
                )
            }
        }
    }

    private fun centroid(pts: List<FractalVec2>): FractalVec2 {
        var sx = 0f
        var sy = 0f
        pts.forEach {
            sx += it.x
            sy += it.y
        }
        val n = pts.size.coerceAtLeast(1).toFloat()
        return FractalVec2(sx / n, sy / n)
    }

    private fun emitRecursive(
        anchors: List<FractalVec2>,
        depth: Int,
        samples: Int,
        alpha: Float,
        strokeScale: Float,
        colorIndex: Int,
        branchAngle: Float,
        out: MutableList<FractalStroke>,
        rnd: Random,
    ) {
        if (anchors.size < 2) return
        val curve = sampleOpenBezier(anchors, samples)
        out += FractalStroke(
            points = curve,
            alpha = alpha,
            strokeScale = strokeScale,
            colorIndex = colorIndex,
        )
        if (depth <= 0) return

        val midCount = (anchors.size - 1).coerceAtLeast(1)
        for (i in 0 until midCount) {
            val a = anchors[i]
            val b = anchors[(i + 1) % anchors.size]
            val mid = (a + b) * 0.5f
            val edge = b - a
            val len = edge.length().coerceAtLeast(1e-4f)
            val normal = FractalVec2(-edge.y / len, edge.x / len)
            val scale = 0.38f + rnd.nextFloat() * 0.12f
            val child = listOf(
                mid + normal * (0.04f * strokeScale),
                mid + edge.rotated(branchAngle) * (scale * 0.35f) + normal * (0.08f * strokeScale),
                mid + edge.rotated(-branchAngle) * (scale * 0.35f) - normal * (0.06f * strokeScale),
                mid - normal * (0.03f * strokeScale),
            )
            emitRecursive(
                anchors = child,
                depth = depth - 1,
                samples = (samples * 0.7f).toInt().coerceAtLeast(8),
                alpha = alpha * 0.72f,
                strokeScale = strokeScale * 0.65f,
                colorIndex = (colorIndex + 1 + i) % 8,
                branchAngle = branchAngle + 0.4f + i * 0.15f,
                out = out,
                rnd = rnd,
            )
        }
    }

    /** Catmull-style cubic through anchors via derived Bezier handles. */
    private fun sampleOpenBezier(anchors: List<FractalVec2>, samplesPerSeg: Int): List<FractalVec2> {
        if (anchors.size == 2) {
            return List(samplesPerSeg) { i ->
                val u = i / (samplesPerSeg - 1).coerceAtLeast(1).toFloat()
                anchors[0] * (1f - u) + anchors[1] * u
            }
        }
        val out = mutableListOf<FractalVec2>()
        for (i in 0 until anchors.lastIndex) {
            val p0 = anchors[(i - 1).coerceAtLeast(0)]
            val p1 = anchors[i]
            val p2 = anchors[i + 1]
            val p3 = anchors[(i + 2).coerceAtMost(anchors.lastIndex)]
            val c1 = p1 + (p2 - p0) * (1f / 6f)
            val c2 = p2 - (p3 - p1) * (1f / 6f)
            val segSamples = samplesPerSeg.coerceAtLeast(4)
            val start = if (i == 0) 0 else 1
            for (s in start until segSamples) {
                val u = s / (segSamples - 1).toFloat()
                out += cubicBezier(p1, c1, c2, p2, u)
            }
        }
        return out
    }

    private fun cubicBezier(
        p0: FractalVec2,
        p1: FractalVec2,
        p2: FractalVec2,
        p3: FractalVec2,
        u: Float,
    ): FractalVec2 {
        val omu = 1f - u
        val a = p0 * (omu * omu * omu)
        val b = p1 * (3f * omu * omu * u)
        val c = p2 * (3f * omu * u * u)
        val d = p3 * (u * u * u)
        return a + b + c + d
    }
}

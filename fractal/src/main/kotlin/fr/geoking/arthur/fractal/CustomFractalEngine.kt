package fr.geoking.arthur.fractal

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** Quality maps to recursion depth and stroke budget. */
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

/** Cubic Bezier segment in normalized [0,1] space. */
data class FractalCubic(
    val p0: FractalVec2,
    val c1: FractalVec2,
    val c2: FractalVec2,
    val p3: FractalVec2,
)

/** One luminous stroke for Compose or android.graphics bakers. */
data class FractalStroke(
    val cubics: List<FractalCubic>,
    val alpha: Float,
    val strokeScale: Float,
    val colorIndex: Int,
) {
    val start: FractalVec2 get() = cubics.first().p0
    val end: FractalVec2 get() = cubics.last().p3
}

/**
 * Fixed recursive branch structure (random scales) independent of morph phase [t].
 * Rebuild only when [CustomFractalParams] or quality changes.
 */
data class FractalTopology(
    val params: CustomFractalParams,
    val quality: CustomFractalQuality,
    val branchAngle0: Float,
    val root: FractalTopologyNode,
)

data class FractalTopologyNode(
    val alpha: Float,
    val strokeScale: Float,
    val colorIndex: Int,
    val branchAngle: Float,
    /** Random scale used when spawning this node from a parent edge; unused for root. */
    val spawnScale: Float,
    val children: List<FractalTopologyNode>,
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

    fun strokeBudget(quality: CustomFractalQuality): Int = when (quality) {
        CustomFractalQuality.Low -> 48
        CustomFractalQuality.Medium -> 96
        CustomFractalQuality.High -> 160
    }

    fun topology(
        params: CustomFractalParams,
        quality: CustomFractalQuality = CustomFractalQuality.Medium,
    ): FractalTopology {
        val p = params.normalized()
        val depth = depthFor(quality)
        val rnd = Random(seed(p))
        val branchAngle0 = (seed(p) % 360).toFloat() * (PI.toFloat() / 180f)
        val rootAnchors = p.points.map { FractalVec2(it.x, it.y) }
        val root = buildNode(
            anchors = rootAnchors,
            depth = depth,
            alpha = 0.92f,
            strokeScale = 1f,
            colorIndex = 0,
            branchAngle = branchAngle0,
            spawnScale = 1f,
            rnd = rnd,
            budget = strokeBudget(quality),
            used = intArrayOf(0),
        )
        return FractalTopology(
            params = p,
            quality = quality,
            branchAngle0 = branchAngle0,
            root = root,
        )
    }

    fun strokesAt(
        topology: FractalTopology,
        t: Float,
    ): List<FractalStroke> {
        val p = topology.params
        val anchors = morphAnchors(p, t.mod(1f), seed(p))
        val out = ArrayList<FractalStroke>(strokeBudget(topology.quality))
        emitFromNode(
            node = topology.root,
            anchors = anchors,
            out = out,
            budget = strokeBudget(topology.quality),
        )
        return out
    }

    fun frame(
        params: CustomFractalParams,
        t: Float,
        quality: CustomFractalQuality = CustomFractalQuality.Medium,
    ): List<FractalStroke> = strokesAt(topology(params, quality), t)

    private fun depthFor(quality: CustomFractalQuality): Int = when (quality) {
        CustomFractalQuality.Low -> 3
        CustomFractalQuality.Medium -> 4
        CustomFractalQuality.High -> 5
    }

    private fun morphAnchors(
        params: CustomFractalParams,
        t: Float,
        structureSeed: Long,
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
            CustomFractalMorphMode.Unfold -> {
                // Dedicated stream so morph jitter never steals branch-scale randomness.
                val rnd = Random(structureSeed xor 0xD1B54A32L)
                base.mapIndexed { i, v ->
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

    private fun buildNode(
        anchors: List<FractalVec2>,
        depth: Int,
        alpha: Float,
        strokeScale: Float,
        colorIndex: Int,
        branchAngle: Float,
        spawnScale: Float,
        rnd: Random,
        budget: Int,
        used: IntArray,
    ): FractalTopologyNode {
        used[0]++
        val children = mutableListOf<FractalTopologyNode>()
        if (depth > 0 && anchors.size >= 2 && used[0] < budget) {
            val midCount = (anchors.size - 1).coerceAtLeast(1)
            for (i in 0 until midCount) {
                if (used[0] >= budget) break
                val a = anchors[i]
                val b = anchors[(i + 1) % anchors.size]
                val mid = (a + b) * 0.5f
                val edge = b - a
                val len = edge.length().coerceAtLeast(1e-4f)
                val normal = FractalVec2(-edge.y / len, edge.x / len)
                val scale = 0.38f + rnd.nextFloat() * 0.12f
                val childAnchors = childAnchors(mid, edge, normal, branchAngle, strokeScale, scale)
                children += buildNode(
                    anchors = childAnchors,
                    depth = depth - 1,
                    alpha = alpha * 0.72f,
                    strokeScale = strokeScale * 0.65f,
                    colorIndex = (colorIndex + 1 + i) % 8,
                    branchAngle = branchAngle + 0.4f + i * 0.15f,
                    spawnScale = scale,
                    rnd = rnd,
                    budget = budget,
                    used = used,
                )
            }
        }
        return FractalTopologyNode(
            alpha = alpha,
            strokeScale = strokeScale,
            colorIndex = colorIndex,
            branchAngle = branchAngle,
            spawnScale = spawnScale,
            children = children,
        )
    }

    private fun emitFromNode(
        node: FractalTopologyNode,
        anchors: List<FractalVec2>,
        out: MutableList<FractalStroke>,
        budget: Int,
    ) {
        if (out.size >= budget || anchors.size < 2) return
        val cubics = openBezierCubics(anchors)
        if (cubics.isEmpty()) return
        out += FractalStroke(
            cubics = cubics,
            alpha = node.alpha,
            strokeScale = node.strokeScale,
            colorIndex = node.colorIndex,
        )
        if (node.children.isEmpty()) return
        val midCount = (anchors.size - 1).coerceAtLeast(1)
        for (i in node.children.indices) {
            if (out.size >= budget) return
            if (i >= midCount) break
            val a = anchors[i]
            val b = anchors[(i + 1) % anchors.size]
            val mid = (a + b) * 0.5f
            val edge = b - a
            val len = edge.length().coerceAtLeast(1e-4f)
            val normal = FractalVec2(-edge.y / len, edge.x / len)
            val child = node.children[i]
            val childAnchors = childAnchors(
                mid = mid,
                edge = edge,
                normal = normal,
                branchAngle = node.branchAngle,
                strokeScale = node.strokeScale,
                scale = child.spawnScale,
            )
            emitFromNode(child, childAnchors, out, budget)
        }
    }

    private fun childAnchors(
        mid: FractalVec2,
        edge: FractalVec2,
        normal: FractalVec2,
        branchAngle: Float,
        strokeScale: Float,
        scale: Float,
    ): List<FractalVec2> = listOf(
        mid + normal * (0.04f * strokeScale),
        mid + edge.rotated(branchAngle) * (scale * 0.35f) + normal * (0.08f * strokeScale),
        mid + edge.rotated(-branchAngle) * (scale * 0.35f) - normal * (0.06f * strokeScale),
        mid - normal * (0.03f * strokeScale),
    )

    /** Catmull-style open cubic chain through anchors (no polyline sampling). */
    private fun openBezierCubics(anchors: List<FractalVec2>): List<FractalCubic> {
        if (anchors.size < 2) return emptyList()
        if (anchors.size == 2) {
            val a = anchors[0]
            val b = anchors[1]
            val third = (b - a) * (1f / 3f)
            return listOf(FractalCubic(a, a + third, b - third, b))
        }
        val out = ArrayList<FractalCubic>(anchors.lastIndex)
        for (i in 0 until anchors.lastIndex) {
            val p0 = anchors[(i - 1).coerceAtLeast(0)]
            val p1 = anchors[i]
            val p2 = anchors[i + 1]
            val p3 = anchors[(i + 2).coerceAtMost(anchors.lastIndex)]
            val c1 = p1 + (p2 - p0) * (1f / 6f)
            val c2 = p2 - (p3 - p1) * (1f / 6f)
            out += FractalCubic(p1, c1, c2, p2)
        }
        return out
    }
}

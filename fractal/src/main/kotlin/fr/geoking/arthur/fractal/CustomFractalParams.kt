package fr.geoking.arthur.fractal

import kotlin.math.roundToInt

/** Normalized plot point in `[0, 1]` (Control Plane tap field). */
data class NormPoint(val x: Float, val y: Float) {
    fun clamped(): NormPoint = NormPoint(x.coerceIn(0f, 1f), y.coerceIn(0f, 1f))
}

/** Gentle Ambient morph — no sudden flashes. */
enum class CustomFractalMorphMode {
    Breathe,
    Orbit,
    Unfold,
}

/**
 * Realme AOD–style tap-authored fractal — Premium only.
 * Immutable and compactly serializable for Canvas Pairing manifests.
 */
data class CustomFractalParams(
    val points: List<NormPoint>,
    val colorSeed: Int = 0,
    val morphMode: CustomFractalMorphMode = CustomFractalMorphMode.Breathe,
    val version: Int = SCHEMA_VERSION,
) {
    fun normalized(): CustomFractalParams {
        val clamped = points.map { it.clamped() }.take(MAX_POINTS)
        require(clamped.size >= MIN_POINTS) {
            "Custom fractal needs $MIN_POINTS–$MAX_POINTS points (got ${clamped.size})"
        }
        return copy(points = clamped, version = SCHEMA_VERSION)
    }

    /** Stable Content Engine Artwork id — encodes full params (pairing-safe). */
    fun toArtworkId(): String = encode(normalized())

    fun toArtworkTitle(): String = "Custom Bezier ${points.size}p"

    companion object {
        const val SCHEMA_VERSION = 1
        const val MIN_POINTS = 3
        const val MAX_POINTS = 12
        const val ID_PREFIX = "customfractal."

        fun isCustomId(artworkId: String): Boolean = artworkId.startsWith(ID_PREFIX)

        fun fromArtworkId(artworkId: String): CustomFractalParams? {
            if (!isCustomId(artworkId)) return null
            return decode(artworkId)
        }

        fun encode(params: CustomFractalParams): String {
            val p = params.normalized()
            val pointPart = p.points.joinToString("_") { pt ->
                "${quantize(pt.x)}_${quantize(pt.y)}"
            }
            return buildString {
                append(ID_PREFIX)
                append("v").append(p.version)
                append(".c").append(p.colorSeed)
                append(".m").append(p.morphMode.ordinal)
                append(".p").append(pointPart)
            }
        }

        fun decode(encoded: String): CustomFractalParams? {
            if (!encoded.startsWith(ID_PREFIX)) return null
            val body = encoded.removePrefix(ID_PREFIX)
            val version = Regex("""^v(\d+)""").find(body)?.groupValues?.get(1)?.toIntOrNull()
                ?: return null
            if (version != SCHEMA_VERSION) return null
            val colorSeed = Regex("""\.c(-?\d+)""").find(body)?.groupValues?.get(1)?.toIntOrNull()
                ?: return null
            val modeOrdinal = Regex("""\.m(\d+)""").find(body)?.groupValues?.get(1)?.toIntOrNull()
                ?: return null
            val mode = CustomFractalMorphMode.entries.getOrNull(modeOrdinal) ?: return null
            val pointBody = Regex("""\.p(.+)$""").find(body)?.groupValues?.get(1) ?: return null
            val nums = pointBody.split('_').mapNotNull { it.toIntOrNull() }
            if (nums.size < MIN_POINTS * 2 || nums.size % 2 != 0) return null
            val points = nums.chunked(2).map { (qx, qy) ->
                NormPoint(dequantize(qx), dequantize(qy))
            }
            if (points.size !in MIN_POINTS..MAX_POINTS) return null
            return CustomFractalParams(
                points = points,
                colorSeed = colorSeed,
                morphMode = mode,
                version = version,
            )
        }

        private fun quantize(v: Float): Int =
            (v.coerceIn(0f, 1f) * QUANT).roundToInt().coerceIn(0, QUANT)

        private fun dequantize(q: Int): Float = q.coerceIn(0, QUANT) / QUANT.toFloat()

        private const val QUANT = 1000
    }
}

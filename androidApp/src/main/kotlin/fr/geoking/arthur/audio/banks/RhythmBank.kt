package fr.geoking.arthur.audio.banks

/** Rhythm token: duration in quarter-note fractions (1 = quarter). Negative = rest of that length. */
data class RhythmPattern(
    val id: String,
    val tokens: FloatArray,
) {
    override fun equals(other: Any?): Boolean =
        other is RhythmPattern && id == other.id && tokens.contentEquals(other.tokens)

    override fun hashCode(): Int = 31 * id.hashCode() + tokens.contentHashCode()
}

/** Onset cells on a swung eighth grid (true = strike, false = rest). */
data class GrooveCellPattern(
    val id: String,
    val cells: BooleanArray,
) {
    override fun equals(other: Any?): Boolean =
        other is GrooveCellPattern && id == other.id && cells.contentEquals(other.cells)

    override fun hashCode(): Int = 31 * id.hashCode() + cells.contentHashCode()
}

object RhythmBank {
    private val sparse = RhythmPattern("sparse", floatArrayOf(2f, -1f, 1f, -2f, 2f))
    private val medium = RhythmPattern("medium", floatArrayOf(1f, 1f, -0.5f, 0.5f, 1f, -1f))
    private val flowing = RhythmPattern("flow", floatArrayOf(0.5f, 0.5f, 1f, 0.5f, 0.5f, 1f))
    private val swing = RhythmPattern("swing", floatArrayOf(0.66f, 0.34f, 0.66f, 0.34f, -1f, 1f))
    private val claveSoft = RhythmPattern("clave", floatArrayOf(0.75f, -0.75f, 0.75f, 0.5f, -0.5f, 1f))
    private val freeish = RhythmPattern("free", floatArrayOf(1.5f, -0.5f, 0.75f, -1.25f, 2f))
    private val pulseSteady = RhythmPattern("pulse", floatArrayOf(0.5f, 0.5f, 0.5f, 0.5f))
    private val whole = RhythmPattern("whole", floatArrayOf(4f, -2f, 4f))

    private val all = listOf(sparse, medium, flowing, swing, claveSoft, freeish, pulseSteady, whole)

    // Swung-eighth cell grids (8 cells = 1 bar of 4/4).
    private val swingComp = GrooveCellPattern(
        "swing_comp",
        booleanArrayOf(true, false, true, true, true, false, true, false),
    )
    private val swingSparse = GrooveCellPattern(
        "swing_sparse",
        booleanArrayOf(true, false, false, true, false, false, true, false),
    )
    private val swingWalk = GrooveCellPattern(
        "swing_walk",
        booleanArrayOf(true, false, true, false, true, false, true, true),
    )

    private val jazzGroovePatterns = listOf(swingComp, swingSparse, swingWalk)

    // Straight / sparse cells for ambient piano (Zen, Classical, Ballad).
    private val straightSparse = GrooveCellPattern(
        "straight_sparse",
        booleanArrayOf(true, false, false, false, true, false, false, false),
    )
    private val balladCells = GrooveCellPattern(
        "ballad_cells",
        booleanArrayOf(true, false, false, true, false, false, true, false),
    )
    private val zenBreath = GrooveCellPattern(
        "zen_breath",
        booleanArrayOf(true, false, false, false, false, false, true, false),
    )

    private val ambientGroovePatterns = listOf(straightSparse, balladCells, zenBreath)

    // Ultra-sparse drift for Ocean / Cosmic / Atmosphere beds.
    private val oceanDrift = GrooveCellPattern(
        "ocean_drift",
        booleanArrayOf(true, false, false, false, false, false, false, false),
    )
    private val cosmicPulse = GrooveCellPattern(
        "cosmic_pulse",
        booleanArrayOf(true, false, false, false, false, false, true, false),
    )
    private val atmBreath = GrooveCellPattern(
        "atm_breath",
        booleanArrayOf(true, false, false, false, false, false, false, false),
    )

    private val textureGroovePatterns = listOf(oceanDrift, cosmicPulse, atmBreath)
    private val atmosphereGroovePatterns = listOf(atmBreath, oceanDrift)

    fun patternsFor(style: fr.geoking.arthur.audio.MusicStyle): List<RhythmPattern> =
        when (style) {
            fr.geoking.arthur.audio.MusicStyle.TibetanBowl,
            fr.geoking.arthur.audio.MusicStyle.CosmicDrone,
            fr.geoking.arthur.audio.MusicStyle.OceanWaves,
            fr.geoking.arthur.audio.MusicStyle.SoftRain,
            fr.geoking.arthur.audio.MusicStyle.WindAmbience,
            fr.geoking.arthur.audio.MusicStyle.Fireplace,
            fr.geoking.arthur.audio.MusicStyle.OrchestraPads,
            fr.geoking.arthur.audio.MusicStyle.OrchestraSwell,
            -> listOf(sparse, whole, freeish, medium)
            fr.geoking.arthur.audio.MusicStyle.Zen,
            fr.geoking.arthur.audio.MusicStyle.ClassicalPiano,
            fr.geoking.arthur.audio.MusicStyle.PianoBallad,
            fr.geoking.arthur.audio.MusicStyle.ViolinLead,
            -> listOf(flowing, medium, swing, freeish)
            // Legacy token list kept for non-groove fallback; groove path uses [groovePatternsFor].
            fr.geoking.arthur.audio.MusicStyle.JazzPiano, fr.geoking.arthur.audio.MusicStyle.BarAmbience,
            fr.geoking.arthur.audio.MusicStyle.NightLounge,
            fr.geoking.arthur.audio.MusicStyle.BassOnly,
            -> listOf(swing, swing, flowing, medium)
            fr.geoking.arthur.audio.MusicStyle.SoftGuitar,
            fr.geoking.arthur.audio.MusicStyle.RockBallad,
            fr.geoking.arthur.audio.MusicStyle.HawaiianUkulele,
            -> listOf(flowing, medium, swing, pulseSteady)
            fr.geoking.arthur.audio.MusicStyle.AfricanPulse -> listOf(claveSoft, pulseSteady, flowing, medium)
            fr.geoking.arthur.audio.MusicStyle.WindChimes,
            fr.geoking.arthur.audio.MusicStyle.Songbirds,
            -> listOf(flowing, freeish, medium, swing)
            fr.geoking.arthur.audio.MusicStyle.Chiptune,
            fr.geoking.arthur.audio.MusicStyle.ChipArp,
            fr.geoking.arthur.audio.MusicStyle.ArcadeGlow,
            -> listOf(pulseSteady, flowing, medium, swing)
        }

    /**
     * Eighth-cell patterns for groove-clock styles.
     * [atmosphere] forces ultra-sparse drift cells so beds stay calm.
     */
    fun groovePatternsFor(
        style: fr.geoking.arthur.audio.MusicStyle,
        atmosphere: Boolean = false,
    ): List<GrooveCellPattern> {
        if (atmosphere) return atmosphereGroovePatterns
        return when (style) {
            fr.geoking.arthur.audio.MusicStyle.JazzPiano,
            fr.geoking.arthur.audio.MusicStyle.BarAmbience,
            fr.geoking.arthur.audio.MusicStyle.NightLounge,
            fr.geoking.arthur.audio.MusicStyle.BassOnly,
            -> jazzGroovePatterns
            fr.geoking.arthur.audio.MusicStyle.Zen,
            fr.geoking.arthur.audio.MusicStyle.ClassicalPiano,
            fr.geoking.arthur.audio.MusicStyle.PianoBallad,
            -> ambientGroovePatterns
            fr.geoking.arthur.audio.MusicStyle.OceanWaves,
            fr.geoking.arthur.audio.MusicStyle.SoftRain,
            fr.geoking.arthur.audio.MusicStyle.WindAmbience,
            fr.geoking.arthur.audio.MusicStyle.CosmicDrone,
            fr.geoking.arthur.audio.MusicStyle.OrchestraPads,
            fr.geoking.arthur.audio.MusicStyle.OrchestraSwell,
            -> textureGroovePatterns
            else -> emptyList()
        }
    }

    /** Swing ratio for [GrooveTransport]; 0.5 = straight eighths. */
    fun grooveSwingRatio(
        style: fr.geoking.arthur.audio.MusicStyle,
        atmosphere: Boolean = false,
    ): Float {
        if (atmosphere) return 0.52f
        return when (style) {
            fr.geoking.arthur.audio.MusicStyle.JazzPiano,
            fr.geoking.arthur.audio.MusicStyle.BarAmbience,
            fr.geoking.arthur.audio.MusicStyle.NightLounge,
            fr.geoking.arthur.audio.MusicStyle.BassOnly,
            -> 0.67f
            fr.geoking.arthur.audio.MusicStyle.ClassicalPiano,
            fr.geoking.arthur.audio.MusicStyle.PianoBallad,
            -> 0.55f
            fr.geoking.arthur.audio.MusicStyle.OceanWaves,
            fr.geoking.arthur.audio.MusicStyle.SoftRain,
            fr.geoking.arthur.audio.MusicStyle.WindAmbience,
            -> 0.54f
            fr.geoking.arthur.audio.MusicStyle.CosmicDrone,
            fr.geoking.arthur.audio.MusicStyle.OrchestraPads,
            fr.geoking.arthur.audio.MusicStyle.OrchestraSwell,
            -> 0.52f
            fr.geoking.arthur.audio.MusicStyle.Zen -> 0.50f
            else -> 0.50f
        }
    }

    fun pickGroove(style: fr.geoking.arthur.audio.MusicStyle, index: Int): GrooveCellPattern? {
        val list = groovePatternsFor(style)
        if (list.isEmpty()) return null
        return list[Math.floorMod(index, list.size)]
    }

    fun pick(style: fr.geoking.arthur.audio.MusicStyle, index: Int): RhythmPattern {
        val list = patternsFor(style)
        return list[Math.floorMod(index, list.size)]
    }

    fun allPatterns(): List<RhythmPattern> = all
}

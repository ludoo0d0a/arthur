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
            fr.geoking.arthur.audio.MusicStyle.JazzPiano, fr.geoking.arthur.audio.MusicStyle.BarAmbience,
            fr.geoking.arthur.audio.MusicStyle.NightLounge,
            fr.geoking.arthur.audio.MusicStyle.BassOnly,
            -> listOf(flowing, swing, medium, pulseSteady)
            fr.geoking.arthur.audio.MusicStyle.SoftGuitar,
            fr.geoking.arthur.audio.MusicStyle.RockBallad,
            -> listOf(flowing, medium, swing, pulseSteady)
            fr.geoking.arthur.audio.MusicStyle.AfricanPulse -> listOf(claveSoft, pulseSteady, flowing, medium)
            fr.geoking.arthur.audio.MusicStyle.WindChimes,
            fr.geoking.arthur.audio.MusicStyle.Songbirds,
            -> listOf(flowing, freeish, medium, swing)
        }

    fun pick(style: fr.geoking.arthur.audio.MusicStyle, index: Int): RhythmPattern {
        val list = patternsFor(style)
        return list[Math.floorMod(index, list.size)]
    }

    fun allPatterns(): List<RhythmPattern> = all
}

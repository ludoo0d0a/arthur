package fr.geoking.arthur.audio.banks

/** Scale-degree motif cell (relative to current chord/root). Rest = Int.MIN_VALUE. */
data class MelodyMotif(
    val id: String,
    /** Scale degree indices into the preset scale (0-based); [REST] for silence. */
    val degrees: IntArray,
) {
    companion object {
        const val REST = Int.MIN_VALUE
    }

    override fun equals(other: Any?): Boolean =
        other is MelodyMotif && id == other.id && degrees.contentEquals(other.degrees)

    override fun hashCode(): Int = 31 * id.hashCode() + degrees.contentHashCode()
}

object MelodyBank {
    private val R = MelodyMotif.REST

    // Longer phrase motifs with rests and octave jumps (degree + scale length wraps in sequencer).
    private val ascendingPhrase = MelodyMotif("asc_ph", intArrayOf(0, 1, 2, 3, 4, R, 2))
    private val descendingSigh = MelodyMotif("sigh", intArrayOf(5, 4, 3, 2, 1, 0))
    private val neighborWalk = MelodyMotif("neighbor", intArrayOf(2, 3, 2, 1, 2, 0))
    private val leapReturn = MelodyMotif("leap", intArrayOf(0, 4, 2, 5, 3, 1, 0))
    private val pedalReturn = MelodyMotif("pedal", intArrayOf(0, 2, 0, 3, 0, 4, 0))
    private val callHigh = MelodyMotif("call", intArrayOf(3, 4, 5, 4, R))
    private val responseLow = MelodyMotif("resp", intArrayOf(2, 1, 0, R, 0))
    private val question = MelodyMotif("q", intArrayOf(1, 2, 4, 5, R))
    private val answer = MelodyMotif("a", intArrayOf(4, 3, 1, 0))
    private val arcade = MelodyMotif("arcade", intArrayOf(0, 2, 4, 2, 5, 4, 2, 0))
    private val waltz = MelodyMotif("waltz", intArrayOf(0, 2, 4, R, 4, 2, 0, R))
    private val bounce = MelodyMotif("bounce", intArrayOf(0, 4, 0, 5, 2, 4, 0))
    private val lullaby = MelodyMotif("lull", intArrayOf(4, 2, 0, 2, 4, 5, 4, R, 2, 0))
    private val stepClimb = MelodyMotif("climb", intArrayOf(0, 1, 2, 1, 2, 3, 2, 3, 4))
    private val echoPair = MelodyMotif("echo", intArrayOf(0, 2, R, 0, 2, 4, R, 4, 2, 0))
    private val africanRoll = MelodyMotif("afr", intArrayOf(0, 2, 3, 2, 5, 3, 2, 0))
    private val wholeDrift = MelodyMotif("drift", intArrayOf(0, 1, 2, 3, 2, 1, 0, R))
    private val longTone = MelodyMotif("long", intArrayOf(0, R, 0, R, 2))
    private val chimeLeap = MelodyMotif("chime", intArrayOf(0, 4, R, 5, 2, R, 4, 0))

    private val zen = listOf(
        ascendingPhrase, descendingSigh, neighborWalk, pedalReturn, lullaby, stepClimb, echoPair,
    )
    private val jazz = listOf(
        neighborWalk, leapReturn, question, answer, descendingSigh, arcade, bounce, waltz, echoPair,
    )
    private val guitar = listOf(
        ascendingPhrase, pedalReturn, leapReturn, callHigh, responseLow, stepClimb, lullaby,
    )
    private val african = listOf(africanRoll, callHigh, responseLow, ascendingPhrase, pedalReturn, bounce)
    private val cosmic = listOf(wholeDrift, longTone, descendingSigh, leapReturn, lullaby)
    private val bowl = listOf(longTone, pedalReturn, descendingSigh, echoPair)
    private val ocean = listOf(longTone, wholeDrift, ascendingPhrase, lullaby)
    private val chimes = listOf(chimeLeap, callHigh, neighborWalk, wholeDrift, leapReturn)

    fun motifsFor(style: fr.geoking.arthur.audio.MusicStyle): List<MelodyMotif> =
        when (style) {
            fr.geoking.arthur.audio.MusicStyle.Zen -> zen
            fr.geoking.arthur.audio.MusicStyle.JazzPiano, fr.geoking.arthur.audio.MusicStyle.BarAmbience,
            fr.geoking.arthur.audio.MusicStyle.NightLounge,
            -> jazz
            fr.geoking.arthur.audio.MusicStyle.SoftGuitar -> guitar
            fr.geoking.arthur.audio.MusicStyle.AfricanPulse -> african
            fr.geoking.arthur.audio.MusicStyle.CosmicDrone, fr.geoking.arthur.audio.MusicStyle.TibetanBowl ->
                if (style == fr.geoking.arthur.audio.MusicStyle.TibetanBowl) bowl else cosmic
            fr.geoking.arthur.audio.MusicStyle.OceanWaves -> ocean
            fr.geoking.arthur.audio.MusicStyle.WindChimes -> chimes
        }

    fun pick(style: fr.geoking.arthur.audio.MusicStyle, index: Int): MelodyMotif {
        val list = motifsFor(style)
        return list[Math.floorMod(index, list.size)]
    }
}

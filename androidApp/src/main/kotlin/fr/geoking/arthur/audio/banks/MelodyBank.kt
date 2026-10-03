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

    // Longer phrase motifs (8–16 degrees) with rests — original shapes, genre-inspired.
    // Motif degrees stay ≤4 so phrases sit in a warm mid register over the root.
    private val ascendingPhrase = MelodyMotif("asc_ph", intArrayOf(0, 1, 2, 3, 4, R, 2, 0))
    private val descendingSigh = MelodyMotif("sigh", intArrayOf(4, 3, 2, 1, 0, R, 0, 2))
    private val neighborWalk = MelodyMotif("neighbor", intArrayOf(2, 3, 2, 1, 2, 0, 1, 2))
    private val leapReturn = MelodyMotif("leap", intArrayOf(0, 3, 2, 4, 2, 1, 0, R))
    private val pedalReturn = MelodyMotif("pedal", intArrayOf(0, 2, 0, 3, 0, 4, 0, 2))
    private val callHigh = MelodyMotif("call", intArrayOf(2, 3, 4, 3, R, 2, 1))
    private val responseLow = MelodyMotif("resp", intArrayOf(2, 1, 0, R, 0, 1, 0))
    private val question = MelodyMotif("q", intArrayOf(1, 2, 3, 4, R, 3, 2))
    private val answer = MelodyMotif("a", intArrayOf(4, 3, 1, 0, R, 0, 2, 0))
    private val arcade = MelodyMotif("arcade", intArrayOf(0, 2, 4, 2, 3, 4, 2, 0))
    private val waltz = MelodyMotif("waltz", intArrayOf(0, 2, 4, R, 4, 2, 0, R, 2, 3))
    private val bounce = MelodyMotif("bounce", intArrayOf(0, 3, 0, 4, 2, 3, 0, 2))
    private val lullaby = MelodyMotif("lull", intArrayOf(4, 2, 0, 2, 3, 4, 3, R, 2, 0, R, 0))
    private val stepClimb = MelodyMotif("climb", intArrayOf(0, 1, 2, 1, 2, 3, 2, 3, 4, 3, 2))
    private val echoPair = MelodyMotif("echo", intArrayOf(0, 2, R, 0, 2, 4, R, 3, 2, 0))
    private val africanRoll = MelodyMotif("afr", intArrayOf(0, 2, 3, 2, 4, 3, 2, 0, 2))
    private val wholeDrift = MelodyMotif("drift", intArrayOf(0, 1, 2, 3, 2, 1, 0, R, 0))
    private val longTone = MelodyMotif("long", intArrayOf(0, R, 0, R, 2, R, 0))
    private val chimeLeap = MelodyMotif("chime", intArrayOf(0, 3, R, 4, 2, R, 3, 0))
    private val classicalTheme = MelodyMotif(
        "class_th",
        intArrayOf(0, 2, 4, 3, 4, 2, 0, R, 3, 2, 0, 2, 3, 4, 2, 0),
    )
    private val bluesLick = MelodyMotif(
        "blues",
        intArrayOf(0, 2, 3, 2, 0, R, 4, 3, 2, 0, 2, 0),
    )
    private val balladArc = MelodyMotif(
        "ballad",
        intArrayOf(0, 2, 4, R, 4, 3, 2, 0, R, 2, 3, 2, 0),
    )
    private val jazzGuide = MelodyMotif(
        "jazz_g",
        intArrayOf(2, 1, 0, 3, 2, 1, R, 0, 2, 3, 4, 3, 2, 0),
    )
    private val sparseBreath = MelodyMotif("breath", intArrayOf(0, R, R, 2, R, R, 3, R, 0))

    private val zen = listOf(
        ascendingPhrase, descendingSigh, neighborWalk, pedalReturn, lullaby, stepClimb,
        echoPair, classicalTheme, balladArc,
    )
    private val jazz = listOf(
        neighborWalk, leapReturn, question, answer, descendingSigh, arcade, bounce,
        waltz, echoPair, bluesLick, jazzGuide,
    )
    private val guitar = listOf(
        ascendingPhrase, pedalReturn, leapReturn, callHigh, responseLow, stepClimb,
        lullaby, balladArc, classicalTheme,
    )
    private val african = listOf(africanRoll, callHigh, responseLow, ascendingPhrase, pedalReturn, bounce)
    private val cosmic = listOf(wholeDrift, longTone, descendingSigh, leapReturn, lullaby, sparseBreath)
    private val bowl = listOf(longTone, pedalReturn, descendingSigh, echoPair, sparseBreath)
    private val ocean = listOf(longTone, wholeDrift, ascendingPhrase, lullaby, sparseBreath, balladArc)
    private val chimes = listOf(chimeLeap, callHigh, neighborWalk, wholeDrift, leapReturn, sparseBreath)

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

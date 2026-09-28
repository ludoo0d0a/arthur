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
    private val ascending3 = MelodyMotif("asc3", intArrayOf(0, 1, 2))
    private val descendingSigh = MelodyMotif("sigh", intArrayOf(4, 2, 0))
    private val neighbor = MelodyMotif("neighbor", intArrayOf(2, 3, 2, 1))
    private val leapReturn = MelodyMotif("leap", intArrayOf(0, 4, 2))
    private val pedalReturn = MelodyMotif("pedal", intArrayOf(0, 2, 0, 3, 0))
    private val callHigh = MelodyMotif("call", intArrayOf(3, 4, 5))
    private val responseLow = MelodyMotif("resp", intArrayOf(2, 1, 0))
    private val longTone = MelodyMotif("long", intArrayOf(0, MelodyMotif.REST, 0))
    private val question = MelodyMotif("q", intArrayOf(1, 2, 4, MelodyMotif.REST))
    private val answer = MelodyMotif("a", intArrayOf(3, 1, 0))
    private val africanRoll = MelodyMotif("afr", intArrayOf(0, 2, 3, 2, 5, 3))
    private val wholeDrift = MelodyMotif("drift", intArrayOf(0, 1, 2, 3, 2, 1))

    private val zen = listOf(ascending3, descendingSigh, neighbor, pedalReturn, longTone)
    private val jazz = listOf(neighbor, leapReturn, question, answer, descendingSigh)
    private val guitar = listOf(ascending3, pedalReturn, leapReturn, callHigh, responseLow)
    private val african = listOf(africanRoll, callHigh, responseLow, ascending3, pedalReturn)
    private val cosmic = listOf(wholeDrift, longTone, descendingSigh, leapReturn)
    private val bowl = listOf(longTone, pedalReturn, descendingSigh)
    private val ocean = listOf(longTone, wholeDrift, ascending3)
    private val chimes = listOf(leapReturn, callHigh, neighbor, wholeDrift)

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

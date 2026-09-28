package fr.geoking.arthur.audio.banks

/** Chord quality as scale-degree offsets stacked above the progression root. */
enum class ChordQuality(val intervals: IntArray) {
    RootFifth(intArrayOf(0, 7)),
    Add9(intArrayOf(0, 7, 14)),
    Sus2(intArrayOf(0, 2, 7)),
    OpenTriad(intArrayOf(0, 4, 7)),
    JazzDrop(intArrayOf(0, 10, 14, 17)),
    ParallelThirds(intArrayOf(0, 4)),
    ParallelFourths(intArrayOf(0, 5)),
}

/** Roman-ish progression state for the harmony Markov chain. */
enum class HarmonyState {
    I, IV, V, Ii, BVII, Pedal
}

data class HarmonyPattern(
    val id: String,
    val qualities: List<ChordQuality>,
    val progression: List<HarmonyState>,
)

object HarmonyBank {
    private val zenPads = HarmonyPattern(
        "zen_pads",
        listOf(ChordQuality.RootFifth, ChordQuality.Sus2, ChordQuality.Add9),
        listOf(HarmonyState.I, HarmonyState.Pedal, HarmonyState.IV, HarmonyState.I),
    )
    private val jazzProg = HarmonyPattern(
        "jazz_ii_v",
        listOf(ChordQuality.JazzDrop, ChordQuality.OpenTriad, ChordQuality.Add9),
        listOf(HarmonyState.Ii, HarmonyState.V, HarmonyState.I, HarmonyState.IV),
    )
    private val barWarm = HarmonyPattern(
        "bar_warm",
        listOf(ChordQuality.OpenTriad, ChordQuality.Add9, ChordQuality.Sus2),
        listOf(HarmonyState.I, HarmonyState.BVII, HarmonyState.IV, HarmonyState.I),
    )
    private val guitarOpen = HarmonyPattern(
        "guitar_open",
        listOf(ChordQuality.RootFifth, ChordQuality.Sus2, ChordQuality.OpenTriad),
        listOf(HarmonyState.I, HarmonyState.IV, HarmonyState.I, HarmonyState.V),
    )
    private val africanPar = HarmonyPattern(
        "afr_par",
        listOf(ChordQuality.ParallelThirds, ChordQuality.ParallelFourths, ChordQuality.RootFifth),
        listOf(HarmonyState.I, HarmonyState.IV, HarmonyState.BVII, HarmonyState.I),
    )
    private val cosmic = HarmonyPattern(
        "cosmic",
        listOf(ChordQuality.RootFifth, ChordQuality.Add9, ChordQuality.Sus2),
        listOf(HarmonyState.Pedal, HarmonyState.I, HarmonyState.BVII, HarmonyState.Pedal),
    )
    private val bowl = HarmonyPattern(
        "bowl",
        listOf(ChordQuality.RootFifth, ChordQuality.Add9),
        listOf(HarmonyState.Pedal, HarmonyState.I, HarmonyState.Pedal),
    )
    private val ocean = HarmonyPattern(
        "ocean",
        listOf(ChordQuality.Sus2, ChordQuality.RootFifth),
        listOf(HarmonyState.Pedal, HarmonyState.IV, HarmonyState.I),
    )

    fun patternsFor(style: fr.geoking.arthur.audio.MusicStyle): List<HarmonyPattern> =
        when (style) {
            fr.geoking.arthur.audio.MusicStyle.Zen -> listOf(zenPads, ocean, bowl)
            fr.geoking.arthur.audio.MusicStyle.JazzPiano -> listOf(jazzProg, barWarm)
            fr.geoking.arthur.audio.MusicStyle.BarAmbience, fr.geoking.arthur.audio.MusicStyle.NightLounge ->
                listOf(barWarm, jazzProg, zenPads)
            fr.geoking.arthur.audio.MusicStyle.SoftGuitar -> listOf(guitarOpen, zenPads)
            fr.geoking.arthur.audio.MusicStyle.AfricanPulse -> listOf(africanPar, guitarOpen)
            fr.geoking.arthur.audio.MusicStyle.CosmicDrone -> listOf(cosmic, bowl, zenPads)
            fr.geoking.arthur.audio.MusicStyle.TibetanBowl -> listOf(bowl, zenPads, cosmic)
            fr.geoking.arthur.audio.MusicStyle.OceanWaves -> listOf(ocean, zenPads)
            fr.geoking.arthur.audio.MusicStyle.WindChimes -> listOf(zenPads, cosmic, ocean)
        }

    fun pick(style: fr.geoking.arthur.audio.MusicStyle, index: Int): HarmonyPattern {
        val list = patternsFor(style)
        return list[Math.floorMod(index, list.size)]
    }

    /** Semitone offset of a progression state relative to root. */
    fun stateSemitones(state: HarmonyState): Int = when (state) {
        HarmonyState.I, HarmonyState.Pedal -> 0
        HarmonyState.Ii -> 2
        HarmonyState.IV -> 5
        HarmonyState.V -> 7
        HarmonyState.BVII -> 10
    }
}

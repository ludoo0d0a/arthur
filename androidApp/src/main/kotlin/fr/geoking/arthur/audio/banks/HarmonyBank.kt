package fr.geoking.arthur.audio.banks

/** Chord quality as scale-degree offsets stacked above the progression root. */
enum class ChordQuality(val intervals: IntArray) {
    RootFifth(intArrayOf(0, 7)),
    Add9(intArrayOf(0, 7, 14)),
    Sus2(intArrayOf(0, 2, 7)),
    OpenTriad(intArrayOf(0, 4, 7)),
    JazzDrop(intArrayOf(0, 10, 14, 17)),
    Minor7(intArrayOf(0, 3, 7, 10)),
    Dom7(intArrayOf(0, 4, 7, 10)),
    ParallelThirds(intArrayOf(0, 4)),
    ParallelFourths(intArrayOf(0, 5)),
}

/** Roman-ish progression state for the harmony chain. */
enum class HarmonyState {
    I, IV, V, Ii, Vi, BVII, Pedal
}

data class HarmonyPattern(
    val id: String,
    val qualities: List<ChordQuality>,
    val progression: List<HarmonyState>,
    /** Prefer walking the progression in order (refrain) vs free Markov. */
    val sequential: Boolean = true,
)

object HarmonyBank {
    private val zenPads = HarmonyPattern(
        "zen_pads",
        listOf(ChordQuality.RootFifth, ChordQuality.Sus2, ChordQuality.Add9, ChordQuality.OpenTriad),
        listOf(HarmonyState.I, HarmonyState.V, HarmonyState.Vi, HarmonyState.IV),
    )
    private val classicalCadence = HarmonyPattern(
        "classical_iv_v",
        listOf(ChordQuality.OpenTriad, ChordQuality.Add9, ChordQuality.RootFifth),
        listOf(HarmonyState.I, HarmonyState.IV, HarmonyState.V, HarmonyState.I),
    )
    private val jazzProg = HarmonyPattern(
        "jazz_ii_v",
        listOf(ChordQuality.Minor7, ChordQuality.Dom7, ChordQuality.JazzDrop, ChordQuality.OpenTriad),
        listOf(HarmonyState.Ii, HarmonyState.V, HarmonyState.I, HarmonyState.Vi),
    )
    private val jazzTurnaround = HarmonyPattern(
        "jazz_turn",
        listOf(ChordQuality.Minor7, ChordQuality.Dom7, ChordQuality.Add9),
        listOf(HarmonyState.I, HarmonyState.Vi, HarmonyState.Ii, HarmonyState.V),
    )
    private val blues12 = HarmonyPattern(
        "blues_12",
        listOf(ChordQuality.Dom7, ChordQuality.OpenTriad, ChordQuality.RootFifth),
        listOf(
            HarmonyState.I, HarmonyState.I, HarmonyState.I, HarmonyState.I,
            HarmonyState.IV, HarmonyState.IV, HarmonyState.I, HarmonyState.I,
            HarmonyState.V, HarmonyState.IV, HarmonyState.I, HarmonyState.V,
        ),
    )
    private val barWarm = HarmonyPattern(
        "bar_warm",
        listOf(ChordQuality.OpenTriad, ChordQuality.Add9, ChordQuality.Sus2, ChordQuality.Minor7),
        listOf(HarmonyState.I, HarmonyState.BVII, HarmonyState.IV, HarmonyState.I),
    )
    private val balladPop = HarmonyPattern(
        "ballad_pop",
        listOf(ChordQuality.OpenTriad, ChordQuality.Add9, ChordQuality.Sus2),
        listOf(HarmonyState.I, HarmonyState.V, HarmonyState.Vi, HarmonyState.IV),
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
        listOf(HarmonyState.Pedal, HarmonyState.I, HarmonyState.Pedal, HarmonyState.IV),
    )
    private val ocean = HarmonyPattern(
        "ocean",
        listOf(ChordQuality.Sus2, ChordQuality.RootFifth, ChordQuality.Add9),
        listOf(HarmonyState.Pedal, HarmonyState.IV, HarmonyState.I, HarmonyState.Pedal),
    )

    fun patternsFor(style: fr.geoking.arthur.audio.MusicStyle): List<HarmonyPattern> =
        when (style) {
            fr.geoking.arthur.audio.MusicStyle.Zen -> listOf(zenPads, classicalCadence, balladPop, ocean)
            fr.geoking.arthur.audio.MusicStyle.JazzPiano -> listOf(jazzProg, jazzTurnaround, blues12)
            fr.geoking.arthur.audio.MusicStyle.BarAmbience, fr.geoking.arthur.audio.MusicStyle.NightLounge ->
                listOf(barWarm, jazzProg, blues12, balladPop)
            fr.geoking.arthur.audio.MusicStyle.SoftGuitar,
            fr.geoking.arthur.audio.MusicStyle.RockBallad,
            fr.geoking.arthur.audio.MusicStyle.HawaiianUkulele,
            -> listOf(guitarOpen, balladPop, classicalCadence, zenPads)
            fr.geoking.arthur.audio.MusicStyle.AfricanPulse -> listOf(africanPar, guitarOpen)
            fr.geoking.arthur.audio.MusicStyle.CosmicDrone,
            fr.geoking.arthur.audio.MusicStyle.OrchestraPads,
            fr.geoking.arthur.audio.MusicStyle.OrchestraSwell,
            -> listOf(cosmic, bowl, zenPads)
            fr.geoking.arthur.audio.MusicStyle.TibetanBowl -> listOf(bowl, zenPads, cosmic)
            fr.geoking.arthur.audio.MusicStyle.OceanWaves,
            fr.geoking.arthur.audio.MusicStyle.SoftRain,
            fr.geoking.arthur.audio.MusicStyle.WindAmbience,
            fr.geoking.arthur.audio.MusicStyle.Fireplace,
            fr.geoking.arthur.audio.MusicStyle.Songbirds,
            -> listOf(ocean, zenPads, balladPop)
            fr.geoking.arthur.audio.MusicStyle.WindChimes -> listOf(zenPads, cosmic, ocean)
            fr.geoking.arthur.audio.MusicStyle.ClassicalPiano,
            fr.geoking.arthur.audio.MusicStyle.PianoBallad,
            fr.geoking.arthur.audio.MusicStyle.ViolinLead,
            -> listOf(classicalCadence, balladPop, zenPads)
            fr.geoking.arthur.audio.MusicStyle.BassOnly -> listOf(blues12, barWarm, jazzProg)
            fr.geoking.arthur.audio.MusicStyle.Chiptune,
            fr.geoking.arthur.audio.MusicStyle.ChipArp,
            fr.geoking.arthur.audio.MusicStyle.ArcadeGlow,
            -> listOf(balladPop, classicalCadence, africanPar, blues12)
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
        HarmonyState.Vi -> 9
        HarmonyState.BVII -> 10
    }
}

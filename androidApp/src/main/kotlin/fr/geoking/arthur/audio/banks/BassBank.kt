package fr.geoking.arthur.audio.banks

import fr.geoking.arthur.audio.MusicStyle

/** Bass pattern as chord-relative steps: 0 = root, 1 = fifth, 2 = walking approach. */
enum class BassStep {
    Root,
    Fifth,
    WalkUp,
    WalkDown,
    Pedal,
}

data class BassPattern(
    val id: String,
    val steps: List<BassStep>,
)

object BassBank {
    private val jazzWalk = BassPattern(
        "jazz_walk",
        listOf(BassStep.Root, BassStep.Fifth, BassStep.WalkUp, BassStep.Root),
    )
    private val bluesRoot5 = BassPattern(
        "blues_r5",
        listOf(BassStep.Root, BassStep.Root, BassStep.Fifth, BassStep.Root),
    )
    private val balladPedal = BassPattern(
        "ballad_ped",
        listOf(BassStep.Pedal, BassStep.Root, BassStep.Pedal, BassStep.Fifth),
    )
    private val classical = BassPattern(
        "classical",
        listOf(BassStep.Root, BassStep.Fifth, BassStep.Root, BassStep.WalkDown),
    )
    private val drone = BassPattern(
        "drone",
        listOf(BassStep.Pedal, BassStep.Pedal, BassStep.Root, BassStep.Pedal),
    )

    fun patternFor(style: MusicStyle, index: Int): BassPattern {
        val list = when (style) {
            MusicStyle.JazzPiano, MusicStyle.BarAmbience, MusicStyle.NightLounge,
            MusicStyle.BassOnly,
            -> listOf(jazzWalk, bluesRoot5, balladPedal)
            MusicStyle.SoftGuitar, MusicStyle.Zen, MusicStyle.RockBallad, MusicStyle.HawaiianUkulele,
            MusicStyle.ClassicalPiano, MusicStyle.PianoBallad, MusicStyle.ViolinLead,
            -> listOf(balladPedal, classical, jazzWalk)
            MusicStyle.AfricanPulse ->
                listOf(bluesRoot5, jazzWalk)
            MusicStyle.OceanWaves, MusicStyle.TibetanBowl, MusicStyle.CosmicDrone,
            MusicStyle.WindChimes, MusicStyle.SoftRain, MusicStyle.WindAmbience,
            MusicStyle.Fireplace, MusicStyle.Songbirds, MusicStyle.OrchestraPads,
            MusicStyle.OrchestraSwell,
            -> listOf(drone, balladPedal)
        }
        return list[Math.floorMod(index, list.size)]
    }

    /**
     * Semitone offset from chord root for a bass step.
     * [walkSemitone] is a small approach (±1 or ±2) chosen by the sequencer.
     */
    fun stepSemitones(step: BassStep, walkSemitone: Int = 2): Int = when (step) {
        BassStep.Root, BassStep.Pedal -> 0
        BassStep.Fifth -> 7
        BassStep.WalkUp -> walkSemitone.coerceIn(1, 3)
        BassStep.WalkDown -> -walkSemitone.coerceIn(1, 3)
    }
}

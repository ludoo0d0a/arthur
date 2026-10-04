package fr.geoking.arthur.audio

/** Content-driven ambient music style palette. */
enum class MusicStyle {
    Zen,
    BarAmbience,
    JazzPiano,
    SoftGuitar,
    TibetanBowl,
    OceanWaves,
    AfricanPulse,
    WindChimes,
    NightLounge,
    CosmicDrone,
    SoftRain,
    WindAmbience,
    Fireplace,
    Songbirds,
    ClassicalPiano,
    OrchestraPads,
    OrchestraSwell,
    ViolinLead,
    RockBallad,
    BassOnly,
    PianoBallad,
}

/** Snake_case ids shared with [fr.geoking.arthur.shared.marketplace.AudioPackCatalog]. */
object MusicStyleIds {
    fun toSuffix(style: MusicStyle): String = when (style) {
        MusicStyle.Zen -> "zen"
        MusicStyle.BarAmbience -> "bar_ambience"
        MusicStyle.JazzPiano -> "jazz_piano"
        MusicStyle.SoftGuitar -> "soft_guitar"
        MusicStyle.TibetanBowl -> "tibetan_bowl"
        MusicStyle.OceanWaves -> "ocean_waves"
        MusicStyle.AfricanPulse -> "african_pulse"
        MusicStyle.WindChimes -> "wind_chimes"
        MusicStyle.NightLounge -> "night_lounge"
        MusicStyle.CosmicDrone -> "cosmic_drone"
        MusicStyle.SoftRain -> "soft_rain"
        MusicStyle.WindAmbience -> "wind_ambience"
        MusicStyle.Fireplace -> "fireplace"
        MusicStyle.Songbirds -> "songbirds"
        MusicStyle.ClassicalPiano -> "classical_piano"
        MusicStyle.OrchestraPads -> "orchestra_pads"
        MusicStyle.OrchestraSwell -> "orchestra_swell"
        MusicStyle.ViolinLead -> "violin_lead"
        MusicStyle.RockBallad -> "rock_ballad"
        MusicStyle.BassOnly -> "bass_only"
        MusicStyle.PianoBallad -> "piano_ballad"
    }

    fun fromSuffix(suffix: String): MusicStyle? = MusicStyle.entries.firstOrNull {
        toSuffix(it) == suffix
    }
}

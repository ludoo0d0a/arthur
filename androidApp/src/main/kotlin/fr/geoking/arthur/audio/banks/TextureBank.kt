package fr.geoking.arthur.audio.banks

enum class TextureCueKind {
    WaveSwell,
    WindGust,
    SoftRain,
    BowlStrike,
    ChimeCluster,
    ArpeggioCascade,
    ThumbPianoRoll,
    SilenceBreath,
    FireCrackle,
    BirdChirp,
    StringSwell,
}

data class TexturePattern(
    val id: String,
    val cues: List<TextureCueKind>,
)

object TextureBank {
    private val ocean = TexturePattern(
        "ocean",
        listOf(TextureCueKind.WaveSwell, TextureCueKind.SoftRain, TextureCueKind.SilenceBreath),
    )
    private val wind = TexturePattern(
        "wind",
        listOf(TextureCueKind.WindGust, TextureCueKind.ChimeCluster, TextureCueKind.SilenceBreath),
    )
    private val bowl = TexturePattern(
        "bowl",
        listOf(TextureCueKind.BowlStrike, TextureCueKind.SilenceBreath, TextureCueKind.ChimeCluster),
    )
    private val jazz = TexturePattern(
        "jazz",
        listOf(TextureCueKind.ArpeggioCascade, TextureCueKind.SilenceBreath, TextureCueKind.ChimeCluster),
    )
    private val african = TexturePattern(
        "african",
        listOf(TextureCueKind.ThumbPianoRoll, TextureCueKind.ArpeggioCascade, TextureCueKind.SilenceBreath),
    )
    private val zen = TexturePattern(
        "zen",
        listOf(TextureCueKind.ChimeCluster, TextureCueKind.BowlStrike, TextureCueKind.SilenceBreath),
    )
    private val cosmic = TexturePattern(
        "cosmic",
        listOf(TextureCueKind.BowlStrike, TextureCueKind.WindGust, TextureCueKind.SilenceBreath),
    )
    private val lounge = TexturePattern(
        "lounge",
        listOf(TextureCueKind.ArpeggioCascade, TextureCueKind.SilenceBreath, TextureCueKind.SoftRain),
    )
    private val rain = TexturePattern(
        "rain",
        listOf(TextureCueKind.SoftRain, TextureCueKind.WindGust, TextureCueKind.SilenceBreath),
    )
    private val fire = TexturePattern(
        "fire",
        listOf(TextureCueKind.FireCrackle, TextureCueKind.SilenceBreath, TextureCueKind.SoftRain),
    )
    private val birds = TexturePattern(
        "birds",
        listOf(TextureCueKind.BirdChirp, TextureCueKind.SilenceBreath, TextureCueKind.ChimeCluster),
    )
    private val strings = TexturePattern(
        "strings",
        listOf(TextureCueKind.StringSwell, TextureCueKind.SilenceBreath, TextureCueKind.ArpeggioCascade),
    )

    fun patternsFor(style: fr.geoking.arthur.audio.MusicStyle): List<TexturePattern> =
        when (style) {
            fr.geoking.arthur.audio.MusicStyle.OceanWaves -> listOf(ocean, wind, zen)
            fr.geoking.arthur.audio.MusicStyle.SoftRain -> listOf(rain, ocean, wind)
            fr.geoking.arthur.audio.MusicStyle.WindAmbience -> listOf(wind, rain, zen)
            fr.geoking.arthur.audio.MusicStyle.Fireplace -> listOf(fire, zen, wind)
            fr.geoking.arthur.audio.MusicStyle.Songbirds -> listOf(birds, wind, zen)
            fr.geoking.arthur.audio.MusicStyle.WindChimes -> listOf(wind, zen, cosmic)
            fr.geoking.arthur.audio.MusicStyle.TibetanBowl -> listOf(bowl, zen, cosmic)
            fr.geoking.arthur.audio.MusicStyle.JazzPiano, fr.geoking.arthur.audio.MusicStyle.BarAmbience ->
                listOf(jazz, lounge, zen)
            fr.geoking.arthur.audio.MusicStyle.NightLounge,
            fr.geoking.arthur.audio.MusicStyle.PianoBallad,
            -> listOf(lounge, jazz, wind)
            fr.geoking.arthur.audio.MusicStyle.AfricanPulse -> listOf(african, jazz, zen)
            fr.geoking.arthur.audio.MusicStyle.SoftGuitar,
            fr.geoking.arthur.audio.MusicStyle.RockBallad,
            fr.geoking.arthur.audio.MusicStyle.HawaiianUkulele,
            -> listOf(jazz, zen, african)
            fr.geoking.arthur.audio.MusicStyle.CosmicDrone,
            fr.geoking.arthur.audio.MusicStyle.OrchestraPads,
            fr.geoking.arthur.audio.MusicStyle.OrchestraSwell,
            -> listOf(cosmic, bowl, wind)
            fr.geoking.arthur.audio.MusicStyle.Zen -> listOf(zen, bowl, wind)
            fr.geoking.arthur.audio.MusicStyle.ClassicalPiano,
            fr.geoking.arthur.audio.MusicStyle.ViolinLead,
            -> listOf(strings, zen, jazz)
            fr.geoking.arthur.audio.MusicStyle.BassOnly -> listOf(jazz, lounge, zen)
        }

    fun pick(style: fr.geoking.arthur.audio.MusicStyle, index: Int): TexturePattern {
        val list = patternsFor(style)
        return list[Math.floorMod(index, list.size)]
    }

    fun transitionCue(style: fr.geoking.arthur.audio.MusicStyle, index: Int): TextureCueKind {
        val pattern = pick(style, index)
        return pattern.cues[Math.floorMod(index, pattern.cues.size)]
    }
}

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

    fun patternsFor(style: fr.geoking.arthur.audio.MusicStyle): List<TexturePattern> =
        when (style) {
            fr.geoking.arthur.audio.MusicStyle.OceanWaves -> listOf(ocean, wind, zen)
            fr.geoking.arthur.audio.MusicStyle.WindChimes -> listOf(wind, zen, cosmic)
            fr.geoking.arthur.audio.MusicStyle.TibetanBowl -> listOf(bowl, zen, cosmic)
            fr.geoking.arthur.audio.MusicStyle.JazzPiano, fr.geoking.arthur.audio.MusicStyle.BarAmbience ->
                listOf(jazz, lounge, zen)
            fr.geoking.arthur.audio.MusicStyle.NightLounge -> listOf(lounge, jazz, wind)
            fr.geoking.arthur.audio.MusicStyle.AfricanPulse -> listOf(african, jazz, zen)
            fr.geoking.arthur.audio.MusicStyle.SoftGuitar -> listOf(jazz, zen, african)
            fr.geoking.arthur.audio.MusicStyle.CosmicDrone -> listOf(cosmic, bowl, wind)
            fr.geoking.arthur.audio.MusicStyle.Zen -> listOf(zen, bowl, wind)
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

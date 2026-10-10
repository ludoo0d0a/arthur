package fr.geoking.arthur.shared.marketplace

/**
 * Audio Marketplace packs: thematic bundles of procedural music style suffixes.
 * Style suffixes are snake_case ids shared with Android [MusicStyle] mapping.
 */
object AudioPackCatalog {
    const val JAZZ_AFTER_DARK = "jazz_after_dark"
    const val WORLD_PULSE = "world_pulse"
    const val WIND_GARDEN = "wind_garden"
    const val COSMIC_DRIFT = "cosmic_drift"
    const val HEARTH_WEATHER = "hearth_weather"
    const val DAWN_CHORUS = "dawn_chorus"
    const val TEMPLE_RESONANCE = "temple_resonance"
    const val SALON_CLASSIQUE = "salon_classique"
    const val GRAND_ORCHESTRA = "grand_orchestra"
    const val SOLO_VIOLIN = "solo_violin"
    const val ROCK_BALLAD = "rock_ballad"
    const val BASS_ONLY = "bass_only"
    const val MIDNIGHT_BALLAD = "midnight_ballad"
    const val HAWAIIAN_BREEZE = "hawaiian_breeze"
    const val ARCADE_CHIPS = "arcade_chips"

    /** Free styles always playable without a pack purchase. */
    val freeStyleSuffixes: Set<String> = setOf(
        "jazz_piano",
        "zen",
        "soft_guitar",
    )

    data class PackDef(
        val suffix: String,
        val styleSuffixes: List<String>,
        /** Primary style used when the user taps the pack tile. */
        val primaryStyleSuffix: String,
    )

    val monetizedPacks: List<PackDef> = listOf(
        PackDef(JAZZ_AFTER_DARK, listOf("bar_ambience", "night_lounge"), "night_lounge"),
        PackDef(WORLD_PULSE, listOf("african_pulse"), "african_pulse"),
        PackDef(WIND_GARDEN, listOf("wind_chimes"), "wind_chimes"),
        PackDef(COSMIC_DRIFT, listOf("cosmic_drone"), "cosmic_drone"),
        PackDef(
            HEARTH_WEATHER,
            listOf("ocean_waves", "soft_rain", "wind_ambience", "fireplace"),
            "ocean_waves",
        ),
        PackDef(DAWN_CHORUS, listOf("songbirds"), "songbirds"),
        PackDef(TEMPLE_RESONANCE, listOf("tibetan_bowl"), "tibetan_bowl"),
        PackDef(SALON_CLASSIQUE, listOf("classical_piano"), "classical_piano"),
        PackDef(
            GRAND_ORCHESTRA,
            listOf("orchestra_pads", "orchestra_swell"),
            "orchestra_pads",
        ),
        PackDef(SOLO_VIOLIN, listOf("violin_lead"), "violin_lead"),
        PackDef(ROCK_BALLAD, listOf("rock_ballad"), "rock_ballad"),
        PackDef(BASS_ONLY, listOf("bass_only"), "bass_only"),
        PackDef(MIDNIGHT_BALLAD, listOf("piano_ballad"), "piano_ballad"),
        PackDef(HAWAIIAN_BREEZE, listOf("hawaiian_ukulele"), "hawaiian_ukulele"),
        PackDef(
            ARCADE_CHIPS,
            listOf("chiptune", "chip_arp", "arcade_glow"),
            "chiptune",
        ),
    )

    val monetizedPackSuffixes: List<String> = monetizedPacks.map { it.suffix }

    private val styleToPacks: Map<String, List<String>> = buildMap {
        for (pack in monetizedPacks) {
            for (style in pack.styleSuffixes) {
                val existing = get(style).orEmpty()
                put(style, existing + pack.suffix)
            }
        }
    }

    fun packsCoveringStyle(styleSuffix: String): List<String> =
        styleToPacks[styleSuffix].orEmpty()

    fun packDef(suffix: String): PackDef? = monetizedPacks.firstOrNull { it.suffix == suffix }

    fun primaryStyleForPack(suffix: String): String? = packDef(suffix)?.primaryStyleSuffix
}

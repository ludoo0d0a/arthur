package fr.geoking.arthur.audio.banks

import fr.geoking.arthur.audio.MusicStyle

/** Facade over style-tagged pattern libraries. */
object PatternBank {
    fun ensureNonEmpty(style: MusicStyle) {
        require(MelodyBank.motifsFor(style).isNotEmpty()) { "melody empty for $style" }
        require(RhythmBank.patternsFor(style).isNotEmpty()) { "rhythm empty for $style" }
        require(HarmonyBank.patternsFor(style).isNotEmpty()) { "harmony empty for $style" }
        require(TextureBank.patternsFor(style).isNotEmpty()) { "texture empty for $style" }
    }
}

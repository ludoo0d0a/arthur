package fr.geoking.arthur.audio.stem

import kotlinx.serialization.Serializable

@Serializable
enum class StemCategory {
    NATURE_FX,
    SCIFI_FX,
    JAZZ_MUSIC,
    CHORDS,
    PERCUSSION,
    AMBIENT_PAD
}

@Serializable
data class StemTrack(
    val id: String,
    val name: String,
    val category: StemCategory,
    val filename: String,
    val defaultGain: Float = 0.8f,
    val defaultPan: Float = 0.0f,
    val isOptional: Boolean = false,
    val activationProbability: Float = 1.0f,
    val dynamicRule: String = "DEFAULT"
)

@Serializable
data class StemArrangement(
    val id: String,
    val title: String,
    val bpm: Int = 90,
    val key: String = "C Major",
    val loopDurationSeconds: Double = 8.0,
    val tracks: List<StemTrack>
)

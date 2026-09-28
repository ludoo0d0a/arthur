package fr.geoking.arthur.audio

/**
 * Deterministic ambient preset derived from an artwork id.
 * Same id → same style/banks/root/tempo class; session walks stay random inside this space.
 */
data class MusicPreset(
    val artworkId: String,
    val style: MusicStyle,
    /** Stable seed from artwork id (banks + matrices). */
    val artworkSeed: Long,
    /** Root frequency in Hz (around A3–C5). */
    val rootHz: Float,
    /** Scale degree offsets in semitones from root (ascending). */
    val scaleSemitones: IntArray,
    /** Nominal BPM for rhythm grid. */
    val tempoBpm: Float,
    /** 0..1 melodic activity. */
    val density: Float,
    /** 0..1 chance of ornaments / transition decorations. */
    val ornamentRate: Float,
    val trackMix: TrackMix,
    val melodyBankIndex: Int,
    val rhythmBankIndex: Int,
    val harmonyBankIndex: Int,
    val textureBankIndex: Int,
    val formSeed: Long,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MusicPreset) return false
        return artworkId == other.artworkId &&
            style == other.style &&
            artworkSeed == other.artworkSeed &&
            rootHz == other.rootHz &&
            scaleSemitones.contentEquals(other.scaleSemitones) &&
            tempoBpm == other.tempoBpm &&
            density == other.density &&
            ornamentRate == other.ornamentRate &&
            trackMix == other.trackMix &&
            melodyBankIndex == other.melodyBankIndex &&
            rhythmBankIndex == other.rhythmBankIndex &&
            harmonyBankIndex == other.harmonyBankIndex &&
            textureBankIndex == other.textureBankIndex &&
            formSeed == other.formSeed
    }

    override fun hashCode(): Int {
        var result = artworkId.hashCode()
        result = 31 * result + style.hashCode()
        result = 31 * result + artworkSeed.hashCode()
        result = 31 * result + rootHz.hashCode()
        result = 31 * result + scaleSemitones.contentHashCode()
        result = 31 * result + tempoBpm.hashCode()
        result = 31 * result + density.hashCode()
        result = 31 * result + ornamentRate.hashCode()
        result = 31 * result + trackMix.hashCode()
        result = 31 * result + melodyBankIndex
        result = 31 * result + rhythmBankIndex
        result = 31 * result + harmonyBankIndex
        result = 31 * result + textureBankIndex
        result = 31 * result + formSeed.hashCode()
        return result
    }
}

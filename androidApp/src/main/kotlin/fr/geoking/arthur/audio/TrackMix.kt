package fr.geoking.arthur.audio

/** Relative gains for the six concurrent ambient tracks (0..1). */
data class TrackMix(
    val bed: Float = 0.45f,
    val harmony: Float = 0.25f,
    val melody: Float = 0.30f,
    val texture: Float = 0.15f,
    val pulse: Float = 0.12f,
    val transition: Float = 0.40f,
) {
    fun clamped(): TrackMix = copy(
        bed = bed.coerceIn(0f, 1f),
        harmony = harmony.coerceIn(0f, 1f),
        melody = melody.coerceIn(0f, 1f),
        texture = texture.coerceIn(0f, 1f),
        pulse = pulse.coerceIn(0f, 1f),
        transition = transition.coerceIn(0f, 1f),
    )
}

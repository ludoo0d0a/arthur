package fr.geoking.arthur.audio

/**
 * Audio focus changes that ambient playback should mirror into play/pause.
 */
sealed interface AudioFocusEvent {
    data class Lost(val transient: Boolean) : AudioFocusEvent
    data object Gained : AudioFocusEvent
}

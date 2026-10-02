package fr.geoking.arthur.audio

/**
 * Maps [AudioFocusEvent] to ambient play/pause: permanent loss pauses for good,
 * transient loss pauses and resumes on the next [AudioFocusEvent.Gained].
 */
class AmbientAudioFocusHandler(
    private val isPlaying: () -> Boolean,
    private val setPlaying: (Boolean) -> Unit,
) {
    private var resumeOnFocusGain: Boolean = false

    fun onFocusEvent(event: AudioFocusEvent) {
        when (event) {
            is AudioFocusEvent.Lost -> {
                resumeOnFocusGain = event.transient && isPlaying()
                setPlaying(false)
            }
            AudioFocusEvent.Gained -> {
                if (resumeOnFocusGain) {
                    resumeOnFocusGain = false
                    setPlaying(true)
                }
            }
        }
    }
}

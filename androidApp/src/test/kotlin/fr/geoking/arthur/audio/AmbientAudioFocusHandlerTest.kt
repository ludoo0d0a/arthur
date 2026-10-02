package fr.geoking.arthur.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AmbientAudioFocusHandlerTest {
    @Test
    fun permanentLoss_pausesWithoutResumeOnGain() {
        var playing = true
        val handler = AmbientAudioFocusHandler(
            isPlaying = { playing },
            setPlaying = { playing = it },
        )

        handler.onFocusEvent(AudioFocusEvent.Lost(transient = false))
        assertFalse(playing)

        handler.onFocusEvent(AudioFocusEvent.Gained)
        assertFalse(playing)
    }

    @Test
    fun transientLoss_whilePlaying_resumesOnGain() {
        var playing = true
        val handler = AmbientAudioFocusHandler(
            isPlaying = { playing },
            setPlaying = { playing = it },
        )

        handler.onFocusEvent(AudioFocusEvent.Lost(transient = true))
        assertFalse(playing)

        handler.onFocusEvent(AudioFocusEvent.Gained)
        assertTrue(playing)
    }

    @Test
    fun transientLoss_whilePaused_doesNotResumeOnGain() {
        var playing = false
        val handler = AmbientAudioFocusHandler(
            isPlaying = { playing },
            setPlaying = { playing = it },
        )

        handler.onFocusEvent(AudioFocusEvent.Lost(transient = true))
        assertFalse(playing)

        handler.onFocusEvent(AudioFocusEvent.Gained)
        assertFalse(playing)
    }

    @Test
    fun resumeFlag_clearsAfterSingleGain() {
        var playing = true
        var setPlayingCalls = 0
        val handler = AmbientAudioFocusHandler(
            isPlaying = { playing },
            setPlaying = {
                setPlayingCalls += 1
                playing = it
            },
        )

        handler.onFocusEvent(AudioFocusEvent.Lost(transient = true))
        handler.onFocusEvent(AudioFocusEvent.Gained)
        handler.onFocusEvent(AudioFocusEvent.Gained)

        assertEquals(2, setPlayingCalls)
        assertTrue(playing)
    }
}

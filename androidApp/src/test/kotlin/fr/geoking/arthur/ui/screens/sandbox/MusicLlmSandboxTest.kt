package fr.geoking.arthur.ui.screens.sandbox

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import fr.geoking.arthur.phone.theme.ArthurTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MusicLlmSandboxTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun sandboxScreen_rendersInitialComponents() {
        var backCalled = false
        composeRule.setContent {
            ArthurTheme {
                MusicLlmSandboxScreen(
                    onBack = { backCalled = true },
                    downloader = null,
                )
            }
        }

        composeRule.onNodeWithTag("music_llm_sandbox_screen").assertIsDisplayed()
        composeRule.onNodeWithTag("sandbox_description").assertIsDisplayed()
        composeRule.onNodeWithTag("sandbox_model_section").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("sandbox_prompt_section").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("sandbox_format_section").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("sandbox_generate_btn").performScrollTo().assertIsDisplayed()

        composeRule.onNodeWithTag("music_sandbox_back").performClick()
        assertTrue(backCalled)
    }

    @Test
    fun sandboxScreen_presetSelectionAndUpdatePrompt() {
        composeRule.setContent {
            ArthurTheme {
                MusicLlmSandboxScreen(
                    onBack = {},
                    downloader = null,
                )
            }
        }

        composeRule.onNodeWithTag("sandbox_preset_chip_1").performScrollTo().performClick()
        composeRule.onNodeWithTag("sandbox_prompt_input").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun sandboxMiniPlayer_rendersResultAndStemControls() {
        val wav = java.io.File.createTempFile("sandbox_test", ".wav")
        // Minimal valid silent-header WAV is enough for UI; generator tests cover audible PCM.
        wav.writeBytes(
            byteArrayOf(
                0x52, 0x49, 0x46, 0x46, 0x2C, 0x00, 0x00, 0x00,
                0x57, 0x41, 0x56, 0x45, 0x66, 0x6D, 0x74, 0x20,
                0x10, 0x00, 0x00, 0x00, 0x01, 0x00, 0x01, 0x00,
                0x44, 0xAC.toByte(), 0x00, 0x00, 0x88.toByte(), 0x58, 0x01, 0x00,
                0x02, 0x00, 0x10, 0x00, 0x64, 0x61, 0x74, 0x61,
                0x08, 0x00, 0x00, 0x00, 0x00, 0x10, 0x00, 0x10,
                0x00, 0x10, 0x00, 0x10,
            ),
        )
        val dummyResult = GeneratedResult(
            id = "test-res-1",
            prompt = "Atmospheric Forest Rain",
            engineName = "MusicGen Small",
            format = MusicOutputFormat.MULTI_TRACK_STEMS,
            durationSeconds = 30,
            filePath = wav.absolutePath,
            fileSizeMb = 4.2f,
            stems = listOf(
                StemTrackState("s1", "Melody / Lead", isMuted = false, volume = 0.8f),
                StemTrackState("s2", "Bass / Sub", isMuted = true, volume = 0.5f),
            ),
        )

        var playToggled = false
        var mutedStemId: String? = null

        composeRule.setContent {
            ArthurTheme {
                SandboxMiniPlayer(
                    result = dummyResult,
                    isPlaying = false,
                    playbackPositionSec = 5f,
                    onPlayPauseToggle = { playToggled = true },
                    onStop = {},
                    onSeek = {},
                    onStemMuteToggle = { mutedStemId = it },
                )
            }
        }

        composeRule.onNodeWithTag("sandbox_mini_player").assertIsDisplayed()
        composeRule.onNodeWithTag("sandbox_mini_player_format_badge").assertIsDisplayed()
        composeRule.onNodeWithTag("sandbox_mini_player_details").assertIsDisplayed()
        composeRule.onNodeWithTag("sandbox_mini_player_play_pause").performClick()
        assertTrue(playToggled)

        composeRule.onNodeWithTag("sandbox_stem_row_s1").assertIsDisplayed()
        composeRule.onNodeWithTag("sandbox_stem_mute_s1").performClick()
        assertEquals("s1", mutedStemId)
        wav.delete()
    }

    @Test
    fun sandboxOutputFormatEnum_hasExpectedLabels() {
        assertEquals("Full Track (WAV / AAC)", MusicOutputFormat.FULL_TRACK.displayName)
        assertEquals("Multi-track Stems (Opus)", MusicOutputFormat.MULTI_TRACK_STEMS.displayName)
        assertEquals("MIDI + SoundFont", MusicOutputFormat.MIDI_SOUNDFONT.displayName)
        assertEquals("Procedural DSP Sound", MusicOutputFormat.PROCEDURAL_DSP.displayName)
    }
}

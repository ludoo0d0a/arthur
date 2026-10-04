package fr.geoking.arthur.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import fr.geoking.arthur.audio.MusicStyle
import fr.geoking.arthur.phone.theme.ArthurTheme
import fr.geoking.arthur.ui.components.AudioPackTopic
import fr.geoking.arthur.ui.components.PackFamily
import fr.geoking.arthur.ui.components.PackSelection
import fr.geoking.arthur.ui.components.primaryStyle
import fr.geoking.arthur.ui.components.stylesInPack
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SoundPlayerScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun essentialsStyles_areFreeAllowlist() {
        val styles = AudioPackTopic.Essentials.stylesInPack()
        assertEquals(
            listOf(MusicStyle.JazzPiano, MusicStyle.Zen, MusicStyle.SoftGuitar),
            styles,
        )
        assertEquals(MusicStyle.JazzPiano, AudioPackTopic.Essentials.primaryStyle())
    }

    @Test
    fun hearthWeather_includesAtmosphereStyles() {
        val styles = AudioPackTopic.HearthWeather.stylesInPack()
        assertTrue(MusicStyle.OceanWaves in styles)
        assertTrue(MusicStyle.SoftRain in styles)
        assertTrue(MusicStyle.Fireplace in styles)
        assertEquals(MusicStyle.OceanWaves, AudioPackTopic.HearthWeather.primaryStyle())
    }

    @Test
    fun soundPlayerScreen_showsPlayAndDismiss() {
        var dismissed = false
        composeRule.setContent {
            ArthurTheme {
                SoundPlayerScreen(
                    selection = PackSelection(
                        PackFamily.Sound,
                        AudioPackTopic.Essentials.testTagSuffix,
                    ),
                    onDismiss = { dismissed = true },
                    audioSettings = null,
                )
            }
        }
        composeRule.onNodeWithTag("sound_player_screen").assertIsDisplayed()
        composeRule.onNodeWithTag("sound_player_play_pause").assertIsDisplayed()
        composeRule.onNodeWithTag("sound_player_title").assertIsDisplayed()
        composeRule.onNodeWithTag("sound_player_play_pause").performClick()
        composeRule.onNodeWithTag("sound_player_done").performClick()
        assertTrue(dismissed)
    }
}

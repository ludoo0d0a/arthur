package fr.geoking.arthur.ui.screens.neural

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import fr.geoking.arthur.phone.theme.ArthurTheme
import fr.geoking.arthur.shared.audio.NeuralAudioPack
import fr.geoking.arthur.shared.neural.InMemoryNeuralTakeLibrary
import fr.geoking.arthur.shared.neural.NeuralMusicRoute
import fr.geoking.arthur.shared.neural.NeuralTake
import fr.geoking.arthur.shared.neural.routeAfterGenerateRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class NeuralMusicScreensTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val packId = NeuralAudioPack.MUSEUMS_CLASSICAL.id

    @Test
    fun hub_emptyState_andNewTrack() {
        var route: NeuralMusicRoute by mutableStateOf(NeuralMusicRoute.Hub(packId))
        composeRule.setContent {
            ArthurTheme {
                NeuralMusicHost(
                    route = route,
                    onRouteChange = { next -> if (next != null) route = next },
                    takeLibrary = InMemoryNeuralTakeLibrary(),
                    audioSettings = null,
                )
            }
        }
        composeRule.onNodeWithTag("neural_hub_screen").assertIsDisplayed()
        composeRule.onNodeWithTag("neural_hub_empty").assertIsDisplayed()
        composeRule.onNodeWithTag("neural_hub_new").performClick()
        composeRule.onNodeWithTag("neural_create_screen").assertIsDisplayed()
    }

    @Test
    fun create_generateWithoutModel_routesToSetup() {
        val next = routeAfterGenerateRequest(
            packId = packId,
            promptId = "mg_01",
            durationSec = 60,
            modelReady = false,
        )
        assertTrue(next is NeuralMusicRoute.ModelSetup)
    }

    @Test
    fun hub_showsExistingTake() {
        val lib = InMemoryNeuralTakeLibrary()
        lib.save(
            NeuralTake(
                id = "take-1",
                packId = packId,
                promptId = "mg_01",
                durationSec = 60,
                createdAtEpochMs = 1L,
                localPath = "/tmp/x.wav",
            ),
        )
        composeRule.setContent {
            ArthurTheme {
                NeuralHubScreen(
                    packId = packId,
                    takeLibrary = lib,
                    onOpenCreate = {},
                    onOpenTake = {},
                    onDismiss = {},
                )
            }
        }
        composeRule.onNodeWithTag("neural_hub_list").assertIsDisplayed()
        composeRule.onNodeWithTag("neural_hub_take_take-1").assertIsDisplayed()
    }

    @Test
    fun host_backFromCreate_returnsHub() {
        var route: NeuralMusicRoute? by mutableStateOf(NeuralMusicRoute.Create(packId))
        composeRule.setContent {
            ArthurTheme {
                NeuralMusicHost(
                    route = route!!,
                    onRouteChange = { route = it },
                    takeLibrary = InMemoryNeuralTakeLibrary(),
                    audioSettings = null,
                )
            }
        }
        composeRule.onNodeWithTag("neural_create_back").performClick()
        assertEquals(NeuralMusicRoute.Hub(packId), route)
    }
}

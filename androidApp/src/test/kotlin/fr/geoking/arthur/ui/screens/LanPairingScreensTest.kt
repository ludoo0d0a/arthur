package fr.geoking.arthur.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import fr.geoking.arthur.pairing.LanPairingPrefs
import fr.geoking.arthur.phone.theme.ArthurTheme
import fr.geoking.arthur.shared.domain.Artwork
import fr.geoking.arthur.shared.domain.ArtworkKind
import fr.geoking.arthur.shared.domain.PreparedRotation
import fr.geoking.arthur.shared.domain.Source
import fr.geoking.arthur.shared.engine.ContentEngine
import fr.geoking.arthur.shared.marketplace.PackOwnership
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LanPairingScreensTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun phoneLanPairingScreen_rendersHostField() {
        val engine = ContentEngine(
            sources = listOf(object : Source {
                override val id = "bundled"
                override val displayName = "Bundled"
                override suspend fun load(): List<Artwork> = listOf(
                    Artwork(
                        id = "1",
                        title = "One",
                        sourceId = "bundled",
                        kind = ArtworkKind.Photo,
                        remoteUrl = "https://example.com/1.jpg",
                    ),
                )
            }),
            packOwnership = PackOwnership.NONE,
        )
        val prefs = LanPairingPrefs(RuntimeEnvironment.getApplication())
        composeTestRule.setContent {
            ArthurTheme {
                PhoneLanPairingScreen(
                    contentEngine = engine,
                    pairingPrefs = prefs,
                    onDismiss = {},
                )
            }
        }
        composeTestRule.onNodeWithTag("phone_lan_pairing_screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("phone_lan_pairing_host").assertIsDisplayed()
        composeTestRule.onNodeWithTag("phone_lan_pairing_push").assertIsDisplayed()
    }

    @Test
    fun tvLanPairingHostScreen_rendersWaiting() {
        composeTestRule.setContent {
            ArthurTheme {
                TvLanPairingHostScreen(
                    onDismiss = {},
                    onRotationReceived = {},
                    port = 18744,
                )
            }
        }
        composeTestRule.onNodeWithTag("tv_lan_pairing_screen").assertIsDisplayed()
    }

    @Test
    fun contentEngine_catalog_forPairingPush_isNonEmptyWhenSourceHasArt() = kotlinx.coroutines.runBlocking {
        val engine = ContentEngine(
            sources = listOf(object : Source {
                override val id = "bundled"
                override val displayName = "Bundled"
                override suspend fun load(): List<Artwork> = listOf(
                    Artwork(
                        id = "1",
                        title = "One",
                        sourceId = "bundled",
                        kind = ArtworkKind.Photo,
                        remoteUrl = "https://example.com/1.jpg",
                    ),
                )
            }),
            packOwnership = PackOwnership.NONE,
        )
        val catalog = engine.catalog(PreparedRotation(sourceIds = emptyList(), artworkIds = emptyList()))
        assert(artworksForPairingPush(catalog).isNotEmpty())
    }
}

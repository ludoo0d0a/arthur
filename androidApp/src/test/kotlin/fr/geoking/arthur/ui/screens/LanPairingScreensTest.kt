package fr.geoking.arthur.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.assertCountEquals
import fr.geoking.arthur.pairing.DiscoveredPairingTv
import fr.geoking.arthur.pairing.LanPairingPrefs
import fr.geoking.arthur.pairing.PairingTvDiscovery
import fr.geoking.arthur.phone.theme.ArthurTheme
import fr.geoking.arthur.shared.domain.Artwork
import fr.geoking.arthur.shared.domain.ArtworkKind
import fr.geoking.arthur.shared.domain.PreparedRotation
import fr.geoking.arthur.shared.domain.Source
import fr.geoking.arthur.shared.engine.ContentEngine
import fr.geoking.arthur.shared.marketplace.PackOwnership
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    fun phoneLanPairingScreen_showsSyncAndScan_withoutHostField() {
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
        val discovery = FakePairingTvDiscovery(
            listOf(DiscoveredPairingTv("Arthur TV", "192.168.1.10", 8742)),
        )
        composeTestRule.setContent {
            ArthurTheme {
                PhoneLanPairingScreen(
                    contentEngine = engine,
                    pairingPrefs = prefs,
                    onDismiss = {},
                    discovery = discovery,
                )
            }
        }
        composeTestRule.onNodeWithTag("phone_lan_pairing_screen").assertIsDisplayed()
        composeTestRule.onAllNodesWithTag("phone_lan_pairing_host").assertCountEquals(0)
        composeTestRule.onNodeWithTag("phone_lan_pairing_push").assertIsDisplayed()
        composeTestRule.onNodeWithTag("phone_lan_pairing_scan").assertIsDisplayed()
        composeTestRule.onNodeWithTag("phone_lan_pairing_tv_192.168.1.10").assertIsDisplayed()
    }

    @Test
    fun tvLanPairingHostScreen_rendersWaitingAndQrTagWhenPossible() {
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

private class FakePairingTvDiscovery(
    initial: List<DiscoveredPairingTv>,
) : PairingTvDiscovery {
    private val _tvs = MutableStateFlow(initial)
    override val tvs: StateFlow<List<DiscoveredPairingTv>> = _tvs.asStateFlow()
    override fun start() = Unit
    override fun stop() = Unit
}

package fr.geoking.arthur.phone

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import fr.geoking.arthur.shared.domain.Artwork
import fr.geoking.arthur.shared.domain.ArtworkKind
import fr.geoking.arthur.ui.components.ArtworkRenderer
import java.util.Base64
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okio.Buffer
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Regression coverage for museum (Rijksmuseum) artwork pictures: they must be fetched
 * and displayed for real in the preview / Ambient surface, not silently swapped for the
 * generic still placeholder.
 */
@RunWith(AndroidJUnit4::class)
class MuseumPictureAvailabilityTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.close()
    }

    @Test
    fun museumPicture_isFetchedAndDisplayed_whenAvailable() {
        server.enqueue(
            MockResponse.Builder()
                .body(Buffer().write(ONE_PIXEL_PNG))
                .build(),
        )
        val artwork = museumArtwork(remoteUrl = server.url("/artwork.png").toString())

        composeRule.setContent {
            ArtworkRenderer(artwork = artwork, isActive = true)
        }

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("artwork_remote_image").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("artwork_remote_image").assertIsDisplayed()
        composeRule.onNodeWithTag("artwork_placeholder").assertDoesNotExist()
    }

    @Test
    fun museumPicture_fallsBackToPlaceholder_whenUnavailable() {
        server.enqueue(MockResponse.Builder().code(404).build())
        val artwork = museumArtwork(remoteUrl = server.url("/missing.png").toString())

        composeRule.setContent {
            ArtworkRenderer(artwork = artwork, isActive = true)
        }

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("artwork_placeholder").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("artwork_placeholder").assertIsDisplayed()
        composeRule.onNodeWithTag("artwork_remote_image").assertDoesNotExist()
    }

    private fun museumArtwork(remoteUrl: String) = Artwork(
        id = "rijks-test",
        title = "Test Painting",
        attribution = "Rijksmuseum",
        sourceId = "rijksmuseum",
        kind = ArtworkKind.Painting,
        remoteUrl = remoteUrl,
    )

    companion object {
        // 1x1 transparent PNG used as a stand-in for a real museum picture.
        private val ONE_PIXEL_PNG: ByteArray = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=",
        )
    }
}

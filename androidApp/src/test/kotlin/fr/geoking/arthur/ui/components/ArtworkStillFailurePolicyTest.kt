package fr.geoking.arthur.ui.components

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import fr.geoking.arthur.phone.theme.ArthurTheme
import fr.geoking.arthur.shared.domain.Artwork
import fr.geoking.arthur.shared.domain.ArtworkKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class ArtworkStillFailurePolicyTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun silentFailure_neverShowsRetry_andInvokesOnStillFailed() {
        var failedCount = 0
        // No path/url → immediate terminal failure (no network).
        val artwork = Artwork(
            id = "silent-fail",
            title = "silent-fail",
            sourceId = "test",
            kind = ArtworkKind.Photo,
        )

        composeTestRule.setContent {
            ArthurTheme {
                ArtworkRenderer(
                    artwork = artwork,
                    isActive = true,
                    silentFailure = true,
                    onStillFailed = { failedCount++ },
                )
            }
        }

        composeTestRule.waitUntil(timeoutMillis = 5_000) { failedCount >= 1 }
        assertEquals(1, failedCount)
        assertTrue(
            composeTestRule.onAllNodesWithTag("artwork_retry_button")
                .fetchSemanticsNodes()
                .isEmpty(),
        )
    }

    @Test
    fun withoutSilentFailure_showsUnavailableUi() {
        val artwork = Artwork(
            id = "retry-ui",
            title = "retry-ui",
            sourceId = "test",
            kind = ArtworkKind.Photo,
        )

        composeTestRule.setContent {
            ArthurTheme {
                ArtworkRenderer(
                    artwork = artwork,
                    isActive = true,
                    silentFailure = false,
                )
            }
        }

        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithTag("artwork_unavailable_message")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeTestRule.onNodeWithTag("artwork_unavailable_message").assertIsDisplayed()
    }

    @Test
    fun transitionHost_keepsLastGoodVisibleWhileNextFailsSilently() {
        var failedCount = 0
        var target by mutableStateOf(goodLocalStill("good-a"))

        composeTestRule.setContent {
            ArthurTheme {
                ArtworkTransitionHost(
                    artwork = target,
                    isActive = true,
                    silentFailure = true,
                    onStillFailed = { failedCount++ },
                )
            }
        }

        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithTag("artwork_remote_image")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        composeTestRule.runOnIdle {
            target = Artwork(
                id = "bad-b",
                title = "bad-b",
                sourceId = "test",
                kind = ArtworkKind.Photo,
            )
        }
        composeTestRule.waitForIdle()
        composeTestRule.mainClock.advanceTimeBy(500)

        composeTestRule.waitUntil(timeoutMillis = 5_000) { failedCount >= 1 }
        assertTrue(
            composeTestRule.onAllNodesWithTag("artwork_retry_button")
                .fetchSemanticsNodes()
                .isEmpty(),
        )
        // Previous successful still remains on screen (lastGood).
        composeTestRule.onNodeWithTag("artwork_remote_image").assertIsDisplayed()
    }

    private fun goodLocalStill(id: String): Artwork {
        val tempFile = java.io.File.createTempFile("art_$id", ".png").apply {
            deleteOnExit()
            java.io.FileOutputStream(this).use { out ->
                val bmp = android.graphics.Bitmap.createBitmap(
                    32,
                    32,
                    android.graphics.Bitmap.Config.ARGB_8888,
                )
                bmp.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out)
            }
        }
        return Artwork(
            id = id,
            title = id,
            sourceId = "test",
            kind = ArtworkKind.Photo,
            localPath = tempFile.absolutePath,
        )
    }
}

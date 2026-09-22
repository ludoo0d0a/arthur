package fr.geoking.arthur.ui.components

import android.graphics.Bitmap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import fr.geoking.arthur.phone.theme.ArthurTheme
import fr.geoking.arthur.shared.domain.Artwork
import fr.geoking.arthur.shared.domain.ArtworkKind
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.File
import java.io.FileOutputStream

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class ArtworkRendererTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun stillArtwork_rendersSmartScaledImageAndBlurredBackground() {
        val tempFile = File.createTempFile("test_art", ".png").apply {
            deleteOnExit()
            FileOutputStream(this).use { out ->
                val bmp = Bitmap.createBitmap(100, 50, Bitmap.Config.ARGB_8888)
                bmp.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
        }

        val artwork = Artwork(
            id = "test-landscape-photo",
            title = "Wide Landscape",
            sourceId = "test",
            kind = ArtworkKind.Photo,
            localPath = tempFile.absolutePath,
        )

        composeTestRule.setContent {
            ArthurTheme {
                ArtworkRenderer(artwork = artwork, isActive = true)
            }
        }

        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithTag("artwork_remote_image").fetchSemanticsNodes().isNotEmpty()
        }

        composeTestRule.onNodeWithTag("artwork_remote_image_container").assertIsDisplayed()
        composeTestRule.onNodeWithTag("artwork_remote_image_bg").assertIsDisplayed()
        composeTestRule.onNodeWithTag("artwork_remote_image").assertIsDisplayed()
    }
}

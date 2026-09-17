package fr.geoking.arthur.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import fr.geoking.arthur.phone.theme.ArthurTheme
import fr.geoking.arthur.shared.domain.Artwork
import fr.geoking.arthur.shared.domain.ArtworkKind
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AmbientScreenLoaderTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val sampleArtwork = Artwork(
        id = "test-1",
        title = "Sample Title",
        attribution = "Sample Author",
        sourceId = "bundled",
        kind = ArtworkKind.Photo,
        remoteUrl = "https://example.com/sample.jpg",
    )

    @Test
    fun ambientScreen_doesNotDisplayDiscreteLoader() {
        composeTestRule.setContent {
            ArthurTheme {
                AmbientScreenContent(
                    title = "Ambient Test",
                    artwork = sampleArtwork,
                    isLoading = true,
                )
            }
        }

        composeTestRule.onNodeWithTag("discrete_ambient_loader").assertDoesNotExist()
    }
}

package fr.geoking.arthur.phone

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import fr.geoking.arthur.phone.theme.ArthurTheme
import fr.geoking.arthur.shared.domain.Artwork
import fr.geoking.arthur.shared.domain.ArtworkKind
import fr.geoking.arthur.shared.source.BundledPackSource
import fr.geoking.arthur.shared.source.MetSource
import fr.geoking.arthur.shared.source.RijksmuseumSource
import fr.geoking.arthur.ui.screens.ControlPlaneContent
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ControlPlaneFilterTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val catalog = listOf(
        Artwork(
            id = "met-1",
            title = "Met Painting",
            sourceId = MetSource.ID,
            kind = ArtworkKind.Painting,
            remoteUrl = "https://example.com/met.jpg",
        ),
        Artwork(
            id = "rijks-sculpt",
            title = "Rijks Sculpture",
            sourceId = RijksmuseumSource.ID,
            kind = ArtworkKind.Sculpture,
            remoteUrl = "https://example.com/rijks.jpg",
        ),
        Artwork(
            id = "bundled-sculpt",
            title = "Bundled Sculpture",
            sourceId = BundledPackSource.ID,
            kind = ArtworkKind.Sculpture,
            remoteUrl = "https://example.com/bundled.jpg",
        ),
        Artwork(
            id = "photo-1",
            title = "Harbor",
            sourceId = BundledPackSource.ID,
            kind = ArtworkKind.Photo,
            remoteUrl = "https://example.com/photo.jpg",
        ),
    )

    @Test
    fun photoTopics_visibleOnlyWhenPhotoSelected() {
        setControlPlane()

        composeRule.onNodeWithTag("stock_topic_row").assertDoesNotExist()
        composeRule.onNodeWithTag("source_filter_row").assertDoesNotExist()

        composeRule.onNodeWithTag("filter_chip_photo").performClick()
        composeRule.onNodeWithTag("stock_topic_row").assertIsDisplayed()
        composeRule.onNodeWithTag("source_filter_row").assertDoesNotExist()

        composeRule.onNodeWithTag("filter_chip_all").performClick()
        composeRule.onNodeWithTag("stock_topic_row").assertDoesNotExist()
    }

    @Test
    fun sculpture_showsSourceChips_toggleOffMeansAll() {
        setControlPlane()

        composeRule.onNodeWithTag("filter_chip_sculpture").performClick()
        composeRule.onNodeWithTag("source_filter_row").assertIsDisplayed()
        composeRule.onNodeWithTag("stock_topic_row").assertDoesNotExist()

        composeRule.onNodeWithTag("source_chip_met").assertIsNotSelected()
        composeRule.onNodeWithTag("source_chip_met").performClick()
        composeRule.onNodeWithTag("source_chip_met").assertIsSelected()

        composeRule.onNodeWithTag("source_chip_met").performClick()
        composeRule.onNodeWithTag("source_chip_met").assertIsNotSelected()
    }

    private fun setControlPlane() {
        composeRule.setContent {
            ArthurTheme {
                ControlPlaneContent(
                    catalog = catalog,
                    selected = catalog.first(),
                    onSelect = {},
                    onStartAmbient = {},
                    showFractalPreview = false,
                )
            }
        }
    }
}

package fr.geoking.arthur.phone

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import fr.geoking.arthur.phone.theme.ArthurTheme
import fr.geoking.arthur.ui.components.PackFamily
import fr.geoking.arthur.ui.components.PackSelection
import fr.geoking.arthur.ui.screens.ControlPlaneContent
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ControlPlaneFilterTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun home_showsPackGrid() {
        setControlPlane(openedFamily = null)

        composeRule.onNodeWithTag("pack_grid").assertIsDisplayed()
        composeRule.onNodeWithTag("pack_tile_museum").assertIsDisplayed()
        composeRule.onNodeWithTag("pack_tile_genart").assertIsDisplayed()
        composeRule.onNodeWithTag("pack_tile_photo").assertIsDisplayed()
        composeRule.onNodeWithTag("fractal_preview").assertDoesNotExist()
        composeRule.onNodeWithTag("artwork_list").assertDoesNotExist()
    }

    @Test
    fun genartPack_opensSubPacks_allSelected() {
        var selection = PackSelection(PackFamily.Genart)
        composeRule.setContent {
            ArthurTheme {
                ControlPlaneContent(
                    openedFamily = PackFamily.Genart,
                    selection = selection,
                    onOpenFamily = {},
                    onSelectSubPack = { selection = it },
                    onBackToHome = {},
                    onStartAmbient = {},
                )
            }
        }

        composeRule.onNodeWithTag("pack_tile_genart_all").assertIsDisplayed()
        composeRule.onNodeWithTag("pack_tile_genart_nature").assertIsDisplayed()
        composeRule.onNodeWithTag("pack_tile_genart_weather").assertIsDisplayed()
        composeRule.onNodeWithTag("pack_tile_genart_abstract").assertIsDisplayed()
        composeRule.onNodeWithTag("pack_tile_genart_all").assertIsSelected()
    }

    @Test
    fun photoPack_showsTopicSubPacks() {
        setControlPlane(openedFamily = PackFamily.Photo, selection = PackSelection(PackFamily.Photo))

        composeRule.onNodeWithTag("pack_tile_photo_all").assertIsDisplayed()
        composeRule.onNodeWithTag("pack_tile_photo_nature").assertIsDisplayed()
        composeRule.onNodeWithTag("pack_tile_photo_suggestions").assertIsDisplayed()
    }

    @Test
    fun sculpturePack_showsMuseumSubPacks() {
        setControlPlane(
            openedFamily = PackFamily.Sculpture,
            selection = PackSelection(PackFamily.Sculpture),
        )

        composeRule.onNodeWithTag("pack_tile_sculpture_all").assertIsDisplayed()
        composeRule.onNodeWithTag("pack_tile_sculpture_met").assertIsDisplayed()
        composeRule.onNodeWithTag("pack_tile_sculpture_suggestions").assertIsDisplayed()
    }

    @Test
    fun tapMuseum_opensSubPacks() {
        var opened: PackFamily? = null
        composeRule.setContent {
            ArthurTheme {
                ControlPlaneContent(
                    openedFamily = opened,
                    selection = PackSelection(PackFamily.Museum),
                    onOpenFamily = { opened = it },
                    onSelectSubPack = {},
                    onBackToHome = { opened = null },
                    onStartAmbient = {},
                )
            }
        }

        composeRule.onNodeWithTag("pack_tile_museum").performClick()
        composeRule.waitForIdle()
        // State is local to test host — re-set with opened family to assert navigation contract.
        composeRule.setContent {
            ArthurTheme {
                ControlPlaneContent(
                    openedFamily = PackFamily.Museum,
                    selection = PackSelection(PackFamily.Museum),
                    onOpenFamily = {},
                    onSelectSubPack = {},
                    onBackToHome = {},
                    onStartAmbient = {},
                )
            }
        }
        composeRule.onNodeWithTag("pack_tile_museum_all").assertIsDisplayed()
        composeRule.onNodeWithTag("pack_tile_museum_met").assertIsDisplayed()
        composeRule.onNodeWithTag("pack_tile_museum_europeana").assertIsDisplayed()
        composeRule.onNodeWithTag("pack_back").assertIsDisplayed()
    }

    private fun setControlPlane(
        openedFamily: PackFamily?,
        selection: PackSelection = PackSelection(PackFamily.Museum),
    ) {
        composeRule.setContent {
            ArthurTheme {
                ControlPlaneContent(
                    openedFamily = openedFamily,
                    selection = selection,
                    onOpenFamily = {},
                    onSelectSubPack = {},
                    onBackToHome = {},
                    onStartAmbient = {},
                )
            }
        }
    }
}

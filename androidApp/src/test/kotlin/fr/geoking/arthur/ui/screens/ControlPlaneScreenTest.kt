package fr.geoking.arthur.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import fr.geoking.arthur.phone.theme.ArthurTheme
import fr.geoking.arthur.ui.components.PackFamily
import fr.geoking.arthur.ui.components.PackSelection
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ControlPlaneScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun controlPlane_displaysMediaPlayerFab_andTriggersCallback() {
        var clicked = false
        composeTestRule.setContent {
            ArthurTheme {
                ControlPlaneContent(
                    openedFamily = null,
                    selection = PackSelection(PackFamily.Museum),
                    onOpenFamily = {},
                    onSelectSubPack = {},
                    onBackToHome = {},
                    onStartAmbient = {},
                    onOpenMediaPlayer = { clicked = true },
                )
            }
        }

        composeTestRule.onNodeWithTag("media_player_fab").assertIsDisplayed()
        composeTestRule.onNodeWithTag("media_player_fab").performClick()
        assertTrue(clicked)
    }
}

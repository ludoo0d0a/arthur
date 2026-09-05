package fr.geoking.arthur.phone

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ControlPlaneSmokeTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun controlPlane_showsTitleAndPackGrid() {
        composeRule.onNodeWithTag("control_plane_title").assertIsDisplayed()
        composeRule.onNodeWithTag("pack_grid").assertIsDisplayed()
        composeRule.onNodeWithTag("pack_tile_museum").assertIsDisplayed()
        composeRule.onNodeWithTag("start_ambient").assertIsDisplayed()
        composeRule.onNodeWithTag("fractal_preview").assertDoesNotExist()
        composeRule.onNodeWithTag("artwork_list").assertDoesNotExist()
    }
}

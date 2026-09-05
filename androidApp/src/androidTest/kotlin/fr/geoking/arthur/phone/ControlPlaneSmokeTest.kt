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
    fun controlPlane_showsTitleAndList() {
        composeRule.onNodeWithTag("control_plane_title").assertIsDisplayed()
        composeRule.onNodeWithTag("category_filter_row").assertIsDisplayed()
        composeRule.onNodeWithTag("artwork_list").assertIsDisplayed()
        composeRule.onNodeWithTag("start_ambient").assertIsDisplayed()
        // Sublevel rows are context-dependent; hidden on default All.
        composeRule.onNodeWithTag("stock_topic_row").assertDoesNotExist()
        composeRule.onNodeWithTag("museum_topic_row").assertDoesNotExist()
        composeRule.onNodeWithTag("genart_topic_row").assertDoesNotExist()
    }
}

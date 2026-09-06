package fr.geoking.arthur.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import fr.geoking.arthur.phone.theme.ArthurTheme
import fr.geoking.arthur.shared.error.ErrorCategory
import fr.geoking.arthur.shared.error.ErrorLogger
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SettingsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun settingsScreen_whenShowDeveloperTrue_showsDeveloperMenu() {
        composeTestRule.setContent {
            ArthurTheme {
                SettingsScreen(
                    onDismiss = {},
                    showDeveloper = true,
                )
            }
        }

        composeTestRule.onNodeWithText("Developer").assertIsDisplayed()
    }

    @Test
    fun settingsScreen_whenShowDeveloperFalse_hidesDeveloperMenu() {
        composeTestRule.setContent {
            ArthurTheme {
                SettingsScreen(
                    onDismiss = {},
                    showDeveloper = false,
                )
            }
        }

        composeTestRule.onNodeWithText("Developer").assertDoesNotExist()
    }

    @Test
    fun developerErrorLogScreen_rendersAndClearsErrors() {
        val errorLogger = ErrorLogger(clock = { 1000L })
        errorLogger.log(
            sourceId = "europeana",
            message = "Authentication failure 401",
            category = ErrorCategory.Authentication,
            statusCode = 401,
        )

        assertEquals(1, errorLogger.errors.value.size)

        composeTestRule.setContent {
            ArthurTheme {
                DeveloperErrorLogScreen(errorLogger = errorLogger)
            }
        }

        composeTestRule.onNodeWithTag("developer_error_log_screen").assertIsDisplayed()
        composeTestRule.onNodeWithText("Authentication failure 401").assertIsDisplayed()

        composeTestRule.onNodeWithTag("btn_clear_all_errors").performClick()

        assertEquals(0, errorLogger.errors.value.size)
    }
}

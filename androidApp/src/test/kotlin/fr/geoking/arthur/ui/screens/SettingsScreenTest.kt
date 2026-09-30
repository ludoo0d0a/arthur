package fr.geoking.arthur.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import android.content.Context
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import fr.geoking.arthur.R
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
@Config(sdk = [34], qualifiers = "en")
class SettingsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun settingsScreen_whenShowDeveloperTrue_showsDeveloperMenu() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val devTitle = context.getString(R.string.screen_developer)
        composeTestRule.setContent {
            ArthurTheme {
                SettingsScreen(
                    onDismiss = {},
                    showDeveloper = true,
                )
            }
        }

        composeTestRule.onNodeWithText(devTitle).assertExists()
    }

    @Test
    fun settingsScreen_whenShowDeveloperFalse_hidesDeveloperMenu() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val devTitle = context.getString(R.string.screen_developer)
        composeTestRule.setContent {
            ArthurTheme {
                SettingsScreen(
                    onDismiss = {},
                    showDeveloper = false,
                )
            }
        }

        composeTestRule.onNodeWithText(devTitle).assertDoesNotExist()
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

        val errId = errorLogger.errors.value.first().id
        composeTestRule.onNodeWithTag("btn_copy_error_$errId").assertIsDisplayed()

        composeTestRule.onNodeWithTag("btn_clear_all_errors").performClick()

        assertEquals(0, errorLogger.errors.value.size)
    }

    @Test
    fun developerErrorLogScreen_filtersByCategoryAndSource() {
        val errorLogger = ErrorLogger(clock = { 1000L })
        errorLogger.log(
            sourceId = "europeana",
            message = "Europeana Auth Error",
            category = ErrorCategory.Authentication,
        )
        errorLogger.log(
            sourceId = "rijksmuseum",
            message = "Rijksmuseum Rate Limit",
            category = ErrorCategory.RateLimit,
        )

        composeTestRule.setContent {
            ArthurTheme {
                DeveloperErrorLogScreen(errorLogger = errorLogger)
            }
        }

        composeTestRule.onNodeWithText("Europeana Auth Error").assertIsDisplayed()
        composeTestRule.onNodeWithText("Rijksmuseum Rate Limit").assertIsDisplayed()

        composeTestRule.onNodeWithTag("chip_category_Authentication").performClick()
        composeTestRule.onNodeWithText("Europeana Auth Error").assertIsDisplayed()
        composeTestRule.onNodeWithText("Rijksmuseum Rate Limit").assertDoesNotExist()

        composeTestRule.onNodeWithTag("chip_category_all").performClick()
        composeTestRule.onNodeWithTag("chip_source_rijksmuseum").performClick()
        composeTestRule.onNodeWithText("Rijksmuseum Rate Limit").assertIsDisplayed()
        composeTestRule.onNodeWithText("Europeana Auth Error").assertDoesNotExist()
    }

    @Test
    fun developerContent_showsVerboseToggle() {
        var verboseState = false
        composeTestRule.setContent {
            ArthurTheme {
                SettingsScreen(
                    onDismiss = {},
                    showDeveloper = true,
                    verbose = verboseState,
                    onVerboseChange = { verboseState = it },
                    initialScreenStack = listOf(SettingsScreenPage.Developer),
                )
            }
        }

        composeTestRule.onNodeWithTag("dev_verbose").assertIsDisplayed()
        composeTestRule.onNodeWithTag("dev_verbose").performClick()
        assertEquals(true, verboseState)
    }

    @Test
    fun developerContent_showsSimulateAllPacksToggle() {
        var simulateAllPacksState = false
        composeTestRule.setContent {
            ArthurTheme {
                SettingsScreen(
                    onDismiss = {},
                    showDeveloper = true,
                    simulateAllPacks = simulateAllPacksState,
                    onSimulateAllPacksChange = { simulateAllPacksState = it },
                    initialScreenStack = listOf(SettingsScreenPage.Developer),
                )
            }
        }

        composeTestRule.onNodeWithTag("dev_simulate_all_packs").assertIsDisplayed()
        composeTestRule.onNodeWithTag("dev_simulate_all_packs").performClick()
        assertEquals(true, simulateAllPacksState)
    }
}

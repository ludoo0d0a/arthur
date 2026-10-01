package fr.geoking.arthur.ui.screens

import android.content.Context
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.core.app.ApplicationProvider
import fr.geoking.arthur.phone.theme.ArthurTheme
import fr.geoking.arthur.shared.domain.Artwork
import fr.geoking.arthur.shared.domain.ArtworkKind
import fr.geoking.arthur.source.AmbientAudioSettings
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AmbientScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val sampleArtwork = Artwork(
        id = "test-art-1",
        title = "Sample Artwork",
        attribution = "Artist Name",
        sourceId = "genart",
        kind = ArtworkKind.Genart,
    )

    @Test
    fun ambientScreen_whenMediaPlayerActive_displaysProgressBar() {
        stopKoin()
        val context = ApplicationProvider.getApplicationContext<Context>()
        val audioSettings = AmbientAudioSettings(context)
        audioSettings.setEnabled(true)
        startKoin {
            modules(
                module {
                    single { audioSettings }
                },
            )
        }
        try {
            composeTestRule.setContent {
                ArthurTheme {
                    AmbientScreenContent(
                        title = "Ambient Test",
                        artwork = sampleArtwork,
                        rotationPool = listOf(
                            sampleArtwork,
                            sampleArtwork.copy(id = "test-art-2", title = "Artwork 2"),
                        ),
                        isActive = true,
                    )
                }
            }

            composeTestRule.onNodeWithTag("ambient_media_player").assertExists()
            composeTestRule.onNodeWithTag("ambient_media_progress").assertExists()
        } finally {
            stopKoin()
        }
    }
}

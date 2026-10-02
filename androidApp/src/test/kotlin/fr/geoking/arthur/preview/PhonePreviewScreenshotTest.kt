package fr.geoking.arthur.preview

import android.app.Application
import fr.geoking.arthur.phone.theme.ArthurTheme
import fr.geoking.arthur.shared.domain.Artwork
import fr.geoking.arthur.shared.domain.ArtworkKind
import fr.geoking.arthur.shared.source.BundledPackSource
import fr.geoking.arthur.ui.components.PackFamily
import fr.geoking.arthur.ui.components.PackSelection
import fr.geoking.arthur.ui.screens.AmbientScreenContent
import fr.geoking.arthur.ui.screens.ControlPlaneContent
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * Renders key Arthur phone screens to PNGs via Robolectric + Roborazzi.
 *
 * Run: `./gradlew :androidApp:generatePhoneScreenshots -PscreenshotLocales=en,fr`
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    sdk = [33],
    application = Application::class,
    qualifiers = "w411dp-h891dp-xxhdpi",
)
class PhonePreviewScreenshotTest {
    /**
     * Pre-baked genart still under repo `screenshots/` — local file so Roborazzi
     * stays offline and does not hang on live GenartEffectCanvas animation.
     */
    private val ambientArtwork: Artwork by lazy {
        val still = listOf(
            File("screenshots/#13-Aurora Ribbons.png"),
            File("../screenshots/#13-Aurora Ribbons.png"),
        ).firstOrNull { it.isFile }?.absoluteFile
        check(still != null) { "Missing ambient still screenshots/#13-Aurora Ribbons.png" }
        Artwork(
            id = "bundled-ambient-preview",
            title = "Aurora Ribbons",
            attribution = "Arthur Genart",
            sourceId = BundledPackSource.ID,
            kind = ArtworkKind.Painting,
            localPath = still.absolutePath,
        )
    }

    @Test
    fun control_plane() {
        PhonePreviewScreenshotCapture.capture("control_plane.png") {
            ArthurTheme {
                ControlPlaneContent(
                    openedFamily = null,
                    selection = PackSelection(PackFamily.Museum),
                    onOpenFamily = {},
                    onSelectSubPack = {},
                    onBackToHome = {},
                    onStartAmbient = {},
                )
            }
        }
    }

    @Test
    fun control_plane_sources() {
        PhonePreviewScreenshotCapture.capture("control_plane_sources.png") {
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
    }

    @Test
    fun ambient() {
        PhonePreviewScreenshotCapture.capture("ambient.png") {
            ArthurTheme {
                AmbientScreenContent(
                    title = ambientArtwork.title,
                    artwork = ambientArtwork,
                )
            }
        }
    }
}

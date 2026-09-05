package fr.geoking.arthur.preview

import android.app.Application
import fr.geoking.arthur.phone.theme.ArthurTheme
import fr.geoking.arthur.ui.components.PackFamily
import fr.geoking.arthur.ui.components.PackSelection
import fr.geoking.arthur.ui.screens.AmbientScreenContent
import fr.geoking.arthur.ui.screens.ControlPlaneContent
import fr.geoking.arthur.shared.source.BundledPackSource
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

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
    private val catalog = BundledPackSource.defaultPack()

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
                    openedFamily = PackFamily.Genart,
                    selection = PackSelection(PackFamily.Genart),
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
                AmbientScreenContent(title = catalog.first().title)
            }
        }
    }
}

package fr.geoking.arthur.preview

import android.app.Application
import fr.geoking.arthur.phone.theme.ArthurTheme
import fr.geoking.arthur.ui.screens.AmbientScreenContent
import fr.geoking.arthur.ui.screens.ControlPlaneContent
import fr.geoking.arthur.shared.domain.Artwork
import fr.geoking.arthur.shared.domain.ArtworkKind
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
    private val catalog: List<Artwork> = BundledPackSource.defaultPack()

    @Test
    fun control_plane() {
        PhonePreviewScreenshotCapture.capture("control_plane.png") {
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

    @Test
    fun control_plane_sources() {
        PhonePreviewScreenshotCapture.capture("control_plane_sources.png") {
            ArthurTheme {
                ControlPlaneContent(
                    catalog = catalog + Artwork(
                        id = "demo-fractal",
                        title = "Mandelbrot preset",
                        attribution = "Arthur",
                        sourceId = BundledPackSource.ID,
                        kind = ArtworkKind.FractalPreset,
                    ),
                    selected = null,
                    onSelect = {},
                    onStartAmbient = {},
                    showFractalPreview = false,
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

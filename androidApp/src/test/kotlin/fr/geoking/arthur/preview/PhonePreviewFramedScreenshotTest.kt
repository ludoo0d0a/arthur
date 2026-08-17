package fr.geoking.arthur.preview

import android.app.Application
import fr.geoking.arthur.phone.AmbientScreenContent
import fr.geoking.arthur.phone.ControlPlaneContent
import fr.geoking.arthur.phone.theme.ArthurTheme
import fr.geoking.arthur.shared.source.BundledPackSource
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Framed phone screenshots (device chassis) — Scora-aligned.
 *
 * Run: `./gradlew :androidApp:generatePhoneScreenshotsFramed -PscreenshotLocales=en,fr`
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    sdk = [33],
    application = Application::class,
    qualifiers = PhoneFramedQualifiers,
)
class PhonePreviewFramedScreenshotTest {
    private val catalog = BundledPackSource.defaultPack()

    @Test
    fun control_plane_framed() {
        PhonePreviewScreenshotCapture.capture(
            fileName = "control_plane.png",
            withFrame = true,
        ) {
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
    fun ambient_framed() {
        PhonePreviewScreenshotCapture.capture(
            fileName = "ambient.png",
            withFrame = true,
        ) {
            ArthurTheme {
                AmbientScreenContent(title = catalog.first().title)
            }
        }
    }
}

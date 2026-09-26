package fr.geoking.arthur.auto

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Color
import fr.geoking.arthur.shared.domain.Artwork
import fr.geoking.arthur.shared.domain.ArtworkKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], application = Application::class)
class AmbientStillRendererTest {

    @Test
    fun extractGradientColors_extractsTopAndBottomTones() {
        // Create a 100x100 test bitmap with red top and blue bottom
        val testBmp = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        for (y in 0 until 50) {
            for (x in 0 until 100) {
                testBmp.setPixel(x, y, Color.RED)
            }
        }
        for (y in 50 until 100) {
            for (x in 0 until 100) {
                testBmp.setPixel(x, y, Color.BLUE)
            }
        }

        val (topColor, botColor) = AmbientStillRenderer.extractGradientColors(testBmp)

        // Verify topColor is derived from red and botColor from blue
        val topHsv = FloatArray(3)
        val botHsv = FloatArray(3)
        Color.colorToHSV(topColor, topHsv)
        Color.colorToHSV(botColor, botHsv)

        // Hue of red is 0 degrees, blue is 240 degrees
        assertEquals(0f, topHsv[0], 5f)
        assertEquals(240f, botHsv[0], 5f)
    }

    @Test
    fun render_stillImage_drawsFullBleedCover() {
        val tempFile = File.createTempFile("test_still_", ".png")
        try {
            val sampleBmp = Bitmap.createBitmap(200, 100, Bitmap.Config.ARGB_8888)
            sampleBmp.eraseColor(Color.GREEN)
            tempFile.outputStream().use { out ->
                sampleBmp.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            sampleBmp.recycle()

            val artwork = Artwork(
                id = "photo.test",
                title = "Test Photo",
                sourceId = "test",
                kind = ArtworkKind.Photo,
                localPath = tempFile.absolutePath,
            )

            val rendered = AmbientStillRenderer.render(artwork, generation = 1L)

            assertNotNull(rendered)
            assertEquals(AmbientStillRenderer.SIZE, rendered.width)
            assertEquals(AmbientStillRenderer.SIZE, rendered.height)
            // Landscape source is center-cropped to fill the square — no framed margins.
            assertEquals(Color.GREEN, rendered.getPixel(2, 2))
            assertEquals(
                Color.GREEN,
                rendered.getPixel(AmbientStillRenderer.SIZE / 2, AmbientStillRenderer.SIZE / 2),
            )
            rendered.recycle()
        } finally {
            tempFile.delete()
        }
    }

    @Test
    fun render_genart_isFullBleedSquare() {
        val artwork = Artwork(
            id = "genart.particles",
            title = "Particles",
            sourceId = "genart",
            kind = ArtworkKind.Genart,
        )
        val rendered = AmbientStillRenderer.render(artwork, generation = 1L)
        assertEquals(AmbientStillRenderer.SIZE, rendered.width)
        assertEquals(AmbientStillRenderer.SIZE, rendered.height)
        // Opaque bake fills the square (no transparent / framed margins).
        assertTrue(Color.alpha(rendered.getPixel(2, 2)) == 255)
        assertTrue(Color.alpha(rendered.getPixel(AmbientStillRenderer.SIZE - 3, AmbientStillRenderer.SIZE - 3)) == 255)
        rendered.recycle()
    }
}

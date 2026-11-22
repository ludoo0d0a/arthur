package fr.geoking.arthur.preview

import android.app.Application
import android.graphics.Bitmap
import fr.geoking.arthur.fractal.FractalStillRenderer
import fr.geoking.arthur.shared.source.FractalSource
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * Bakes one still preview PNG per fractal preset into [screenshot.outputDir]
 * (default: repo `screenshots/fractal/`), named `#N-Name.png` from the catalog title.
 *
 * Run: `./gradlew :androidApp:generateFractalScreenshots`
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], application = Application::class)
class FractalPreviewScreenshotTest {

    @Test
    fun generateAllFractalPreviews() {
        val outDir = File(
            System.getProperty("screenshot.outputDir")
                ?: error("screenshot.outputDir system property required"),
        )
        outDir.mkdirs()
        assertTrue("output dir must exist: $outDir", outDir.isDirectory)

        val size = System.getProperty("fractal.screenshot.size")?.toIntOrNull() ?: DEFAULT_SIZE
        val generation = System.getProperty("fractal.screenshot.generation")?.toLongOrNull()
            ?: DEFAULT_GENERATION

        val entries = FractalSource.defaultCatalog()
        assertTrue("expected fractal catalog entries", entries.isNotEmpty())

        var written = 0
        for (entry in entries) {
            val type = FractalStillRenderer.typeForArtworkId(entry.id)
                ?: error("Unknown fractal id: ${entry.id}")
            val parsed = parseTitle(entry.title)
                ?: error("Catalog title must be '#N - Name': ${entry.title}")
            val fileName = "#${parsed.number}-${parsed.name}.png"
            val file = File(outDir, fileName)

            val bitmap = FractalStillRenderer.render(
                type = type,
                size = size,
                generation = generation,
            )
            try {
                file.outputStream().use { out ->
                    assertTrue(
                        "compress failed for ${entry.id}",
                        bitmap.compress(Bitmap.CompressFormat.PNG, 92, out),
                    )
                }
            } finally {
                bitmap.recycle()
            }
            assertTrue("missing $file", file.isFile && file.length() > 0L)
            written++
        }
        assertTrue("wrote $written previews", written == entries.size)
        println("Wrote $written fractal previews → ${outDir.absolutePath}")
    }

    private data class ParsedTitle(val number: Int, val name: String)

    private fun parseTitle(title: String): ParsedTitle? {
        val match = TITLE_REGEX.matchEntire(title.trim()) ?: return null
        val number = match.groupValues[1].toIntOrNull() ?: return null
        val name = match.groupValues[2].trim()
        if (name.isEmpty()) return null
        val safeName = name.replace('/', '-').replace('\\', '-')
        return ParsedTitle(number, safeName)
    }

    companion object {
        private val TITLE_REGEX = Regex("""^#(\d+)\s*-\s*(.+)$""")
        private const val DEFAULT_SIZE = 720
        private const val DEFAULT_GENERATION = 7L
    }
}

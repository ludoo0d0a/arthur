package fr.geoking.arthur.preview

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import fr.geoking.arthur.genart.GenartCatalog
import fr.geoking.arthur.genart.GenartStillRenderer
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * Bakes one still preview PNG per genart engine into [screenshot.outputDir]
 * (default: repo `screenshots/`), named `#N-Name.png` from the catalog title.
 *
 * Run: `./gradlew :androidApp:generateGenartScreenshots`
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], application = Application::class)
class GenartPreviewScreenshotTest {

    @Test
    fun generateAllGenartPreviews() {
        val outDir = File(
            System.getProperty("screenshot.outputDir")
                ?: error("screenshot.outputDir system property required"),
        )
        outDir.mkdirs()
        assertTrue("output dir must exist: $outDir", outDir.isDirectory)

        val size = System.getProperty("genart.screenshot.size")?.toIntOrNull() ?: DEFAULT_SIZE
        val generation = System.getProperty("genart.screenshot.generation")?.toLongOrNull()
            ?: DEFAULT_GENERATION

        val entries = GenartCatalog.entries()
        assertTrue("expected genart catalog entries", entries.isNotEmpty())

        var written = 0
        for (entry in entries) {
            val parsed = parseTitle(entry.title)
                ?: error("Catalog title must be '#N - Name': ${entry.title}")
            val fileName = "#${parsed.number}-${parsed.name}.png"
            val file = File(outDir, fileName)

            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            try {
                GenartStillRenderer.draw(
                    canvas = Canvas(bitmap),
                    engineId = entry.engine,
                    size = size,
                    generation = generation,
                )
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
        println("Wrote $written genart previews → ${outDir.absolutePath}")
    }

    private data class ParsedTitle(val number: Int, val name: String)

    private fun parseTitle(title: String): ParsedTitle? {
        val match = TITLE_REGEX.matchEntire(title.trim()) ?: return null
        val number = match.groupValues[1].toIntOrNull() ?: return null
        val name = match.groupValues[2].trim()
        if (name.isEmpty()) return null
        // Keep spaces; strip path separators only
        val safeName = name.replace('/', '-').replace('\\', '-')
        return ParsedTitle(number, safeName)
    }

    companion object {
        private val TITLE_REGEX = Regex("""^#(\d+)\s*-\s*(.+)$""")
        private const val DEFAULT_SIZE = 720
        private const val DEFAULT_GENERATION = 7L
    }
}

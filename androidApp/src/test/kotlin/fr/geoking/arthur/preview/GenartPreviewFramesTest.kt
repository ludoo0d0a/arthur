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
import kotlin.math.PI
import kotlin.math.sin

/**
 * Bakes a PNG frame sequence per genart engine for later ffmpeg encode to MP4.
 *
 * Layout: `{outputDir}/frames/<stableIdSlug>/frame_000.png` …
 *
 * Run: `./gradlew :androidApp:generateGenartPreviewFrames`
 *
 * Props: genart.video.size, genart.video.fps, genart.video.seconds,
 * genart.video.generation, genart.video.engines (comma stable ids or enum names).
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], application = Application::class)
class GenartPreviewFramesTest {

    @Test
    fun generateGenartPreviewFrames() {
        val outDir = File(
            System.getProperty("screenshot.outputDir")
                ?: error("screenshot.outputDir system property required"),
        )
        outDir.mkdirs()
        assertTrue("output dir must exist: $outDir", outDir.isDirectory)

        val size = System.getProperty("genart.video.size")?.toIntOrNull() ?: DEFAULT_SIZE
        val fps = System.getProperty("genart.video.fps")?.toIntOrNull() ?: DEFAULT_FPS
        val seconds = System.getProperty("genart.video.seconds")?.toFloatOrNull() ?: DEFAULT_SECONDS
        val generation = System.getProperty("genart.video.generation")?.toLongOrNull()
            ?: DEFAULT_GENERATION
        val filter = parseEngineFilter(System.getProperty("genart.video.engines"))

        val frameCount = (fps * seconds).toInt().coerceAtLeast(1)
        val entries = GenartCatalog.entries().filter { entry ->
            filter == null ||
                entry.id in filter ||
                entry.engine.name.lowercase() in filter ||
                entry.engine.name in filter
        }
        assertTrue("expected genart catalog entries after filter", entries.isNotEmpty())

        var writtenEngines = 0
        for (entry in entries) {
            val slug = entry.id.removePrefix("genart.").ifEmpty { entry.engine.name.lowercase() }
            val frameDir = File(outDir, "frames/$slug")
            frameDir.mkdirs()

            val rotationDeg = ((generation * 37L) % 360L).toFloat()
            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            try {
                for (i in 0 until frameCount) {
                    val t = i / frameCount.toFloat()
                    val phase = t * 2f * PI.toFloat()
                    val pulse = 0.35f + 0.65f * (0.5f + 0.5f * sin(t * 2f * PI.toFloat()))
                    bitmap.eraseColor(0)
                    GenartStillRenderer.drawFrame(
                        canvas = Canvas(bitmap),
                        engineId = entry.engine,
                        size = size,
                        generation = generation,
                        phase = phase,
                        rotationDeg = rotationDeg,
                        pulse = pulse,
                    )
                    val file = File(frameDir, "frame_%03d.png".format(i))
                    file.outputStream().use { out ->
                        assertTrue(
                            "compress failed for ${entry.id} frame $i",
                            bitmap.compress(Bitmap.CompressFormat.PNG, 92, out),
                        )
                    }
                    assertTrue("missing $file", file.isFile && file.length() > 0L)
                }
            } finally {
                bitmap.recycle()
            }
            writtenEngines++
        }
        assertTrue("wrote frames for $writtenEngines engines", writtenEngines == entries.size)
        println(
            "Wrote $frameCount frames × $writtenEngines engines → ${File(outDir, "frames").absolutePath}",
        )
    }

    private fun parseEngineFilter(raw: String?): Set<String>? {
        val trimmed = raw?.trim().orEmpty()
        if (trimmed.isEmpty()) return null
        return trimmed.split(',')
            .map { it.trim().removePrefix("genart.").lowercase() }
            .filter { it.isNotEmpty() }
            .toSet()
            .ifEmpty { null }
    }

    companion object {
        private const val DEFAULT_SIZE = 720
        private const val DEFAULT_FPS = 12
        private const val DEFAULT_SECONDS = 4f
        private const val DEFAULT_GENERATION = 7L
    }
}

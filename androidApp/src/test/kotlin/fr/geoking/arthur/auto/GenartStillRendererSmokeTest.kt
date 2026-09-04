package fr.geoking.arthur.auto

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import fr.geoking.arthur.genart.GenartEngineId
import fr.geoking.arthur.genart.GenartStillRenderer
import fr.geoking.arthur.shared.domain.Artwork
import fr.geoking.arthur.shared.domain.ArtworkKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], application = Application::class)
class GenartStillRendererSmokeTest {
    @Test
    fun stillBakers_produceOpaqueBitmapsAt64() {
        GenartEngineId.entries.forEach { engine ->
            val bitmap = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
            GenartStillRenderer.draw(
                canvas = Canvas(bitmap),
                engineId = engine,
                size = 64,
                generation = 3L,
            )
            assertTrue("$engine center should be painted", sampleHasPaint(bitmap))
            bitmap.recycle()
        }
    }

    @Test
    fun ambientStill_juliusIds_bakeDistinctFrames() {
        val sphere = bake("genart.sphere", 5L)
        val waves = bake("genart.waves", 5L)
        val micro = bake("genart.micro", 5L)
        assertTrue(sampleHasPaint(sphere))
        assertTrue(sampleHasPaint(waves))
        assertTrue(sampleHasPaint(micro))
        assertNotEquals(checksum(sphere), checksum(waves))
        assertNotEquals(checksum(waves), checksum(micro))
        sphere.recycle()
        waves.recycle()
        micro.recycle()
    }

    @Test
    fun ambientStill_natureIds_bakeDistinctFrames() {
        val snow = bake("genart.snow", 5L)
        val aurora = bake("genart.aurora", 5L)
        val dunes = bake("genart.dunes", 5L)
        assertTrue(sampleHasPaint(snow))
        assertTrue(sampleHasPaint(aurora))
        assertTrue(sampleHasPaint(dunes))
        assertNotEquals(checksum(snow), checksum(aurora))
        assertNotEquals(checksum(aurora), checksum(dunes))
        snow.recycle()
        aurora.recycle()
        dunes.recycle()
    }

    @Test
    fun ambientStill_roadmapIds_bakeDistinctFrames() {
        val ids = listOf(
            "genart.snow",
            "genart.grass",
            "genart.birdflock",
            "genart.mountains",
            "genart.aurora",
            "genart.pondripples",
            "genart.fallingleaves",
            "genart.breathcircles",
            "genart.fireembers",
            "genart.dunes",
            "genart.constellation",
            "genart.clouds",
        )
        val checksums = ids.map { id ->
            val bitmap = bake(id, 5L)
            assertTrue("$id should be painted", sampleHasPaint(bitmap))
            val sum = checksum(bitmap)
            bitmap.recycle()
            sum
        }
        assertEquals(ids.size, checksums.toSet().size)
    }

    @Test
    fun ambientStill_customFractal_bakesFromEncodedId() {
        val id = "customfractal.v1.c42.m0.p200_700_500_200_800_700"
        val a = AmbientStillRenderer.render(
            Artwork(id = id, title = "Custom", sourceId = "customfractal", kind = ArtworkKind.CustomFractal),
            generation = 1L,
        )
        val b = AmbientStillRenderer.render(
            Artwork(id = id, title = "Custom", sourceId = "customfractal", kind = ArtworkKind.CustomFractal),
            generation = 90L,
        )
        assertTrue(sampleHasPaint(a))
        assertTrue(sampleHasPaint(b))
        assertNotEquals(checksum(a), checksum(b))
        a.recycle()
        b.recycle()
    }

    private fun bake(id: String, generation: Long): Bitmap =
        AmbientStillRenderer.render(
            Artwork(id = id, title = id, sourceId = "genart", kind = ArtworkKind.Genart),
            generation,
        )

    private fun sampleHasPaint(bitmap: Bitmap): Boolean {
        val points = listOf(
            8 to 8,
            bitmap.width / 2 to bitmap.height / 2,
            bitmap.width - 9 to bitmap.height - 9,
            bitmap.width / 3 to bitmap.height * 2 / 3,
        )
        return points.any { (x, y) -> bitmap.getPixel(x, y) != 0 }
    }

    private fun checksum(bitmap: Bitmap): Long {
        var sum = 0L
        val step = maxOf(1, bitmap.width / 16)
        for (y in 0 until bitmap.height step step) {
            for (x in 0 until bitmap.width step step) {
                sum = sum * 31 + bitmap.getPixel(x, y)
            }
        }
        return sum
    }
}

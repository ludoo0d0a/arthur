package fr.geoking.arthur.fractal

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CustomFractalEngineTest {

    private val triangle = CustomFractalParams(
        points = listOf(
            NormPoint(0.2f, 0.7f),
            NormPoint(0.5f, 0.2f),
            NormPoint(0.8f, 0.7f),
        ),
        colorSeed = 42,
        morphMode = CustomFractalMorphMode.Breathe,
    )

    @Test
    fun frame_isDeterministic() {
        val a = CustomFractalEngine.frame(triangle, 0.33f, CustomFractalQuality.Medium)
        val b = CustomFractalEngine.frame(triangle, 0.33f, CustomFractalQuality.Medium)
        assertEquals(a.size, b.size)
        assertEquals(a.first().points, b.first().points)
        assertEquals(CustomFractalEngine.seed(triangle), CustomFractalEngine.seed(triangle))
    }

    @Test
    fun pointEdit_changesSeedAndStrokes() {
        val edited = triangle.copy(
            points = listOf(
                NormPoint(0.25f, 0.7f),
                NormPoint(0.5f, 0.2f),
                NormPoint(0.8f, 0.7f),
            ),
        )
        assertNotEquals(CustomFractalEngine.seed(triangle), CustomFractalEngine.seed(edited))
        val a = CustomFractalEngine.frame(triangle, 0.1f, CustomFractalQuality.Low)
        val b = CustomFractalEngine.frame(edited, 0.1f, CustomFractalQuality.Low)
        assertNotEquals(a.first().points.first(), b.first().points.first())
    }

    @Test
    fun encodeDecode_roundTrips() {
        val id = triangle.toArtworkId()
        assertTrue(CustomFractalParams.isCustomId(id))
        val decoded = CustomFractalParams.fromArtworkId(id)
        assertNotNull(decoded)
        assertEquals(triangle.colorSeed, decoded!!.colorSeed)
        assertEquals(triangle.morphMode, decoded.morphMode)
        assertEquals(triangle.points.size, decoded.points.size)
        triangle.points.zip(decoded.points).forEach { (o, d) ->
            assertEquals(o.x, d.x, 0.002f)
            assertEquals(o.y, d.y, 0.002f)
        }
    }

    @Test
    fun decode_rejectsGarbage() {
        assertNull(CustomFractalParams.fromArtworkId("fractal.mandelbrot"))
        assertNull(CustomFractalParams.fromArtworkId("customfractal.v9.c0.m0.p1_2"))
    }

    @Test
    fun normalized_clampsAndCapsPoints() {
        val many = CustomFractalParams(
            points = List(20) { NormPoint(it / 20f, 1.5f) },
            colorSeed = 1,
        ).normalized()
        assertEquals(CustomFractalParams.MAX_POINTS, many.points.size)
        assertTrue(many.points.all { it.y <= 1f })
    }

    @Test
    fun morphModes_produceFrames() {
        CustomFractalMorphMode.entries.forEach { mode ->
            val strokes = CustomFractalEngine.frame(
                triangle.copy(morphMode = mode),
                0.5f,
                CustomFractalQuality.High,
            )
            assertTrue(strokes.isNotEmpty())
            assertTrue(strokes.first().points.size >= 2)
        }
    }

    @Test
    fun requiresMinPoints() {
        try {
            CustomFractalParams(points = listOf(NormPoint(0.1f, 0.1f))).normalized()
            assertFalse("expected require failure", true)
        } catch (_: IllegalArgumentException) {
            // expected
        }
    }
}

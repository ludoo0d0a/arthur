package fr.geoking.arthur.fractal

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FractalCoherentPaletteTest {

    @Test
    fun fourStops_areDeterministicAndSized() {
        val a = FractalCoherentPalette.fourStops(42)
        val b = FractalCoherentPalette.fourStops(42)
        assertEquals(4, a.size)
        assertEquals(a, b)
    }

    @Test
    fun differentSeeds_canPickDifferentThemes() {
        val themes = (0 until FractalCoherentPalette.themes.size).map {
            FractalCoherentPalette.themeFor(it).name
        }.toSet()
        assertEquals(FractalCoherentPalette.themes.size, themes.size)
    }

    @Test
    fun sampleArgb_staysOpaque() {
        val c = FractalCoherentPalette.sampleArgb(7, 0.42f)
        assertEquals(0xFF, (c ushr 24) and 0xFF)
    }

    @Test
    fun escapeArgb_countHonoured() {
        val stops = FractalCoherentPalette.escapeArgb(3, count = 8)
        assertEquals(8, stops.size)
        assertTrue(stops.all { (it ushr 24) and 0xFF == 0xFF })
    }
}

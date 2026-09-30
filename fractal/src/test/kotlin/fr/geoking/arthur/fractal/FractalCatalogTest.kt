package fr.geoking.arthur.fractal

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FractalCatalogTest {
    @Test
    fun freePresets_includeJuliusTypes() {
        assertEquals(12, FractalCatalog.freePresets().size)
    }

    @Test
    fun customFractal_requiresPremium() {
        assertFalse(FractalCatalog.isCustomAllowed(isPremium = false))
        assertTrue(FractalCatalog.isCustomAllowed(isPremium = true))
    }
}

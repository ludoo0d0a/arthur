package fr.geoking.arthur.genart

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class GenartCatalogTest {
    @Test
    fun entries_coverAllEngines() {
        assertEquals(GenartEngineId.entries.size, GenartCatalog.entries().size)
        GenartEngineId.entries.forEach { engine ->
            assertNotNull(GenartCatalog.entries().firstOrNull { it.engine == engine })
        }
    }

    @Test
    fun engineForId_resolvesStableIds() {
        assertEquals(GenartEngineId.Particles, GenartCatalog.engineForId("genart.particles"))
        assertEquals(GenartEngineId.Tunnel, GenartCatalog.engineForId("genart.tunnel"))
    }
}

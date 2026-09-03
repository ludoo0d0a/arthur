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
        assertEquals(GenartEngineId.Sphere, GenartCatalog.engineForId("genart.sphere"))
        assertEquals(GenartEngineId.Waves, GenartCatalog.engineForId("genart.waves"))
        assertEquals(GenartEngineId.Micro, GenartCatalog.engineForId("genart.micro"))
    }

    @Test
    fun juliusFamily_titlesAreStable() {
        assertEquals("Orbiting Sphere", GenartCatalog.entries().first { it.id == "genart.sphere" }.title)
        assertEquals("Layered Waves", GenartCatalog.entries().first { it.id == "genart.waves" }.title)
        assertEquals("Volumetric Rays", GenartCatalog.entries().first { it.id == "genart.micro" }.title)
    }
}

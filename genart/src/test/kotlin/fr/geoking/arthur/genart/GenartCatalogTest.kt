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
        assertEquals(GenartEngineId.Snow, GenartCatalog.engineForId("genart.snow"))
        assertEquals(GenartEngineId.Grass, GenartCatalog.engineForId("genart.grass"))
        assertEquals(GenartEngineId.BirdFlock, GenartCatalog.engineForId("genart.birdflock"))
        assertEquals(GenartEngineId.Mountains, GenartCatalog.engineForId("genart.mountains"))
        assertEquals(GenartEngineId.Aurora, GenartCatalog.engineForId("genart.aurora"))
        assertEquals(GenartEngineId.PondRipples, GenartCatalog.engineForId("genart.pondripples"))
        assertEquals(GenartEngineId.FallingLeaves, GenartCatalog.engineForId("genart.fallingleaves"))
        assertEquals(GenartEngineId.BreathCircles, GenartCatalog.engineForId("genart.breathcircles"))
        assertEquals(GenartEngineId.FireEmbers, GenartCatalog.engineForId("genart.fireembers"))
        assertEquals(GenartEngineId.Dunes, GenartCatalog.engineForId("genart.dunes"))
        assertEquals(GenartEngineId.Constellation, GenartCatalog.engineForId("genart.constellation"))
        assertEquals(GenartEngineId.Clouds, GenartCatalog.engineForId("genart.clouds"))
    }

    @Test
    fun juliusFamily_titlesAreStable() {
        assertEquals("Orbiting Sphere", GenartCatalog.entries().first { it.id == "genart.sphere" }.title)
        assertEquals("Layered Waves", GenartCatalog.entries().first { it.id == "genart.waves" }.title)
        assertEquals("Volumetric Rays", GenartCatalog.entries().first { it.id == "genart.micro" }.title)
    }

    @Test
    fun roadmapFamily_titlesAreStable() {
        assertEquals("Falling Snow", GenartCatalog.entries().first { it.id == "genart.snow" }.title)
        assertEquals("Grass in Wind", GenartCatalog.entries().first { it.id == "genart.grass" }.title)
        assertEquals("Bird Flock", GenartCatalog.entries().first { it.id == "genart.birdflock" }.title)
        assertEquals("Layered Mountains", GenartCatalog.entries().first { it.id == "genart.mountains" }.title)
        assertEquals("Aurora Ribbons", GenartCatalog.entries().first { it.id == "genart.aurora" }.title)
        assertEquals("Pond Ripples", GenartCatalog.entries().first { it.id == "genart.pondripples" }.title)
        assertEquals("Falling Leaves", GenartCatalog.entries().first { it.id == "genart.fallingleaves" }.title)
        assertEquals("Breath Circles", GenartCatalog.entries().first { it.id == "genart.breathcircles" }.title)
        assertEquals("Fireplace Embers", GenartCatalog.entries().first { it.id == "genart.fireembers" }.title)
        assertEquals("Wind-Blown Dunes", GenartCatalog.entries().first { it.id == "genart.dunes" }.title)
        assertEquals("Constellation Twinkle", GenartCatalog.entries().first { it.id == "genart.constellation" }.title)
        assertEquals("Drifting Clouds", GenartCatalog.entries().first { it.id == "genart.clouds" }.title)
    }
}

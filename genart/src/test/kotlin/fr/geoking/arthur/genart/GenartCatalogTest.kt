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
        assertEquals(GenartEngineId.Rain, GenartCatalog.engineForId("genart.rain"))
        assertEquals(GenartEngineId.Fog, GenartCatalog.engineForId("genart.fog"))
        assertEquals(GenartEngineId.FishSchool, GenartCatalog.engineForId("genart.fishschool"))
        assertEquals(GenartEngineId.Fireflies, GenartCatalog.engineForId("genart.fireflies"))
        assertEquals(GenartEngineId.Sunbeams, GenartCatalog.engineForId("genart.sunbeams"))
        assertEquals(GenartEngineId.Meteors, GenartCatalog.engineForId("genart.meteors"))
        assertEquals(GenartEngineId.Bubbles, GenartCatalog.engineForId("genart.bubbles"))
        assertEquals(GenartEngineId.CherryBlossoms, GenartCatalog.engineForId("genart.cherryblossoms"))
        assertEquals(GenartEngineId.SoftRibbons, GenartCatalog.engineForId("genart.ribbons"))
        assertEquals(GenartEngineId.Nebula, GenartCatalog.engineForId("genart.nebula"))
        assertEquals(GenartEngineId.MorphingBlobs, GenartCatalog.engineForId("genart.blobs"))
        assertEquals(GenartEngineId.SoftNoiseField, GenartCatalog.engineForId("genart.noisefield"))
        assertEquals(GenartEngineId.VoronoiWash, GenartCatalog.engineForId("genart.voronoi"))
        assertEquals(GenartEngineId.SilkFolds, GenartCatalog.engineForId("genart.silk"))
        assertEquals(GenartEngineId.GradientMesh, GenartCatalog.engineForId("genart.gradientmesh"))
        assertEquals(GenartEngineId.ArcMosaic, GenartCatalog.engineForId("genart.arcmosaic"))
        assertEquals(GenartEngineId.Storm, GenartCatalog.engineForId("genart.storm"))
        assertEquals(GenartEngineId.StarField, GenartCatalog.engineForId("genart.starfield"))
        assertEquals(GenartEngineId.SolarSystem, GenartCatalog.engineForId("genart.solarsystem"))
        assertEquals(GenartEngineId.LowFreqNoiseField, GenartCatalog.engineForId("genart.lowfreqnoise"))
        assertEquals(GenartEngineId.Fire, GenartCatalog.engineForId("genart.fire"))
        assertEquals(GenartEngineId.PaperCutPack, GenartCatalog.engineForId("genart.papercut"))
        assertEquals(GenartEngineId.DiamondWeave, GenartCatalog.engineForId("genart.diamondweave"))
    }

    @Test
    fun juliusFamily_titlesAreStable() {
        assertEquals("#6 - Orbiting Sphere", GenartCatalog.entries().first { it.id == "genart.sphere" }.title)
        assertEquals("#7 - Layered Waves", GenartCatalog.entries().first { it.id == "genart.waves" }.title)
        assertEquals("#8 - Volumetric Rays", GenartCatalog.entries().first { it.id == "genart.micro" }.title)
    }

    @Test
    fun roadmapFamily_titlesAreStable() {
        assertEquals("#9 - Falling Snow", GenartCatalog.entries().first { it.id == "genart.snow" }.title)
        assertEquals("#10 - Grass in Wind", GenartCatalog.entries().first { it.id == "genart.grass" }.title)
        assertEquals("#11 - Bird Flock", GenartCatalog.entries().first { it.id == "genart.birdflock" }.title)
        assertEquals("#12 - Layered Mountains", GenartCatalog.entries().first { it.id == "genart.mountains" }.title)
        assertEquals("#13 - Aurora Ribbons", GenartCatalog.entries().first { it.id == "genart.aurora" }.title)
        assertEquals("#14 - Pond Ripples", GenartCatalog.entries().first { it.id == "genart.pondripples" }.title)
        assertEquals("#15 - Falling Leaves", GenartCatalog.entries().first { it.id == "genart.fallingleaves" }.title)
        assertEquals("#16 - Breath Circles", GenartCatalog.entries().first { it.id == "genart.breathcircles" }.title)
        assertEquals("#17 - Fireplace Embers", GenartCatalog.entries().first { it.id == "genart.fireembers" }.title)
        assertEquals("#18 - Wind-Blown Dunes", GenartCatalog.entries().first { it.id == "genart.dunes" }.title)
        assertEquals("#19 - Constellation Twinkle", GenartCatalog.entries().first { it.id == "genart.constellation" }.title)
        assertEquals("#20 - Drifting Clouds", GenartCatalog.entries().first { it.id == "genart.clouds" }.title)
        assertEquals("#21 - Soft Rain", GenartCatalog.entries().first { it.id == "genart.rain" }.title)
        assertEquals("#22 - Soft Fog", GenartCatalog.entries().first { it.id == "genart.fog" }.title)
        assertEquals("#23 - School of Fish", GenartCatalog.entries().first { it.id == "genart.fishschool" }.title)
        assertEquals("#24 - Fireflies", GenartCatalog.entries().first { it.id == "genart.fireflies" }.title)
        assertEquals("#25 - Sunbeams Through Haze", GenartCatalog.entries().first { it.id == "genart.sunbeams" }.title)
        assertEquals("#26 - Sparse Meteors", GenartCatalog.entries().first { it.id == "genart.meteors" }.title)
        assertEquals("#27 - Rising Bubbles", GenartCatalog.entries().first { it.id == "genart.bubbles" }.title)
        assertEquals("#28 - Cherry Blossom Petals", GenartCatalog.entries().first { it.id == "genart.cherryblossoms" }.title)
        assertEquals("#29 - Soft Ribbons", GenartCatalog.entries().first { it.id == "genart.ribbons" }.title)
        assertEquals("#30 - Nebula Drift", GenartCatalog.entries().first { it.id == "genart.nebula" }.title)
        assertEquals("#31 - Morphing Blobs", GenartCatalog.entries().first { it.id == "genart.blobs" }.title)
        assertEquals("#32 - Soft Noise Field", GenartCatalog.entries().first { it.id == "genart.noisefield" }.title)
        assertEquals("#33 - Voronoi Wash", GenartCatalog.entries().first { it.id == "genart.voronoi" }.title)
        assertEquals("#34 - Silk Folds", GenartCatalog.entries().first { it.id == "genart.silk" }.title)
        assertEquals("#35 - Gradient Mesh", GenartCatalog.entries().first { it.id == "genart.gradientmesh" }.title)
        assertEquals("#36 - Arc Mosaic", GenartCatalog.entries().first { it.id == "genart.arcmosaic" }.title)
        assertEquals("#37 - Soft Storm", GenartCatalog.entries().first { it.id == "genart.storm" }.title)
        assertEquals("#38 - Star Field Parallax", GenartCatalog.entries().first { it.id == "genart.starfield" }.title)
        assertEquals("#39 - Solar System", GenartCatalog.entries().first { it.id == "genart.solarsystem" }.title)
        assertEquals("#85 - Low-Frequency Noise Field", GenartCatalog.entries().first { it.id == "genart.lowfreqnoise" }.title)
        assertEquals("#86 - Wildfire", GenartCatalog.entries().first { it.id == "genart.fire" }.title)
        assertEquals("#87 - Paper-Cut Pack", GenartCatalog.entries().first { it.id == "genart.papercut" }.title)
        assertEquals("#88 - Diamond Weave", GenartCatalog.entries().first { it.id == "genart.diamondweave" }.title)
    }
}

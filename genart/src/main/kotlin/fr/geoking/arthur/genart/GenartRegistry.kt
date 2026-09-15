package fr.geoking.arthur.genart

import android.graphics.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import fr.geoking.arthur.genart.engines.AntTrailsEngine
import fr.geoking.arthur.genart.engines.ArcMosaicEngine
import fr.geoking.arthur.genart.engines.AuroraEngine
import fr.geoking.arthur.genart.engines.BirdFlockEngine
import fr.geoking.arthur.genart.engines.BreathCirclesEngine
import fr.geoking.arthur.genart.engines.BubblesEngine
import fr.geoking.arthur.genart.engines.CandleEmberEngine
import fr.geoking.arthur.genart.engines.CherryBlossomsEngine
import fr.geoking.arthur.genart.engines.CloudsEngine
import fr.geoking.arthur.genart.engines.ConstellationEngine
import fr.geoking.arthur.genart.engines.DriftingPollenEngine
import fr.geoking.arthur.genart.engines.DunesEngine
import fr.geoking.arthur.genart.engines.EclipseCoronaEngine
import fr.geoking.arthur.genart.engines.FallingLeavesEngine
import fr.geoking.arthur.genart.engines.FireEngine
import fr.geoking.arthur.genart.engines.FireEmbersEngine
import fr.geoking.arthur.genart.engines.FirefliesEngine
import fr.geoking.arthur.genart.engines.FishSchoolEngine
import fr.geoking.arthur.genart.engines.FogEngine
import fr.geoking.arthur.genart.engines.FrostCrystalsEngine
import fr.geoking.arthur.genart.engines.GradientMeshEngine
import fr.geoking.arthur.genart.engines.GrassEngine
import fr.geoking.arthur.genart.engines.HeatHazeEngine
import fr.geoking.arthur.genart.engines.InkInWaterEngine
import fr.geoking.arthur.genart.engines.IonTrailEngine
import fr.geoking.arthur.genart.engines.LandslideDustEngine
import fr.geoking.arthur.genart.engines.LightDrizzleEngine
import fr.geoking.arthur.genart.engines.MeteorsEngine
import fr.geoking.arthur.genart.engines.MicroEngine
import fr.geoking.arthur.genart.engines.MoonlightRipplesEngine
import fr.geoking.arthur.genart.engines.MorphingBlobsEngine
import fr.geoking.arthur.genart.engines.MountainsEngine
import fr.geoking.arthur.genart.engines.NebulaEngine
import fr.geoking.arthur.genart.engines.ParticlesEngine
import fr.geoking.arthur.genart.engines.PebbleShoreWashEngine
import fr.geoking.arthur.genart.engines.PondRipplesEngine
import fr.geoking.arthur.genart.engines.Pseudo3DEngine
import fr.geoking.arthur.genart.engines.RainEngine
import fr.geoking.arthur.genart.engines.RainbowEngine
import fr.geoking.arthur.genart.engines.SilkFoldsEngine
import fr.geoking.arthur.genart.engines.SleepingPetEngine
import fr.geoking.arthur.genart.engines.SmogEngine
import fr.geoking.arthur.genart.engines.SmokeEngine
import fr.geoking.arthur.genart.engines.SnowEngine
import fr.geoking.arthur.genart.engines.SoftNoiseFieldEngine
import fr.geoking.arthur.genart.engines.SoftRibbonsEngine
import fr.geoking.arthur.genart.engines.SoftShadowsEngine
import fr.geoking.arthur.genart.engines.SolarSystemEngine
import fr.geoking.arthur.genart.engines.SpaceStationDriftEngine
import fr.geoking.arthur.genart.engines.SphereEngine
import fr.geoking.arthur.genart.engines.StarFieldEngine
import fr.geoking.arthur.genart.engines.SteamCurlEngine
import fr.geoking.arthur.genart.engines.StormEngine
import fr.geoking.arthur.genart.engines.SunbeamsEngine
import fr.geoking.arthur.genart.engines.SunshineEngine
import fr.geoking.arthur.genart.engines.TonalGeometryEngine
import fr.geoking.arthur.genart.engines.TunnelEngine
import fr.geoking.arthur.genart.engines.AquariumEngine
import fr.geoking.arthur.genart.engines.AsteroidsEngine
import fr.geoking.arthur.genart.engines.CanyonDunesEngine
import fr.geoking.arthur.genart.engines.CityLightsEngine
import fr.geoking.arthur.genart.engines.ContinentsEngine
import fr.geoking.arthur.genart.engines.DataHorizonEngine
import fr.geoking.arthur.genart.engines.DayNightWashEngine
import fr.geoking.arthur.genart.engines.DiamondWeaveEngine
import fr.geoking.arthur.genart.engines.DistantDinosaursEngine
import fr.geoking.arthur.genart.engines.FieldsEngine
import fr.geoking.arthur.genart.engines.FlowerEngine
import fr.geoking.arthur.genart.engines.GerstnerOceanEngine
import fr.geoking.arthur.genart.engines.LakeEngine
import fr.geoking.arthur.genart.engines.MossGrowthEngine
import fr.geoking.arthur.genart.engines.PaperCutPackEngine
import fr.geoking.arthur.genart.engines.RainOnGlassEngine
import fr.geoking.arthur.genart.engines.ReedsEngine
import fr.geoking.arthur.genart.engines.LowFreqNoiseFieldEngine
import fr.geoking.arthur.genart.engines.RiversEngine
import fr.geoking.arthur.genart.engines.RoadsEngine
import fr.geoking.arthur.genart.engines.SoftCausticsEngine
import fr.geoking.arthur.genart.engines.SoftWindStreaksEngine
import fr.geoking.arthur.genart.engines.SpiralGalaxyEngine
import fr.geoking.arthur.genart.engines.TerrariumDripEngine
import fr.geoking.arthur.genart.engines.TreeEngine
import fr.geoking.arthur.genart.engines.TumbleweedDriftEngine
import fr.geoking.arthur.genart.engines.VoronoiWashEngine
import fr.geoking.arthur.genart.engines.WarpStreakEngine
import fr.geoking.arthur.genart.engines.WaterfallMistEngine
import fr.geoking.arthur.genart.engines.WavesEngine
import fr.geoking.arthur.genart.engines.WindChimeEngine
import fr.geoking.arthur.genart.stills.AntTrailsStill
import fr.geoking.arthur.genart.stills.ArcMosaicStill
import fr.geoking.arthur.genart.stills.AuroraStill
import fr.geoking.arthur.genart.stills.BirdFlockStill
import fr.geoking.arthur.genart.stills.BreathCirclesStill
import fr.geoking.arthur.genart.stills.BubblesStill
import fr.geoking.arthur.genart.stills.CandleEmberStill
import fr.geoking.arthur.genart.stills.CherryBlossomsStill
import fr.geoking.arthur.genart.stills.CloudsStill
import fr.geoking.arthur.genart.stills.ConstellationStill
import fr.geoking.arthur.genart.stills.DriftingPollenStill
import fr.geoking.arthur.genart.stills.DunesStill
import fr.geoking.arthur.genart.stills.EclipseCoronaStill
import fr.geoking.arthur.genart.stills.FallingLeavesStill
import fr.geoking.arthur.genart.stills.FireEmbersStill
import fr.geoking.arthur.genart.stills.FirefliesStill
import fr.geoking.arthur.genart.stills.FireStill
import fr.geoking.arthur.genart.stills.FishSchoolStill
import fr.geoking.arthur.genart.stills.FogStill
import fr.geoking.arthur.genart.stills.FrostCrystalsStill
import fr.geoking.arthur.genart.stills.GradientMeshStill
import fr.geoking.arthur.genart.stills.GrassStill
import fr.geoking.arthur.genart.stills.HeatHazeStill
import fr.geoking.arthur.genart.stills.InkInWaterStill
import fr.geoking.arthur.genart.stills.IonTrailStill
import fr.geoking.arthur.genart.stills.LandslideDustStill
import fr.geoking.arthur.genart.stills.LightDrizzleStill
import fr.geoking.arthur.genart.stills.MeteorsStill
import fr.geoking.arthur.genart.stills.MicroStill
import fr.geoking.arthur.genart.stills.MoonlightRipplesStill
import fr.geoking.arthur.genart.stills.MorphingBlobsStill
import fr.geoking.arthur.genart.stills.MountainsStill
import fr.geoking.arthur.genart.stills.NebulaStill
import fr.geoking.arthur.genart.stills.ParticlesStill
import fr.geoking.arthur.genart.stills.PebbleShoreWashStill
import fr.geoking.arthur.genart.stills.PondRipplesStill
import fr.geoking.arthur.genart.stills.RainStill
import fr.geoking.arthur.genart.stills.RainbowStill
import fr.geoking.arthur.genart.stills.SilkFoldsStill
import fr.geoking.arthur.genart.stills.SleepingPetStill
import fr.geoking.arthur.genart.stills.SmogStill
import fr.geoking.arthur.genart.stills.SmokeStill
import fr.geoking.arthur.genart.stills.SnowStill
import fr.geoking.arthur.genart.stills.SoftNoiseFieldStill
import fr.geoking.arthur.genart.stills.SoftRibbonsStill
import fr.geoking.arthur.genart.stills.SolarSystemStill
import fr.geoking.arthur.genart.stills.SpaceStationDriftStill
import fr.geoking.arthur.genart.stills.SphereStill
import fr.geoking.arthur.genart.stills.StarFieldStill
import fr.geoking.arthur.genart.stills.SteamCurlStill
import fr.geoking.arthur.genart.stills.StormStill
import fr.geoking.arthur.genart.stills.SunbeamsStill
import fr.geoking.arthur.genart.stills.SunshineStill
import fr.geoking.arthur.genart.stills.AquariumStill
import fr.geoking.arthur.genart.stills.AsteroidsStill
import fr.geoking.arthur.genart.stills.CanyonDunesStill
import fr.geoking.arthur.genart.stills.CityLightsStill
import fr.geoking.arthur.genart.stills.ContinentsStill
import fr.geoking.arthur.genart.stills.DataHorizonStill
import fr.geoking.arthur.genart.stills.DayNightWashStill
import fr.geoking.arthur.genart.stills.DiamondWeaveStill
import fr.geoking.arthur.genart.stills.DistantDinosaursStill
import fr.geoking.arthur.genart.stills.FieldsStill
import fr.geoking.arthur.genart.stills.FlowerStill
import fr.geoking.arthur.genart.stills.GerstnerOceanStill
import fr.geoking.arthur.genart.stills.LakeStill
import fr.geoking.arthur.genart.stills.MossGrowthStill
import fr.geoking.arthur.genart.stills.PaperCutPackStill
import fr.geoking.arthur.genart.stills.RainOnGlassStill
import fr.geoking.arthur.genart.stills.LowFreqNoiseFieldStill
import fr.geoking.arthur.genart.stills.ReedsStill
import fr.geoking.arthur.genart.stills.RiversStill
import fr.geoking.arthur.genart.stills.RoadsStill
import fr.geoking.arthur.genart.stills.SoftCausticsStill
import fr.geoking.arthur.genart.stills.SoftWindStreaksStill
import fr.geoking.arthur.genart.stills.SpiralGalaxyStill
import fr.geoking.arthur.genart.stills.TerrariumDripStill
import fr.geoking.arthur.genart.stills.TreeStill
import fr.geoking.arthur.genart.stills.TumbleweedDriftStill
import fr.geoking.arthur.genart.stills.VoronoiWashStill
import fr.geoking.arthur.genart.stills.WarpStreakStill
import fr.geoking.arthur.genart.stills.WaterfallMistStill
import fr.geoking.arthur.genart.stills.WavesStill
import fr.geoking.arthur.genart.stills.WindChimeStill

/**
 * One live Composable + one baked-still renderer, registered under a stable id/title.
 *
 * This is the plugin unit for a genart engine: shipping a new one means adding exactly one
 * [GenartEngineDescriptor] to [GenartRegistry.all] (plus the [GenartEngineId] case it references) —
 * not a new branch in every renderer that dispatches on engine id.
 */
class GenartEngineDescriptor(
    val id: GenartEngineId,
    val stableId: String,
    val title: String,
    val render: @Composable (
        isActive: Boolean,
        paletteColors: List<Color>,
        quality: GenartQuality,
        brightness: Float,
        speed: Float,
        modifier: Modifier,
    ) -> Unit,
    val renderStill: (
        canvas: Canvas,
        size: Int,
        generation: Long,
        phase: Float,
        rotationDeg: Float,
        pulse: Float,
        palette: AnimationPalette,
    ) -> Unit,
)

/** Single registration point for every genart engine — see [GenartEngineDescriptor]. */
object GenartRegistry {
    val all: List<GenartEngineDescriptor> = listOf(
        GenartEngineDescriptor(
            id = GenartEngineId.Particles,
            stableId = "genart.particles",
            title = "#1 - Drifting Particles",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                ParticlesEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                ParticlesStill.draw(
                    canvas = canvas,
                    size = size,
                    seed = generation,
                    time = phase * 0.02f,
                    rotationDeg = rotationDeg,
                    pulse = pulse,
                    palette = palette,
                )
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.Pseudo3D,
            stableId = "genart.pseudo3d",
            title = "#2 - Wire Lattice",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                Pseudo3DEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, _, _, _, palette ->
                GenartStillRenderer.drawFallback(canvas, size, generation, palette, GenartEngineId.Pseudo3D)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.SoftShadows,
            stableId = "genart.softshadows",
            title = "#3 - Soft Shadows",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                SoftShadowsEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, _, _, _, palette ->
                GenartStillRenderer.drawFallback(canvas, size, generation, palette, GenartEngineId.SoftShadows)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.Tunnel,
            stableId = "genart.tunnel",
            title = "#4 - Vanishing Tunnel",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                TunnelEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, _, _, _, palette ->
                GenartStillRenderer.drawFallback(canvas, size, generation, palette, GenartEngineId.Tunnel)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.TonalGeometry,
            stableId = "genart.tonalgeometry",
            title = "#5 - Tonal Geometry",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                TonalGeometryEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, _, _, _, palette ->
                GenartStillRenderer.drawFallback(canvas, size, generation, palette, GenartEngineId.TonalGeometry)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.Sphere,
            stableId = "genart.sphere",
            title = "#6 - Orbiting Sphere",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                SphereEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, _, _, rotationDeg, pulse, palette ->
                SphereStill.draw(canvas = canvas, size = size, rotationDeg = rotationDeg, pulse = pulse, palette = palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.Waves,
            stableId = "genart.waves",
            title = "#7 - Layered Waves",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                WavesEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, _, phase, _, _, palette ->
                WavesStill.draw(canvas = canvas, size = size, phaseBase = phase, palette = palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.Micro,
            stableId = "genart.micro",
            title = "#8 - Volumetric Rays",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                MicroEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, _, _, rotationDeg, pulse, palette ->
                MicroStill.draw(canvas = canvas, size = size, rotationDeg = rotationDeg, pulseScale = 1f + pulse * 0.15f, palette = palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.Snow,
            stableId = "genart.snow",
            title = "#9 - Falling Snow",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                SnowEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                SnowStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.Grass,
            stableId = "genart.grass",
            title = "#10 - Grass in Wind",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                GrassEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                GrassStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.BirdFlock,
            stableId = "genart.birdflock",
            title = "#11 - Bird Flock",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                BirdFlockEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                BirdFlockStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.Mountains,
            stableId = "genart.mountains",
            title = "#12 - Layered Mountains",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                MountainsEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                MountainsStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.Aurora,
            stableId = "genart.aurora",
            title = "#13 - Aurora Ribbons",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                AuroraEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                AuroraStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.PondRipples,
            stableId = "genart.pondripples",
            title = "#14 - Pond Ripples",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                PondRipplesEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                PondRipplesStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.FallingLeaves,
            stableId = "genart.fallingleaves",
            title = "#15 - Falling Leaves",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                FallingLeavesEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                FallingLeavesStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.BreathCircles,
            stableId = "genart.breathcircles",
            title = "#16 - Breath Circles",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                BreathCirclesEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                BreathCirclesStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.FireEmbers,
            stableId = "genart.fireembers",
            title = "#17 - Fireplace Embers",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                FireEmbersEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                FireEmbersStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.Dunes,
            stableId = "genart.dunes",
            title = "#18 - Wind-Blown Dunes",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                DunesEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                DunesStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.Constellation,
            stableId = "genart.constellation",
            title = "#19 - Constellation Twinkle",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                ConstellationEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                ConstellationStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.Clouds,
            stableId = "genart.clouds",
            title = "#20 - Drifting Clouds",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                CloudsEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                CloudsStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.Rain,
            stableId = "genart.rain",
            title = "#21 - Soft Rain",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                RainEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                RainStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.Fog,
            stableId = "genart.fog",
            title = "#22 - Soft Fog",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                FogEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                FogStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.FishSchool,
            stableId = "genart.fishschool",
            title = "#23 - School of Fish",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                FishSchoolEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                FishSchoolStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.Fireflies,
            stableId = "genart.fireflies",
            title = "#24 - Fireflies",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                FirefliesEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                FirefliesStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.Sunbeams,
            stableId = "genart.sunbeams",
            title = "#25 - Sunbeams Through Haze",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                SunbeamsEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                SunbeamsStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.Meteors,
            stableId = "genart.meteors",
            title = "#26 - Sparse Meteors",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                MeteorsEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                MeteorsStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.Bubbles,
            stableId = "genart.bubbles",
            title = "#27 - Rising Bubbles",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                BubblesEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                BubblesStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.CherryBlossoms,
            stableId = "genart.cherryblossoms",
            title = "#28 - Cherry Blossom Petals",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                CherryBlossomsEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                CherryBlossomsStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.SoftRibbons,
            stableId = "genart.ribbons",
            title = "#29 - Soft Ribbons",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                SoftRibbonsEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                SoftRibbonsStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.Nebula,
            stableId = "genart.nebula",
            title = "#30 - Nebula Drift",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                NebulaEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                NebulaStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.MorphingBlobs,
            stableId = "genart.blobs",
            title = "#31 - Morphing Blobs",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                MorphingBlobsEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                MorphingBlobsStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.SoftNoiseField,
            stableId = "genart.noisefield",
            title = "#32 - Soft Noise Field",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                SoftNoiseFieldEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                SoftNoiseFieldStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.VoronoiWash,
            stableId = "genart.voronoi",
            title = "#33 - Voronoi Wash",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                VoronoiWashEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                VoronoiWashStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.SilkFolds,
            stableId = "genart.silk",
            title = "#34 - Silk Folds",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                SilkFoldsEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                SilkFoldsStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.GradientMesh,
            stableId = "genart.gradientmesh",
            title = "#35 - Gradient Mesh",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                GradientMeshEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                GradientMeshStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.ArcMosaic,
            stableId = "genart.arcmosaic",
            title = "#36 - Arc Mosaic",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                ArcMosaicEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                ArcMosaicStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.Storm,
            stableId = "genart.storm",
            title = "#37 - Soft Storm",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                StormEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                StormStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.StarField,
            stableId = "genart.starfield",
            title = "#38 - Star Field Parallax",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                StarFieldEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                StarFieldStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.SolarSystem,
            stableId = "genart.solarsystem",
            title = "#39 - Solar System",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                SolarSystemEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                SolarSystemStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.CandleEmber,
            stableId = "genart.candleember",
            title = "#40 - Candle Ember",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                CandleEmberEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                CandleEmberStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.Rainbow,
            stableId = "genart.rainbow",
            title = "#41 - Soft Rainbow",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                RainbowEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                RainbowStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.Smog,
            stableId = "genart.smog",
            title = "#42 - Soft Smog",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                SmogEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                SmogStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.Smoke,
            stableId = "genart.smoke",
            title = "#43 - Rising Smoke",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                SmokeEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                SmokeStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.HeatHaze,
            stableId = "genart.heathaze",
            title = "#44 - Heat Haze",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                HeatHazeEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                HeatHazeStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.Sunshine,
            stableId = "genart.sunshine",
            title = "#45 - Soft Sunshine",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                SunshineEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                SunshineStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.LightDrizzle,
            stableId = "genart.lightdrizzle",
            title = "#46 - Light Drizzle",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                LightDrizzleEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                LightDrizzleStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.SteamCurl,
            stableId = "genart.steamcurl",
            title = "#47 - Steam Curl",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                SteamCurlEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                SteamCurlStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.DriftingPollen,
            stableId = "genart.pollen",
            title = "#48 - Drifting Pollen",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                DriftingPollenEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                DriftingPollenStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.LandslideDust,
            stableId = "genart.landslidedust",
            title = "#49 - Soft Landslide Dust",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                LandslideDustEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                LandslideDustStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.PebbleShoreWash,
            stableId = "genart.pebbleshore",
            title = "#50 - Pebble Shore Wash",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                PebbleShoreWashEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                PebbleShoreWashStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.FrostCrystals,
            stableId = "genart.frostcrystals",
            title = "#51 - First Frost Crystals",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                FrostCrystalsEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                FrostCrystalsStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.IonTrail,
            stableId = "genart.iontrail",
            title = "#52 - Ion Trail",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                IonTrailEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                IonTrailStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.AntTrails,
            stableId = "genart.anttrails",
            title = "#53 - Ant Trails",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                AntTrailsEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                AntTrailsStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.SleepingPet,
            stableId = "genart.sleepingpet",
            title = "#54 - Sleeping Pet Outline",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                SleepingPetEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                SleepingPetStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.WarpStreak,
            stableId = "genart.warpstreak",
            title = "#55 - Warp Streak",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                WarpStreakEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                WarpStreakStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.SpaceStationDrift,
            stableId = "genart.spacestation",
            title = "#56 - Space Station Drift",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                SpaceStationDriftEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                SpaceStationDriftStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.MoonlightRipples,
            stableId = "genart.moonlightripples",
            title = "#57 - Moonlight Ripples",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                MoonlightRipplesEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                MoonlightRipplesStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.EclipseCorona,
            stableId = "genart.eclipsecorona",
            title = "#58 - Eclipse Corona",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                EclipseCoronaEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                EclipseCoronaStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.InkInWater,
            stableId = "genart.inkinwater",
            title = "#59 - Ink in Water",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                InkInWaterEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                InkInWaterStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.WindChime,
            stableId = "genart.windchime",
            title = "#60 - Wind Chime Silhouette",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                WindChimeEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                WindChimeStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.DayNightWash,
            stableId = "genart.daynightwash",
            title = "#61 - Soft Day-Night Wash",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                DayNightWashEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                DayNightWashStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.TumbleweedDrift,
            stableId = "genart.tumbleweed",
            title = "#62 - Tumbleweed Drift",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                TumbleweedDriftEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                TumbleweedDriftStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.DistantDinosaurs,
            stableId = "genart.dinosaurs",
            title = "#63 - Distant Dinosaur Silhouettes",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                DistantDinosaursEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                DistantDinosaursStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.CityLights,
            stableId = "genart.citylights",
            title = "#64 - City Night Lights",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                CityLightsEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                CityLightsStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.MossGrowth,
            stableId = "genart.moss",
            title = "#65 - Moss Growth",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                MossGrowthEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                MossGrowthStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.TerrariumDrip,
            stableId = "genart.terrariumdrip",
            title = "#66 - Terrarium Drip",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                TerrariumDripEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                TerrariumDripStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.Aquarium,
            stableId = "genart.aquarium",
            title = "#67 - Aquarium",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                AquariumEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                AquariumStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.Fields,
            stableId = "genart.fields",
            title = "#68 - Soft Fields",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                FieldsEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                FieldsStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.RainOnGlass,
            stableId = "genart.rainonglass",
            title = "#69 - Rain on Glass",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                RainOnGlassEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                RainOnGlassStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.Reeds,
            stableId = "genart.reeds",
            title = "#70 - Reeds",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                ReedsEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                ReedsStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.CanyonDunes,
            stableId = "genart.canyondunes",
            title = "#71 - Canyon Dunes",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                CanyonDunesEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                CanyonDunesStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.Continents,
            stableId = "genart.continents",
            title = "#72 - Continents",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                ContinentsEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                ContinentsStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.Roads,
            stableId = "genart.roads",
            title = "#73 - Roads",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                RoadsEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                RoadsStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.Rivers,
            stableId = "genart.rivers",
            title = "#74 - Rivers",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                RiversEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                RiversStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.Tree,
            stableId = "genart.tree",
            title = "#75 - Tree in Wind",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                TreeEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                TreeStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.Flower,
            stableId = "genart.flower",
            title = "#76 - Flower Bloom",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                FlowerEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                FlowerStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.Lake,
            stableId = "genart.lake",
            title = "#77 - Lake Surface",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                LakeEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                LakeStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.Asteroids,
            stableId = "genart.asteroids",
            title = "#78 - Asteroids",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                AsteroidsEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                AsteroidsStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.WaterfallMist,
            stableId = "genart.waterfallmist",
            title = "#79 - Waterfall Mist",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                WaterfallMistEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                WaterfallMistStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.SoftWindStreaks,
            stableId = "genart.windstreaks",
            title = "#80 - Soft Wind Streaks",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                SoftWindStreaksEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                SoftWindStreaksStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.GerstnerOcean,
            stableId = "genart.oceanswell",
            title = "#81 - Ocean Swell",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                GerstnerOceanEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                GerstnerOceanStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.SpiralGalaxy,
            stableId = "genart.galaxy",
            title = "#82 - Spiral Galaxy Drift",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                SpiralGalaxyEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                SpiralGalaxyStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.DataHorizon,
            stableId = "genart.datahorizon",
            title = "#83 - Data Horizon",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                DataHorizonEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                DataHorizonStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.SoftCaustics,
            stableId = "genart.caustics",
            title = "#84 - Soft Caustics",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                SoftCausticsEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                SoftCausticsStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.LowFreqNoiseField,
            stableId = "genart.lowfreqnoise",
            title = "#85 - Low-Frequency Noise Field",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                LowFreqNoiseFieldEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                LowFreqNoiseFieldStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.Fire,
            stableId = "genart.fire",
            title = "#86 - Wildfire",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                FireEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                FireStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.PaperCutPack,
            stableId = "genart.papercut",
            title = "#87 - Paper-Cut Pack",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                PaperCutPackEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                PaperCutPackStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.DiamondWeave,
            stableId = "genart.diamondweave",
            title = "#88 - Diamond Weave",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                DiamondWeaveEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                DiamondWeaveStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
    )

    private val byStableId: Map<String, GenartEngineDescriptor> = all.associateBy { it.stableId }
    private val byEngineId: Map<GenartEngineId, GenartEngineDescriptor> = all.associateBy { it.id }

    fun catalogEntries(): List<GenartCatalogEntry> =
        all.map { GenartCatalogEntry(it.id, it.stableId, it.title) }

    fun engineForId(stableId: String): GenartEngineId? = byStableId[stableId]?.id

    fun descriptorFor(engineId: GenartEngineId): GenartEngineDescriptor = byEngineId.getValue(engineId)
}

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
import fr.geoking.arthur.genart.engines.VoronoiWashEngine
import fr.geoking.arthur.genart.engines.WarpStreakEngine
import fr.geoking.arthur.genart.engines.WavesEngine
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
import fr.geoking.arthur.genart.stills.VoronoiWashStill
import fr.geoking.arthur.genart.stills.WarpStreakStill
import fr.geoking.arthur.genart.stills.WavesStill

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
            title = "Drifting Particles",
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
            title = "Wire Lattice",
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
            title = "Soft Shadows",
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
            title = "Vanishing Tunnel",
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
            title = "Tonal Geometry",
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
            title = "Orbiting Sphere",
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
            title = "Layered Waves",
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
            title = "Volumetric Rays",
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
            title = "Falling Snow",
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
            title = "Grass in Wind",
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
            title = "Bird Flock",
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
            title = "Layered Mountains",
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
            title = "Aurora Ribbons",
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
            title = "Pond Ripples",
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
            title = "Falling Leaves",
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
            title = "Breath Circles",
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
            title = "Fireplace Embers",
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
            title = "Wind-Blown Dunes",
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
            title = "Constellation Twinkle",
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
            title = "Drifting Clouds",
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
            title = "Soft Rain",
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
            title = "Soft Fog",
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
            title = "School of Fish",
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
            title = "Fireflies",
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
            title = "Sunbeams Through Haze",
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
            title = "Sparse Meteors",
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
            title = "Rising Bubbles",
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
            title = "Cherry Blossom Petals",
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
            title = "Soft Ribbons",
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
            title = "Nebula Drift",
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
            title = "Morphing Blobs",
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
            title = "Soft Noise Field",
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
            title = "Voronoi Wash",
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
            title = "Silk Folds",
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
            title = "Gradient Mesh",
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
            title = "Arc Mosaic",
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
            title = "Soft Storm",
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
            title = "Star Field Parallax",
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
            title = "Solar System",
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
            title = "Candle Ember",
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
            title = "Soft Rainbow",
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
            title = "Soft Smog",
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
            title = "Rising Smoke",
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
            title = "Heat Haze",
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
            title = "Soft Sunshine",
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
            title = "Light Drizzle",
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
            title = "Steam Curl",
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
            title = "Drifting Pollen",
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
            title = "Soft Landslide Dust",
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
            title = "Pebble Shore Wash",
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
            title = "First Frost Crystals",
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
            title = "Ion Trail",
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
            title = "Ant Trails",
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
            title = "Sleeping Pet Outline",
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
            title = "Warp Streak",
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
            title = "Space Station Drift",
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
            title = "Moonlight Ripples",
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
            title = "Eclipse Corona",
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
            title = "Ink in Water",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                InkInWaterEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                InkInWaterStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
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

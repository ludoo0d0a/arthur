package fr.geoking.arthur.genart

import android.graphics.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import fr.geoking.arthur.genart.engines.SuperdriveEngine
import fr.geoking.arthur.genart.engines.FireworksEngine
import fr.geoking.arthur.genart.engines.AtmosphericAsteroidEngine
import fr.geoking.arthur.genart.engines.SaturnRingsEngine
import fr.geoking.arthur.genart.engines.LavaSunEngine
import fr.geoking.arthur.genart.engines.MoonEngine
import fr.geoking.arthur.genart.engines.SpiralMandalaEngine
import fr.geoking.arthur.genart.engines.FlowRibbonsEngine
import fr.geoking.arthur.genart.engines.CliffordWashEngine
import fr.geoking.arthur.genart.engines.PlasmaNovaEngine
import fr.geoking.arthur.genart.engines.EnergyTendrilsEngine
import fr.geoking.arthur.genart.engines.InterferenceWashEngine
import fr.geoking.arthur.genart.engines.NestedPackEngine
import fr.geoking.arthur.genart.engines.SilkBloomEngine
import fr.geoking.arthur.genart.engines.CrossingSpotlightsEngine
import fr.geoking.arthur.genart.engines.GlassMarblesEngine
import fr.geoking.arthur.genart.engines.LampInDarknessEngine
import fr.geoking.arthur.genart.engines.MarbleCausticsEngine
import fr.geoking.arthur.genart.engines.PaneReflectionsEngine
import fr.geoking.arthur.genart.engines.BillowingClothEngine
import fr.geoking.arthur.genart.engines.WindCurtainsEngine
import fr.geoking.arthur.genart.engines.MarbleDriftEngine
import fr.geoking.arthur.genart.engines.CanyonLightEngine
import fr.geoking.arthur.genart.engines.DepthShaftsEngine
import fr.geoking.arthur.genart.engines.GlassOrbEngine
import fr.geoking.arthur.genart.engines.SoftRayOrbsEngine
import fr.geoking.arthur.genart.engines.PrismCaveEngine
import fr.geoking.arthur.genart.engines.SoapFilmEngine
import fr.geoking.arthur.genart.engines.LitLatticeEngine
import fr.geoking.arthur.genart.engines.RetroWaveEngine
import fr.geoking.arthur.genart.engines.TerrainMakerEngine
import fr.geoking.arthur.genart.engines.MultiFlamesEngine
import fr.geoking.arthur.genart.engines.AirBubblesEngine
import fr.geoking.arthur.genart.engines.CourtBounceEngine
import fr.geoking.arthur.genart.engines.CopperBarsEngine
import fr.geoking.arthur.genart.engines.NeonHighwayEngine
import fr.geoking.arthur.genart.engines.SoftDropEngine
import fr.geoking.arthur.genart.engines.VectorRocksEngine
import fr.geoking.arthur.genart.engines.BrickCascadeEngine
import fr.geoking.arthur.genart.engines.PixelFormationEngine
import fr.geoking.arthur.genart.engines.CrtBootEngine
import fr.geoking.arthur.genart.engines.LwpPlasmaEngine
import fr.geoking.arthur.genart.engines.LwpStarfieldEngine
import fr.geoking.arthur.genart.engines.LwpVoronoiEngine
import fr.geoking.arthur.genart.engines.ShaderlayNeonGridEngine
import fr.geoking.arthur.genart.engines.ShaderlayVortexTunnelEngine
import fr.geoking.arthur.genart.engines.PpssppCrtScanlinesEngine
import fr.geoking.arthur.genart.engines.PpssppCelShaderEngine
import fr.geoking.arthur.genart.engines.PpssppBloomVignetteEngine
import fr.geoking.arthur.genart.engines.PaperSimplexNoiseEngine
import fr.geoking.arthur.genart.engines.PaperNeuroNoiseEngine
import fr.geoking.arthur.genart.engines.PaperGrainGradientEngine
import fr.geoking.arthur.genart.engines.ShadyNoodleZoomEngine
import fr.geoking.arthur.genart.engines.ShadyWarpSpeedEngine
import fr.geoking.arthur.genart.engines.ShadyMarbledPaperEngine
import fr.geoking.arthur.genart.engines.MilkyWayEngine
import fr.geoking.arthur.genart.engines.PlasmaFlowEngine
import fr.geoking.arthur.genart.engines.RoboticEyeEngine
import fr.geoking.arthur.genart.engines.PenroseTriangleEngine
import fr.geoking.arthur.genart.engines.PenroseStairsEngine
import fr.geoking.arthur.genart.engines.ImpossibleCubeEngine
import fr.geoking.arthur.genart.engines.NeckerCubeEngine
import fr.geoking.arthur.genart.engines.CafeWallEngine
import fr.geoking.arthur.genart.engines.CatSleepEngine
import fr.geoking.arthur.genart.engines.CatTailEngine
import fr.geoking.arthur.genart.engines.CatWindowEngine
import fr.geoking.arthur.genart.engines.DogDistantWalkEngine
import fr.geoking.arthur.genart.engines.DogNapEngine
import fr.geoking.arthur.genart.engines.DogStretchEngine
import fr.geoking.arthur.genart.engines.PandaBambooEngine
import fr.geoking.arthur.genart.engines.PandaRollEngine
import fr.geoking.arthur.genart.engines.SeasonSpringWashEngine
import fr.geoking.arthur.genart.engines.SeasonAutumnWashEngine
import fr.geoking.arthur.genart.engines.SeasonWinterWashEngine
import fr.geoking.arthur.genart.engines.FrostedWindowDawnEngine
import fr.geoking.arthur.genart.engines.PaperLanternEngine
import fr.geoking.arthur.genart.engines.HearthGlowEngine
import fr.geoking.arthur.genart.engines.VolumetricStairwellEngine
import fr.geoking.arthur.genart.engines.WindowDustMotesEngine
import fr.geoking.arthur.genart.engines.BookshelfGlowEngine
import fr.geoking.arthur.genart.engines.FloorLampPoolEngine
import fr.geoking.arthur.genart.engines.CrystalCavernGlowEngine
import fr.geoking.arthur.genart.engines.InkBloomEngine
import fr.geoking.arthur.genart.engines.SumiWashEngine
import fr.geoking.arthur.genart.engines.InkTendrilEngine
import fr.geoking.arthur.genart.engines.KaleidoPetalEngine
import fr.geoking.arthur.genart.engines.KaleidoMirrorEngine
import fr.geoking.arthur.genart.engines.KaleidoOrbitEngine
import fr.geoking.arthur.genart.engines.ZenEnSoEngine
import fr.geoking.arthur.genart.engines.ZenStoneRipplesEngine
import fr.geoking.arthur.genart.engines.ZenIncenseEngine
import fr.geoking.arthur.genart.engines.ObsidianShoreEngine
import fr.geoking.arthur.genart.engines.TideFoamEngine
import fr.geoking.arthur.genart.engines.CoastCliffHazeEngine
import fr.geoking.arthur.genart.engines.DreamVeilEngine
import fr.geoking.arthur.genart.engines.LucidOrbsEngine
import fr.geoking.arthur.genart.engines.SleepParallaxEngine
import fr.geoking.arthur.genart.engines.MicroDewEngine
import fr.geoking.arthur.genart.engines.MicroFiberEngine
import fr.geoking.arthur.genart.engines.MicroDustEngine
import fr.geoking.arthur.genart.engines.MicroPollenCloseEngine
import fr.geoking.arthur.genart.engines.MicroBubbleSkinEngine
import fr.geoking.arthur.genart.engines.AutoDashSoftEngine
import fr.geoking.arthur.genart.engines.AutoRainGlassEngine
import fr.geoking.arthur.genart.engines.AutoDistantHeadlightsEngine
import fr.geoking.arthur.genart.engines.AutoDashboardGlowEngine
import fr.geoking.arthur.genart.engines.AutoTunnelLightEngine
import fr.geoking.arthur.genart.engines.AutoHorizonDriveEngine
import fr.geoking.arthur.genart.engines.MotoHeadlightEngine
import fr.geoking.arthur.genart.engines.MotoWindStreaksEngine
import fr.geoking.arthur.genart.engines.MotoRoadBlurEngine
import fr.geoking.arthur.genart.engines.MotoNightLeanEngine
import fr.geoking.arthur.genart.engines.MotoTailGlowEngine
import fr.geoking.arthur.genart.engines.MotoDawnRideEngine
import fr.geoking.arthur.genart.engines.AvionHorizonCloudsEngine
import fr.geoking.arthur.genart.engines.AvionContrailEngine
import fr.geoking.arthur.genart.engines.AvionWingGlowEngine
import fr.geoking.arthur.genart.engines.AvionCabinWindowEngine
import fr.geoking.arthur.genart.engines.AvionSunriseBandEngine
import fr.geoking.arthur.genart.engines.AvionNightCityEngine
import fr.geoking.arthur.genart.engines.VeloPathFieldsEngine
import fr.geoking.arthur.genart.engines.VeloParallaxTreesEngine
import fr.geoking.arthur.genart.engines.VeloDawnLaneEngine
import fr.geoking.arthur.genart.engines.VeloRainAsphaltEngine
import fr.geoking.arthur.genart.engines.VeloGoldenHourEngine
import fr.geoking.arthur.genart.engines.VeloForestCanopyEngine
import fr.geoking.arthur.genart.engines.TrainWindowBlurEngine
import fr.geoking.arthur.genart.engines.TrainCatenaryEngine
import fr.geoking.arthur.genart.engines.TrainPlatformGlowEngine
import fr.geoking.arthur.genart.engines.TrainCountrysideEngine
import fr.geoking.arthur.genart.engines.TrainNightCarriageEngine
import fr.geoking.arthur.genart.engines.TrainTunnelPulseEngine
import fr.geoking.arthur.genart.engines.UsDinerNeonEngine
import fr.geoking.arthur.genart.engines.UsMotelSignEngine
import fr.geoking.arthur.genart.engines.UsRoute66WashEngine
import fr.geoking.arthur.genart.engines.UsCrtArcadeEngine
import fr.geoking.arthur.genart.engines.UsRetrowaveEngine
import fr.geoking.arthur.genart.engines.UsDriveInEngine
import fr.geoking.arthur.genart.engines.StormStrobeFlashEngine
import fr.geoking.arthur.genart.engines.StormStrobeSheetEngine
import fr.geoking.arthur.genart.engines.StormStrobePulseEngine
import fr.geoking.arthur.genart.engines.StormStrobeForkEngine
import fr.geoking.arthur.genart.engines.StormStrobeHorizonEngine
import fr.geoking.arthur.genart.engines.StormStrobeCloudEngine
import fr.geoking.arthur.genart.engines.PhysarumVeinsEngine
import fr.geoking.arthur.genart.engines.PhysarumLatticeEngine
import fr.geoking.arthur.genart.engines.PhysarumBloomEngine
import fr.geoking.arthur.genart.engines.PhysarumDriftEngine
import fr.geoking.arthur.genart.engines.PhysarumNodeEngine
import fr.geoking.arthur.genart.engines.PhysarumFilamentEngine
import fr.geoking.arthur.genart.engines.ReactionSpotsEngine
import fr.geoking.arthur.genart.engines.ReactionCoralsEngine
import fr.geoking.arthur.genart.engines.ReactionWavesEngine
import fr.geoking.arthur.genart.engines.ReactionMitosisEngine
import fr.geoking.arthur.genart.engines.ReactionMazeEngine
import fr.geoking.arthur.genart.engines.ReactionBlobsEngine
import fr.geoking.arthur.genart.engines.SoftTideRipplesEngine
import fr.geoking.arthur.genart.engines.HarborMistEngine
import fr.geoking.arthur.genart.engines.PandaSitEngine
import fr.geoking.arthur.genart.engines.KanizsaTriangleEngine
import fr.geoking.arthur.genart.engines.CheckerShadowEngine
import fr.geoking.arthur.genart.engines.DriftingHalosEngine
import fr.geoking.arthur.genart.engines.AntTrailsEngine
import fr.geoking.arthur.genart.engines.AuroraWashEngine
import fr.geoking.arthur.genart.engines.ChromaticBlobsEngine
import fr.geoking.arthur.genart.engines.HaloEclipseEngine
import fr.geoking.arthur.genart.engines.PrismaticShadowsEngine
import fr.geoking.arthur.genart.engines.SpectralFoldsEngine
import fr.geoking.arthur.genart.engines.VortexGlowEngine
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
import fr.geoking.arthur.genart.engines.FieldsEngine
import fr.geoking.arthur.genart.engines.FlowerEngine
import fr.geoking.arthur.genart.engines.GerstnerOceanEngine
import fr.geoking.arthur.genart.engines.LakeEngine
import fr.geoking.arthur.genart.engines.MossGrowthEngine
import fr.geoking.arthur.genart.engines.MatrixEngine
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
import fr.geoking.arthur.genart.stills.SuperdriveStill
import fr.geoking.arthur.genart.stills.FireworksStill
import fr.geoking.arthur.genart.stills.AtmosphericAsteroidStill
import fr.geoking.arthur.genart.stills.SaturnRingsStill
import fr.geoking.arthur.genart.stills.LavaSunStill
import fr.geoking.arthur.genart.stills.MoonStill
import fr.geoking.arthur.genart.stills.SpiralMandalaStill
import fr.geoking.arthur.genart.stills.FlowRibbonsStill
import fr.geoking.arthur.genart.stills.CliffordWashStill
import fr.geoking.arthur.genart.stills.PlasmaNovaStill
import fr.geoking.arthur.genart.stills.EnergyTendrilsStill
import fr.geoking.arthur.genart.stills.InterferenceWashStill
import fr.geoking.arthur.genart.stills.NestedPackStill
import fr.geoking.arthur.genart.stills.SilkBloomStill
import fr.geoking.arthur.genart.stills.CrossingSpotlightsStill
import fr.geoking.arthur.genart.stills.GlassMarblesStill
import fr.geoking.arthur.genart.stills.LampInDarknessStill
import fr.geoking.arthur.genart.stills.MarbleCausticsStill
import fr.geoking.arthur.genart.stills.PaneReflectionsStill
import fr.geoking.arthur.genart.stills.BillowingClothStill
import fr.geoking.arthur.genart.stills.WindCurtainsStill
import fr.geoking.arthur.genart.stills.MarbleDriftStill
import fr.geoking.arthur.genart.stills.CanyonLightStill
import fr.geoking.arthur.genart.stills.DepthShaftsStill
import fr.geoking.arthur.genart.stills.GlassOrbStill
import fr.geoking.arthur.genart.stills.SoftRayOrbsStill
import fr.geoking.arthur.genart.stills.PrismCaveStill
import fr.geoking.arthur.genart.stills.SoapFilmStill
import fr.geoking.arthur.genart.stills.LitLatticeStill
import fr.geoking.arthur.genart.stills.RetroWaveStill
import fr.geoking.arthur.genart.stills.TerrainMakerStill
import fr.geoking.arthur.genart.stills.MultiFlamesStill
import fr.geoking.arthur.genart.stills.AirBubblesStill
import fr.geoking.arthur.genart.stills.CourtBounceStill
import fr.geoking.arthur.genart.stills.CopperBarsStill
import fr.geoking.arthur.genart.stills.NeonHighwayStill
import fr.geoking.arthur.genart.stills.SoftDropStill
import fr.geoking.arthur.genart.stills.VectorRocksStill
import fr.geoking.arthur.genart.stills.BrickCascadeStill
import fr.geoking.arthur.genart.stills.PixelFormationStill
import fr.geoking.arthur.genart.stills.CrtBootStill
import fr.geoking.arthur.genart.stills.LwpPlasmaStill
import fr.geoking.arthur.genart.stills.LwpStarfieldStill
import fr.geoking.arthur.genart.stills.LwpVoronoiStill
import fr.geoking.arthur.genart.stills.ShaderlayNeonGridStill
import fr.geoking.arthur.genart.stills.ShaderlayVortexTunnelStill
import fr.geoking.arthur.genart.stills.PpssppCrtScanlinesStill
import fr.geoking.arthur.genart.stills.PpssppCelShaderStill
import fr.geoking.arthur.genart.stills.PpssppBloomVignetteStill
import fr.geoking.arthur.genart.stills.PaperSimplexNoiseStill
import fr.geoking.arthur.genart.stills.PaperNeuroNoiseStill
import fr.geoking.arthur.genart.stills.PaperGrainGradientStill
import fr.geoking.arthur.genart.stills.ShadyNoodleZoomStill
import fr.geoking.arthur.genart.stills.ShadyWarpSpeedStill
import fr.geoking.arthur.genart.stills.ShadyMarbledPaperStill
import fr.geoking.arthur.genart.stills.MilkyWayStill
import fr.geoking.arthur.genart.stills.PlasmaFlowStill
import fr.geoking.arthur.genart.stills.RoboticEyeStill
import fr.geoking.arthur.genart.stills.PenroseTriangleStill
import fr.geoking.arthur.genart.stills.PenroseStairsStill
import fr.geoking.arthur.genart.stills.ImpossibleCubeStill
import fr.geoking.arthur.genart.stills.NeckerCubeStill
import fr.geoking.arthur.genart.stills.CafeWallStill
import fr.geoking.arthur.genart.stills.CatSleepStill
import fr.geoking.arthur.genart.stills.CatTailStill
import fr.geoking.arthur.genart.stills.CatWindowStill
import fr.geoking.arthur.genart.stills.DogDistantWalkStill
import fr.geoking.arthur.genart.stills.DogNapStill
import fr.geoking.arthur.genart.stills.DogStretchStill
import fr.geoking.arthur.genart.stills.PandaBambooStill
import fr.geoking.arthur.genart.stills.PandaRollStill
import fr.geoking.arthur.genart.stills.SeasonSpringWashStill
import fr.geoking.arthur.genart.stills.SeasonAutumnWashStill
import fr.geoking.arthur.genart.stills.SeasonWinterWashStill
import fr.geoking.arthur.genart.stills.FrostedWindowDawnStill
import fr.geoking.arthur.genart.stills.PaperLanternStill
import fr.geoking.arthur.genart.stills.HearthGlowStill
import fr.geoking.arthur.genart.stills.VolumetricStairwellStill
import fr.geoking.arthur.genart.stills.WindowDustMotesStill
import fr.geoking.arthur.genart.stills.BookshelfGlowStill
import fr.geoking.arthur.genart.stills.FloorLampPoolStill
import fr.geoking.arthur.genart.stills.CrystalCavernGlowStill
import fr.geoking.arthur.genart.stills.InkBloomStill
import fr.geoking.arthur.genart.stills.SumiWashStill
import fr.geoking.arthur.genart.stills.InkTendrilStill
import fr.geoking.arthur.genart.stills.KaleidoPetalStill
import fr.geoking.arthur.genart.stills.KaleidoMirrorStill
import fr.geoking.arthur.genart.stills.KaleidoOrbitStill
import fr.geoking.arthur.genart.stills.ZenEnSoStill
import fr.geoking.arthur.genart.stills.ZenStoneRipplesStill
import fr.geoking.arthur.genart.stills.ZenIncenseStill
import fr.geoking.arthur.genart.stills.ObsidianShoreStill
import fr.geoking.arthur.genart.stills.TideFoamStill
import fr.geoking.arthur.genart.stills.CoastCliffHazeStill
import fr.geoking.arthur.genart.stills.DreamVeilStill
import fr.geoking.arthur.genart.stills.LucidOrbsStill
import fr.geoking.arthur.genart.stills.SleepParallaxStill
import fr.geoking.arthur.genart.stills.MicroDewStill
import fr.geoking.arthur.genart.stills.MicroFiberStill
import fr.geoking.arthur.genart.stills.MicroDustStill
import fr.geoking.arthur.genart.stills.MicroPollenCloseStill
import fr.geoking.arthur.genart.stills.MicroBubbleSkinStill
import fr.geoking.arthur.genart.stills.AutoDashSoftStill
import fr.geoking.arthur.genart.stills.AutoRainGlassStill
import fr.geoking.arthur.genart.stills.AutoDistantHeadlightsStill
import fr.geoking.arthur.genart.stills.AutoDashboardGlowStill
import fr.geoking.arthur.genart.stills.AutoTunnelLightStill
import fr.geoking.arthur.genart.stills.AutoHorizonDriveStill
import fr.geoking.arthur.genart.stills.MotoHeadlightStill
import fr.geoking.arthur.genart.stills.MotoWindStreaksStill
import fr.geoking.arthur.genart.stills.MotoRoadBlurStill
import fr.geoking.arthur.genart.stills.MotoNightLeanStill
import fr.geoking.arthur.genart.stills.MotoTailGlowStill
import fr.geoking.arthur.genart.stills.MotoDawnRideStill
import fr.geoking.arthur.genart.stills.AvionHorizonCloudsStill
import fr.geoking.arthur.genart.stills.AvionContrailStill
import fr.geoking.arthur.genart.stills.AvionWingGlowStill
import fr.geoking.arthur.genart.stills.AvionCabinWindowStill
import fr.geoking.arthur.genart.stills.AvionSunriseBandStill
import fr.geoking.arthur.genart.stills.AvionNightCityStill
import fr.geoking.arthur.genart.stills.VeloPathFieldsStill
import fr.geoking.arthur.genart.stills.VeloParallaxTreesStill
import fr.geoking.arthur.genart.stills.VeloDawnLaneStill
import fr.geoking.arthur.genart.stills.VeloRainAsphaltStill
import fr.geoking.arthur.genart.stills.VeloGoldenHourStill
import fr.geoking.arthur.genart.stills.VeloForestCanopyStill
import fr.geoking.arthur.genart.stills.TrainWindowBlurStill
import fr.geoking.arthur.genart.stills.TrainCatenaryStill
import fr.geoking.arthur.genart.stills.TrainPlatformGlowStill
import fr.geoking.arthur.genart.stills.TrainCountrysideStill
import fr.geoking.arthur.genart.stills.TrainNightCarriageStill
import fr.geoking.arthur.genart.stills.TrainTunnelPulseStill
import fr.geoking.arthur.genart.stills.UsDinerNeonStill
import fr.geoking.arthur.genart.stills.UsMotelSignStill
import fr.geoking.arthur.genart.stills.UsRoute66WashStill
import fr.geoking.arthur.genart.stills.UsCrtArcadeStill
import fr.geoking.arthur.genart.stills.UsRetrowaveStill
import fr.geoking.arthur.genart.stills.UsDriveInStill
import fr.geoking.arthur.genart.stills.StormStrobeFlashStill
import fr.geoking.arthur.genart.stills.StormStrobeSheetStill
import fr.geoking.arthur.genart.stills.StormStrobePulseStill
import fr.geoking.arthur.genart.stills.StormStrobeForkStill
import fr.geoking.arthur.genart.stills.StormStrobeHorizonStill
import fr.geoking.arthur.genart.stills.StormStrobeCloudStill
import fr.geoking.arthur.genart.stills.PhysarumVeinsStill
import fr.geoking.arthur.genart.stills.PhysarumLatticeStill
import fr.geoking.arthur.genart.stills.PhysarumBloomStill
import fr.geoking.arthur.genart.stills.PhysarumDriftStill
import fr.geoking.arthur.genart.stills.PhysarumNodeStill
import fr.geoking.arthur.genart.stills.PhysarumFilamentStill
import fr.geoking.arthur.genart.stills.ReactionSpotsStill
import fr.geoking.arthur.genart.stills.ReactionCoralsStill
import fr.geoking.arthur.genart.stills.ReactionWavesStill
import fr.geoking.arthur.genart.stills.ReactionMitosisStill
import fr.geoking.arthur.genart.stills.ReactionMazeStill
import fr.geoking.arthur.genart.stills.ReactionBlobsStill
import fr.geoking.arthur.genart.stills.SoftTideRipplesStill
import fr.geoking.arthur.genart.stills.HarborMistStill
import fr.geoking.arthur.genart.stills.PandaSitStill
import fr.geoking.arthur.genart.stills.KanizsaTriangleStill
import fr.geoking.arthur.genart.stills.CheckerShadowStill
import fr.geoking.arthur.genart.stills.DriftingHalosStill
import fr.geoking.arthur.genart.stills.AntTrailsStill
import fr.geoking.arthur.genart.stills.AuroraWashStill
import fr.geoking.arthur.genart.stills.ChromaticBlobsStill
import fr.geoking.arthur.genart.stills.HaloEclipseStill
import fr.geoking.arthur.genart.stills.PrismaticShadowsStill
import fr.geoking.arthur.genart.stills.SpectralFoldsStill
import fr.geoking.arthur.genart.stills.VortexGlowStill
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
import fr.geoking.arthur.genart.stills.FieldsStill
import fr.geoking.arthur.genart.stills.FlowerStill
import fr.geoking.arthur.genart.stills.GerstnerOceanStill
import fr.geoking.arthur.genart.stills.LakeStill
import fr.geoking.arthur.genart.stills.MatrixStill
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
import fr.geoking.arthur.genart.stills.TunnelStill
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
    /** When false, excluded from Android Auto Ambient rotation. */
    val autoSafe: Boolean = true,
    /** When false, excluded from TV Dream / Ambient rotation. */
    val tvDreamSafe: Boolean = true,
)

/** Single registration point for every genart engine — see [GenartEngineDescriptor]. */
object GenartRegistry {
    val all: List<GenartEngineDescriptor> = listOf(
        GenartEngineDescriptor(
            id = GenartEngineId.Particles,
            stableId = "genart.particles",
            title = "#1 - Particules dérivantes",
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
            title = "#2 - Treillis de fils",
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
            title = "#3 - Ombres douces",
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
            title = "#4 - Tunnel fuyant",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                TunnelEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                TunnelStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.TonalGeometry,
            stableId = "genart.tonalgeometry",
            title = "#5 - Géométrie tonale",
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
            title = "#6 - Sphère en orbite",
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
            title = "#7 - Vagues en couches",
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
            title = "#8 - Rayons volumétriques",
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
            title = "#9 - Neige qui tombe",
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
            title = "#10 - Herbe au vent",
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
            title = "#11 - Vol d'oiseaux",
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
            title = "#12 - Montagnes en couches",
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
            title = "#13 - Rubans d'aurore",
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
            title = "#14 - Rides d'étang",
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
            title = "#15 - Feuilles qui tombent",
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
            title = "#16 - Cercles de souffle",
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
            title = "#17 - Braises de cheminée",
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
            title = "#18 - Dunes au vent",
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
            title = "#19 - Constellation scintillante",
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
            title = "#20 - Nuages dérivants",
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
            title = "#21 - Pluie douce",
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
            title = "#22 - Brouillard doux",
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
            title = "#23 - Banc de poissons",
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
            title = "#24 - Lucioles",
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
            title = "#25 - Rayons dans la brume",
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
            title = "#26 - Météores clairsemés",
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
            title = "#27 - Bulles montantes",
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
            title = "#28 - Pétales de cerisier",
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
            title = "#29 - Rubans doux",
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
            title = "#30 - Nébuleuse dérivante",
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
            title = "#31 - Blobs morphing",
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
            title = "#32 - Champ de bruit doux",
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
            title = "#33 - Lavis Voronoi",
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
            title = "#34 - Plis de soie",
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
            title = "#35 - Maillage dégradé",
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
            title = "#36 - Mosaïque d'arcs",
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
            title = "#37 - Orage doux",
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
            title = "#38 - Champ d'étoiles parallaxe",
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
            title = "#39 - Système solaire",
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
            title = "#40 - Braise de bougie",
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
            title = "#41 - Arc-en-ciel doux",
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
            title = "#42 - Brume urbaine",
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
            title = "#43 - Fumée montante",
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
            title = "#44 - Mirage de chaleur",
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
            title = "#45 - Soleil doux",
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
            title = "#46 - Bruine légère",
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
            title = "#47 - Vapeur enroulée",
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
            title = "#48 - Pollen dérivant",
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
            title = "#49 - Poussière d'éboulement",
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
            title = "#50 - Lavis de galets",
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
            title = "#51 - Premiers cristaux de givre",
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
            title = "#52 - Traînée d'ions",
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
            title = "#53 - Pistes de fourmis",
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
            title = "#54 - Silhouette d'animal endormi",
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
            title = "#55 - Traînées warp",
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
            title = "#56 - Station spatiale dérivante",
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
            title = "#57 - Rides au clair de lune",
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
            title = "#58 - Couronne d'éclipse",
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
            title = "#59 - Encre dans l'eau",
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
            title = "#60 - Silhouette de carillon",
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
            title = "#61 - Lavis jour-nuit",
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
            title = "#62 - Virevoltant dérivant",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                TumbleweedDriftEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                TumbleweedDriftStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.CityLights,
            stableId = "genart.citylights",
            title = "#64 - Lumières de ville la nuit",
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
            title = "#65 - Mousses qui poussent",
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
            title = "#66 - Goutte de terrarium",
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
            title = "#68 - Champs doux",
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
            title = "#69 - Pluie sur vitre",
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
            title = "#70 - Roseaux",
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
            title = "#71 - Dunes de canyon",
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
            title = "#73 - Routes",
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
            title = "#74 - Rivières",
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
            title = "#75 - Arbre au vent",
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
            title = "#76 - Éclosion de fleur",
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
            title = "#77 - Surface de lac",
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
            title = "#78 - Astéroïdes",
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
            title = "#79 - Brume de cascade",
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
            title = "#80 - Traînées de vent doux",
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
            title = "#81 - Houle océanique",
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
            title = "#82 - Galaxie spirale dérivante",
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
            title = "#83 - Horizon de données",
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
            title = "#84 - Caustiques douces",
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
            title = "#85 - Champ de bruit basse fréquence",
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
            title = "#86 - Feu de forêt",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                FireEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                FireStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.ChromaticBlobs,
            stableId = "genart.chromaticblobs",
            title = "#87 - Blobs chromatiques",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                ChromaticBlobsEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                ChromaticBlobsStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.PrismaticShadows,
            stableId = "genart.prismaticshadows",
            title = "#88 - Ombres prismatiques",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                PrismaticShadowsEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                PrismaticShadowsStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.AuroraWash,
            stableId = "genart.aurorawash",
            title = "#89 - Lavis d'aurore",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                AuroraWashEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                AuroraWashStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.VortexGlow,
            stableId = "genart.vortexglow",
            title = "#90 - Lueur de vortex",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                VortexGlowEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                VortexGlowStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.SpectralFolds,
            stableId = "genart.spectralfolds",
            title = "#91 - Plis spectraux",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                SpectralFoldsEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                SpectralFoldsStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.HaloEclipse,
            stableId = "genart.haloeclipse",
            title = "#92 - Halo d'éclipse",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                HaloEclipseEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                HaloEclipseStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.PaperCutPack,
            stableId = "genart.papercut",
            title = "#87 - Découpes de papier",
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
            title = "#88 - Tissage diamant",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                DiamondWeaveEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                DiamondWeaveStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.Matrix,
            stableId = "genart.matrix",
            title = "#93 - Pluie Matrix",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                MatrixEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                MatrixStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.DriftingHalos,
            stableId = "genart.driftinghalos",
            title = "#94 - Halos mobiles",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                DriftingHalosEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                DriftingHalosStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.Superdrive,
            stableId = "genart.superdrive",
            title = "#95 - Vibes Superdrive",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                SuperdriveEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                SuperdriveStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.Fireworks,
            stableId = "genart.fireworks",
            title = "#96 - Feux d'artifice doux",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                FireworksEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                FireworksStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.AtmosphericAsteroid,
            stableId = "genart.atmosphericasteroid",
            title = "#97 - Astéroïde atmosphérique",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                AtmosphericAsteroidEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                AtmosphericAsteroidStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.SaturnRings,
            stableId = "genart.saturnrings",
            title = "#98 - Saturne et ses anneaux",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                SaturnRingsEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                SaturnRingsStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.LavaSun,
            stableId = "genart.lavasun",
            title = "#99 - Soleil de lave",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                LavaSunEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                LavaSunStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.Moon,
            stableId = "genart.moon",
            title = "#100 - Pleine lune",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                MoonEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                MoonStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.SpiralMandala,
            stableId = "genart.spiralmandala",
            title = "#101 - Mandala spirale",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                SpiralMandalaEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                SpiralMandalaStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.FlowRibbons,
            stableId = "genart.flowribbons",
            title = "#102 - Rubans fluides",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                FlowRibbonsEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                FlowRibbonsStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.CliffordWash,
            stableId = "genart.cliffordwash",
            title = "#103 - Lavis Clifford",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                CliffordWashEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                CliffordWashStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.PlasmaNova,
            stableId = "genart.plasmanova",
            title = "#104 - Nova plasma",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                PlasmaNovaEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                PlasmaNovaStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.EnergyTendrils,
            stableId = "genart.energytendrils",
            title = "#105 - Vrilles d'énergie",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                EnergyTendrilsEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                EnergyTendrilsStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.InterferenceWash,
            stableId = "genart.interferencewash",
            title = "#106 - Lavis d'interférence",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                InterferenceWashEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                InterferenceWashStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.NestedPack,
            stableId = "genart.nestedpack",
            title = "#107 - Pack imbriqué",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                NestedPackEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                NestedPackStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.SilkBloom,
            stableId = "genart.silkbloom",
            title = "#108 - Éclosion de soie",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                SilkBloomEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                SilkBloomStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.CrossingSpotlights,
            stableId = "genart.crossingspots",
            title = "#109 - Projecteurs croisés",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                CrossingSpotlightsEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                CrossingSpotlightsStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.LampInDarkness,
            stableId = "genart.lampdark",
            title = "#110 - Lampe dans l'obscurité",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                LampInDarknessEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                LampInDarknessStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.BillowingCloth,
            stableId = "genart.billowcloth",
            title = "#111 - Tissu ondulant",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                BillowingClothEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                BillowingClothStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.WindCurtains,
            stableId = "genart.windcurtains",
            title = "#112 - Rideaux au vent",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                WindCurtainsEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                WindCurtainsStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.GlassMarbles,
            stableId = "genart.glassmarbles",
            title = "#113 - Billes de verre",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                GlassMarblesEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                GlassMarblesStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.PaneReflections,
            stableId = "genart.panereflections",
            title = "#114 - Reflets de vitre",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                PaneReflectionsEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                PaneReflectionsStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.MarbleCaustics,
            stableId = "genart.marblecaustics",
            title = "#115 - Caustiques de marbre",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                MarbleCausticsEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                MarbleCausticsStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.MarbleDrift,
            stableId = "genart.marble",
            title = "#116 - Marbre dérivant",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                MarbleDriftEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                MarbleDriftStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.CanyonLight,
            stableId = "genart.canyonlight",
            title = "#117 - Coup de lumière en canyon",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                CanyonLightEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                CanyonLightStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.DepthShafts,
            stableId = "genart.depthshafts",
            title = "#118 - Puits de lumière sous-marins",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                DepthShaftsEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                DepthShaftsStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.GlassOrb,
            stableId = "genart.glassorb",
            title = "#119 - Orbe de verre sur bureau",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                GlassOrbEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                GlassOrbStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.SoftRayOrbs,
            stableId = "genart.rayorbs",
            title = "#120 - Orbes raymarch doux",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                SoftRayOrbsEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                SoftRayOrbsStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.PrismCave,
            stableId = "genart.prismcave",
            title = "#121 - Grotte de prismes",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                PrismCaveEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                PrismCaveStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.SoapFilm,
            stableId = "genart.soapfilm",
            title = "#122 - Film de savon irisé",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                SoapFilmEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                SoapFilmStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.LitLattice,
            stableId = "genart.litlattice",
            title = "#123 - Treillis filaire éclairé",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                LitLatticeEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                LitLatticeStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.RetroWave,
            stableId = "genart.retrowave",
            title = "#124 - Démo rétro-futuriste",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                RetroWaveEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                RetroWaveStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.TerrainMaker,
            stableId = "genart.terrainmaker",
            title = "#125 - Générateur de terrain",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                TerrainMakerEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                TerrainMakerStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.MultiFlames,
            stableId = "genart.multiflames",
            title = "#126 - Feux multiples",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                MultiFlamesEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                MultiFlamesStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.AirBubbles,
            stableId = "genart.airbubbles",
            title = "#127 - Bulles d'air sous-marines",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                AirBubblesEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                AirBubblesStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.CourtBounce,
            stableId = "genart.courtbounce",
            title = "#128 - Rebond de court",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                CourtBounceEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                CourtBounceStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.CopperBars,
            stableId = "genart.copperbars",
            title = "#129 - Barres de cuivre",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                CopperBarsEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                CopperBarsStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.NeonHighway,
            stableId = "genart.neonhighway",
            title = "#130 - Autoroute néon",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                NeonHighwayEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                NeonHighwayStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.SoftDrop,
            stableId = "genart.softdrop",
            title = "#131 - Goutte douce",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                SoftDropEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                SoftDropStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.VectorRocks,
            stableId = "genart.vectorrocks",
            title = "#132 - Rochers vectoriels",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                VectorRocksEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                VectorRocksStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.BrickCascade,
            stableId = "genart.brickcascade",
            title = "#133 - Cascade de briques",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                BrickCascadeEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                BrickCascadeStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.PixelFormation,
            stableId = "genart.pixelformation",
            title = "#134 - Formation de pixels",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                PixelFormationEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                PixelFormationStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.CrtBoot,
            stableId = "genart.crtboot",
            title = "#135 - Démarrage CRT",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                CrtBootEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                CrtBootStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.LwpPlasma,
            stableId = "genart.lwpplasma",
            title = "#136 - Plasma harmonique LWP",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                LwpPlasmaEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                LwpPlasmaStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.LwpStarfield,
            stableId = "genart.lwpstarfield",
            title = "#137 - Champ d'étoiles warp LWP",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                LwpStarfieldEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                LwpStarfieldStill().draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.LwpVoronoi,
            stableId = "genart.lwpvoronoi",
            title = "#138 - Caustiques cellulaires Voronoi LWP",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                LwpVoronoiEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                LwpVoronoiStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.ShaderlayNeonGrid,
            stableId = "genart.neongrid",
            title = "#139 - Grille néon synthwave Shaderlay",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                ShaderlayNeonGridEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                ShaderlayNeonGridStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.ShaderlayVortexTunnel,
            stableId = "genart.vortextunnel",
            title = "#140 - Tunnel vortex warp Shaderlay",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                ShaderlayVortexTunnelEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                ShaderlayVortexTunnelStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.PpssppCrtScanlines,
            stableId = "genart.crtscanlines",
            title = "#141 - Scanlines CRT PPSSPP",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                PpssppCrtScanlinesEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                PpssppCrtScanlinesStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.PpssppCelShader,
            stableId = "genart.celshader",
            title = "#142 - Cel-shader PPSSPP",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                PpssppCelShaderEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                PpssppCelShaderStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.PpssppBloomVignette,
            stableId = "genart.bloomvignette",
            title = "#143 - Bloom vignette PPSSPP",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                PpssppBloomVignetteEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                PpssppBloomVignetteStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.PaperSimplexNoise,
            stableId = "genart.simplexnoise",
            title = "#144 - Flux bruit simplex papier",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                PaperSimplexNoiseEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                PaperSimplexNoiseStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.PaperNeuroNoise,
            stableId = "genart.neuronnoise",
            title = "#145 - Vagues neuro-bruit papier",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                PaperNeuroNoiseEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                PaperNeuroNoiseStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.PaperGrainGradient,
            stableId = "genart.graingradient",
            title = "#146 - Dégradé grain papier",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                PaperGrainGradientEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                PaperGrainGradientStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.ShadyNoodleZoom,
            stableId = "genart.noodlezoom",
            title = "#147 - Tunnel noodle zoom Shady",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                ShadyNoodleZoomEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                ShadyNoodleZoomStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.ShadyWarpSpeed,
            stableId = "genart.warpspeed",
            title = "#148 - Vitesse warp Shady",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                ShadyWarpSpeedEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                ShadyWarpSpeedStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.ShadyMarbledPaper,
            stableId = "genart.marbledpaper",
            title = "#149 - Papier marbré Shady",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                ShadyMarbledPaperEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                ShadyMarbledPaperStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.MilkyWay,
            stableId = "genart.milkyway",
            title = "#150 - Voie lactée",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                MilkyWayEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                MilkyWayStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.PlasmaFlow,
            stableId = "genart.plasmaflow",
            title = "#151 - Lignes de flux plasma",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                PlasmaFlowEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                PlasmaFlowStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.RoboticEye,
            stableId = "genart.roboticeye",
            title = "#152 - Œil robotique cybernétique",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                RoboticEyeEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                RoboticEyeStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.PenroseTriangle,
            stableId = "genart.penrosetriangle",
            title = "#153 - Triangle de Penrose",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                PenroseTriangleEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                PenroseTriangleStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.PenroseStairs,
            stableId = "genart.penrosestairs",
            title = "#154 - Escalier de Penrose",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                PenroseStairsEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                PenroseStairsStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.ImpossibleCube,
            stableId = "genart.impossiblecube",
            title = "#155 - Cube impossible",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                ImpossibleCubeEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                ImpossibleCubeStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.NeckerCube,
            stableId = "genart.neckercube",
            title = "#156 - Cube de Necker",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                NeckerCubeEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                NeckerCubeStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.CafeWall,
            stableId = "genart.cafewall",
            title = "#157 - Mur du café",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                CafeWallEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                CafeWallStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.KanizsaTriangle,
            stableId = "genart.kanizsa",
            title = "#158 - Triangle de Kanizsa",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                KanizsaTriangleEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                KanizsaTriangleStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.CheckerShadow,
            stableId = "genart.checkershadow",
            title = "#159 - Ombre en damier",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                CheckerShadowEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                CheckerShadowStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.CatWindow,
            stableId = "genart.catwindow",
            title = "#160 - Chat à la fenêtre",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                CatWindowEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                CatWindowStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.CatSleep,
            stableId = "genart.catsleep",
            title = "#161 - Chat endormi",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                CatSleepEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                CatSleepStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.DogNap,
            stableId = "genart.dognap",
            title = "#162 - Sieste de chien",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                DogNapEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                DogNapStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.DogDistantWalk,
            stableId = "genart.dogdistantwalk",
            title = "#163 - Chien au loin",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                DogDistantWalkEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                DogDistantWalkStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.PandaSit,
            stableId = "genart.pandasit",
            title = "#164 - Panda assis",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                PandaSitEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                PandaSitStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.PandaBamboo,
            stableId = "genart.pandabamboo",
            title = "#165 - Panda et bambou",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                PandaBambooEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                PandaBambooStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.CatTail,
            stableId = "genart.cattail",
            title = "#166 - Queue de chat qui balance",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                CatTailEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                CatTailStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.DogStretch,
            stableId = "genart.dogstretch",
            title = "#167 - Étirement de chien",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                DogStretchEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                DogStretchStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.PandaRoll,
            stableId = "genart.pandaroll",
            title = "#168 - Panda qui roule",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                PandaRollEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                PandaRollStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.SeasonSpringWash,
            stableId = "genart.seasonspringwash",
            title = "#169 - Lavis de printemps",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                SeasonSpringWashEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                SeasonSpringWashStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.SeasonAutumnWash,
            stableId = "genart.seasonautumnwash",
            title = "#170 - Lavis d'automne",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                SeasonAutumnWashEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                SeasonAutumnWashStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.SeasonWinterWash,
            stableId = "genart.seasonwinterwash",
            title = "#171 - Lavis d'hiver",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                SeasonWinterWashEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                SeasonWinterWashStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.FrostedWindowDawn,
            stableId = "genart.frostedwindowdawn",
            title = "#172 - Fenêtre givrée à l'aube",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                FrostedWindowDawnEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                FrostedWindowDawnStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.PaperLantern,
            stableId = "genart.paperlantern",
            title = "#173 - Lueur de lanterne en papier",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                PaperLanternEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                PaperLanternStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.HearthGlow,
            stableId = "genart.hearthglow",
            title = "#174 - Lueur d'âtre",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                HearthGlowEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                HearthGlowStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.VolumetricStairwell,
            stableId = "genart.volumetricstairwell",
            title = "#175 - Cage d'escalier volumétrique",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                VolumetricStairwellEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                VolumetricStairwellStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.WindowDustMotes,
            stableId = "genart.windowdustmotes",
            title = "#176 - Poussières dans la lumière",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                WindowDustMotesEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                WindowDustMotesStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.BookshelfGlow,
            stableId = "genart.bookshelfglow",
            title = "#177 - Lueur d'étagère",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                BookshelfGlowEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                BookshelfGlowStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.FloorLampPool,
            stableId = "genart.floorlamppool",
            title = "#178 - Mare de lampe sur pied",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                FloorLampPoolEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                FloorLampPoolStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.CrystalCavernGlow,
            stableId = "genart.crystalcavernglow",
            title = "#179 - Lueur de caverne de cristal",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                CrystalCavernGlowEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                CrystalCavernGlowStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.InkBloom,
            stableId = "genart.inkbloom",
            title = "#180 - Floraison d'encre",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                InkBloomEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                InkBloomStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.SumiWash,
            stableId = "genart.sumiwash",
            title = "#181 - Lavis sumi",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                SumiWashEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                SumiWashStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.InkTendril,
            stableId = "genart.inktendril",
            title = "#182 - Vrille d'encre",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                InkTendrilEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                InkTendrilStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.KaleidoPetal,
            stableId = "genart.kaleidopetal",
            title = "#183 - Pétale kaléido",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                KaleidoPetalEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                KaleidoPetalStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.KaleidoMirror,
            stableId = "genart.kaleidomirror",
            title = "#184 - Miroir kaléido",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                KaleidoMirrorEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                KaleidoMirrorStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.KaleidoOrbit,
            stableId = "genart.kaleidoorbit",
            title = "#185 - Orbite kaléido",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                KaleidoOrbitEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                KaleidoOrbitStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.ZenEnSo,
            stableId = "genart.zenenso",
            title = "#186 - Enso zen",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                ZenEnSoEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                ZenEnSoStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.ZenStoneRipples,
            stableId = "genart.zenstoneripples",
            title = "#187 - Rides de pierre zen",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                ZenStoneRipplesEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                ZenStoneRipplesStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.ZenIncense,
            stableId = "genart.zenincense",
            title = "#188 - Encens zen",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                ZenIncenseEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                ZenIncenseStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.ObsidianShore,
            stableId = "genart.obsidianshore",
            title = "#189 - Rivage d'obsidienne",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                ObsidianShoreEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                ObsidianShoreStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.TideFoam,
            stableId = "genart.tidefoam",
            title = "#190 - Écume de marée",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                TideFoamEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                TideFoamStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.CoastCliffHaze,
            stableId = "genart.coastcliffhaze",
            title = "#191 - Brume de falaise",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                CoastCliffHazeEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                CoastCliffHazeStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.DreamVeil,
            stableId = "genart.dreamveil",
            title = "#192 - Voile de rêve",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                DreamVeilEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                DreamVeilStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.LucidOrbs,
            stableId = "genart.lucidorbs",
            title = "#193 - Orbes lucides",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                LucidOrbsEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                LucidOrbsStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.SleepParallax,
            stableId = "genart.sleepparallax",
            title = "#194 - Parallaxe de sommeil",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                SleepParallaxEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                SleepParallaxStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.MicroDew,
            stableId = "genart.microdew",
            title = "#195 - Micro-rosée",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                MicroDewEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                MicroDewStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.MicroFiber,
            stableId = "genart.microfiber",
            title = "#196 - Micro-fibre",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                MicroFiberEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                MicroFiberStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.MicroDust,
            stableId = "genart.microdust",
            title = "#197 - Micro-poussière",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                MicroDustEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                MicroDustStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.MicroPollenClose,
            stableId = "genart.micropollenclose",
            title = "#198 - Micro-pollen de près",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                MicroPollenCloseEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                MicroPollenCloseStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.MicroBubbleSkin,
            stableId = "genart.microbubbleskin",
            title = "#199 - Peau de micro-bulle",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                MicroBubbleSkinEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                MicroBubbleSkinStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.AutoDashSoft,
            stableId = "genart.autodashsoft",
            title = "#200 - Voyage auto — tableau de bord",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                AutoDashSoftEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                AutoDashSoftStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.AutoRainGlass,
            stableId = "genart.autorainonglass",
            title = "#201 - Voyage auto — pluie sur vitre",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                AutoRainGlassEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                AutoRainGlassStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.AutoDistantHeadlights,
            stableId = "genart.autodistantheadlights",
            title = "#202 - Voyage auto — phares au loin",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                AutoDistantHeadlightsEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                AutoDistantHeadlightsStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.AutoDashboardGlow,
            stableId = "genart.autodashboardglow",
            title = "#203 - Voyage auto — lueur du tableau",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                AutoDashboardGlowEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                AutoDashboardGlowStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.AutoTunnelLight,
            stableId = "genart.autotunnellight",
            title = "#204 - Voyage auto — rythme du tunnel",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                AutoTunnelLightEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                AutoTunnelLightStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.AutoHorizonDrive,
            stableId = "genart.autohorizondrive",
            title = "#205 - Voyage auto — horizon",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                AutoHorizonDriveEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                AutoHorizonDriveStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.MotoHeadlight,
            stableId = "genart.motoheadlight",
            title = "#206 - Voyage moto — phare unique",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                MotoHeadlightEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                MotoHeadlightStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.MotoWindStreaks,
            stableId = "genart.motowindstreaks",
            title = "#207 - Voyage moto — traînées de vent",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                MotoWindStreaksEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                MotoWindStreaksStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.MotoRoadBlur,
            stableId = "genart.motoroadblur",
            title = "#208 - Voyage moto — flou de route",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                MotoRoadBlurEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                MotoRoadBlurStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.MotoNightLean,
            stableId = "genart.motonightlean",
            title = "#209 - Voyage moto — inclinaison nocturne",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                MotoNightLeanEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                MotoNightLeanStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.MotoTailGlow,
            stableId = "genart.mototailglow",
            title = "#210 - Voyage moto — lueur arrière",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                MotoTailGlowEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                MotoTailGlowStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.MotoDawnRide,
            stableId = "genart.motodawnride",
            title = "#211 - Voyage moto — aube",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                MotoDawnRideEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                MotoDawnRideStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.AvionHorizonClouds,
            stableId = "genart.avionhorizonclouds",
            title = "#212 - Voyage avion — nuages d'horizon",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                AvionHorizonCloudsEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                AvionHorizonCloudsStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.AvionContrail,
            stableId = "genart.avioncontrail",
            title = "#213 - Voyage avion — traînée de condensation",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                AvionContrailEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                AvionContrailStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.AvionWingGlow,
            stableId = "genart.avionwingglow",
            title = "#214 - Voyage avion — lueur d'aile",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                AvionWingGlowEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                AvionWingGlowStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.AvionCabinWindow,
            stableId = "genart.avioncabinwindow",
            title = "#215 - Voyage avion — hublot",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                AvionCabinWindowEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                AvionCabinWindowStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.AvionSunriseBand,
            stableId = "genart.avionsunriseband",
            title = "#216 - Voyage avion — bande de lever de soleil",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                AvionSunriseBandEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                AvionSunriseBandStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.AvionNightCity,
            stableId = "genart.avionnightcity",
            title = "#217 - Voyage avion — ville la nuit",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                AvionNightCityEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                AvionNightCityStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.VeloPathFields,
            stableId = "genart.velopathfields",
            title = "#218 - Voyage vélo — chemin et champs",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                VeloPathFieldsEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                VeloPathFieldsStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.VeloParallaxTrees,
            stableId = "genart.veloparallaxtrees",
            title = "#219 - Voyage vélo — arbres en parallaxe",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                VeloParallaxTreesEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                VeloParallaxTreesStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.VeloDawnLane,
            stableId = "genart.velodawnlane",
            title = "#220 - Voyage vélo — voie à l'aube",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                VeloDawnLaneEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                VeloDawnLaneStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.VeloRainAsphalt,
            stableId = "genart.velorainasphalt",
            title = "#221 - Voyage vélo — pluie sur asphalte",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                VeloRainAsphaltEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                VeloRainAsphaltStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.VeloGoldenHour,
            stableId = "genart.velogoldenhour",
            title = "#222 - Voyage vélo — heure dorée",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                VeloGoldenHourEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                VeloGoldenHourStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.VeloForestCanopy,
            stableId = "genart.veloforestcanopy",
            title = "#223 - Voyage vélo — canopée",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                VeloForestCanopyEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                VeloForestCanopyStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.TrainWindowBlur,
            stableId = "genart.trainwindowblur",
            title = "#224 - Voyage train — flou de fenêtre",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                TrainWindowBlurEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                TrainWindowBlurStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.TrainCatenary,
            stableId = "genart.traincatenary",
            title = "#225 - Voyage train — caténaire douce",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                TrainCatenaryEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                TrainCatenaryStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.TrainPlatformGlow,
            stableId = "genart.trainplatformglow",
            title = "#226 - Voyage train — lueur de quai",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                TrainPlatformGlowEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                TrainPlatformGlowStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.TrainCountryside,
            stableId = "genart.traincountryside",
            title = "#227 - Voyage train — campagne",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                TrainCountrysideEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                TrainCountrysideStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.TrainNightCarriage,
            stableId = "genart.trainnightcarriage",
            title = "#228 - Voyage train — wagon de nuit",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                TrainNightCarriageEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                TrainNightCarriageStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.TrainTunnelPulse,
            stableId = "genart.traintunnelpulse",
            title = "#229 - Voyage train — pulsation de tunnel",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                TrainTunnelPulseEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                TrainTunnelPulseStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.UsDinerNeon,
            stableId = "genart.usdinerneon",
            title = "#230 - Diner US néon",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                UsDinerNeonEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                UsDinerNeonStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.UsMotelSign,
            stableId = "genart.usmotelsign",
            title = "#231 - Enseigne de motel US",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                UsMotelSignEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                UsMotelSignStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.UsRoute66Wash,
            stableId = "genart.usroute66wash",
            title = "#232 - Route 66 — lavis désert",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                UsRoute66WashEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                UsRoute66WashStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.UsCrtArcade,
            stableId = "genart.uscrtarcade",
            title = "#233 - Arcade CRT US",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                UsCrtArcadeEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                UsCrtArcadeStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.UsRetrowave,
            stableId = "genart.usretrowave",
            title = "#234 - Horizon retrowave US",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                UsRetrowaveEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                UsRetrowaveStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.UsDriveIn,
            stableId = "genart.usdrivein",
            title = "#235 - Lueur de drive-in US",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                UsDriveInEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                UsDriveInStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.StormStrobeFlash,
            stableId = "genart.stormstrobeflash",
            title = "#236 - Orage strobo — flash",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                StormStrobeFlashEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                StormStrobeFlashStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
            autoSafe = false,
            tvDreamSafe = false,
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.StormStrobeSheet,
            stableId = "genart.stormstrobesheet",
            title = "#237 - Orage strobo — éclair en nappe",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                StormStrobeSheetEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                StormStrobeSheetStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
            autoSafe = false,
            tvDreamSafe = false,
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.StormStrobePulse,
            stableId = "genart.stormstrobepulse",
            title = "#238 - Orage strobo — pulsation",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                StormStrobePulseEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                StormStrobePulseStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
            autoSafe = false,
            tvDreamSafe = false,
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.StormStrobeFork,
            stableId = "genart.stormstrobefork",
            title = "#239 - Orage strobo — fourche",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                StormStrobeForkEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                StormStrobeForkStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
            autoSafe = false,
            tvDreamSafe = false,
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.StormStrobeHorizon,
            stableId = "genart.stormstrobehorizon",
            title = "#240 - Orage strobo — horizon",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                StormStrobeHorizonEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                StormStrobeHorizonStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
            autoSafe = false,
            tvDreamSafe = false,
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.StormStrobeCloud,
            stableId = "genart.stormstrobecloud",
            title = "#241 - Orage strobo — éclat de nuage",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                StormStrobeCloudEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                StormStrobeCloudStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
            autoSafe = false,
            tvDreamSafe = false,
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.PhysarumVeins,
            stableId = "genart.physarumveins",
            title = "#242 - Veines de physarum",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                PhysarumVeinsEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                PhysarumVeinsStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
            autoSafe = false,
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.PhysarumLattice,
            stableId = "genart.physarumlattice",
            title = "#243 - Treillis de physarum",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                PhysarumLatticeEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                PhysarumLatticeStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
            autoSafe = false,
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.PhysarumBloom,
            stableId = "genart.physarumbloom",
            title = "#244 - Floraison de physarum",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                PhysarumBloomEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                PhysarumBloomStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
            autoSafe = false,
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.PhysarumDrift,
            stableId = "genart.physarumdrift",
            title = "#245 - Physarum dérivant",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                PhysarumDriftEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                PhysarumDriftStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
            autoSafe = false,
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.PhysarumNode,
            stableId = "genart.physarumnode",
            title = "#246 - Nid de nœuds physarum",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                PhysarumNodeEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                PhysarumNodeStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
            autoSafe = false,
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.PhysarumFilament,
            stableId = "genart.physarumfilament",
            title = "#247 - Filament de physarum",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                PhysarumFilamentEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                PhysarumFilamentStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
            autoSafe = false,
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.ReactionSpots,
            stableId = "genart.reactionspots",
            title = "#248 - Réaction-diffusion — taches",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                ReactionSpotsEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                ReactionSpotsStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
            autoSafe = false,
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.ReactionCorals,
            stableId = "genart.reactioncorals",
            title = "#249 - Réaction-diffusion — coraux",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                ReactionCoralsEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                ReactionCoralsStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
            autoSafe = false,
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.ReactionWaves,
            stableId = "genart.reactionwaves",
            title = "#250 - Réaction-diffusion — vagues",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                ReactionWavesEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                ReactionWavesStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
            autoSafe = false,
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.ReactionMitosis,
            stableId = "genart.reactionmitosis",
            title = "#251 - Réaction-diffusion — mitose",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                ReactionMitosisEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                ReactionMitosisStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
            autoSafe = false,
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.ReactionMaze,
            stableId = "genart.reactionmaze",
            title = "#252 - Réaction-diffusion — labyrinthe",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                ReactionMazeEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                ReactionMazeStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
            autoSafe = false,
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.ReactionBlobs,
            stableId = "genart.reactionblobs",
            title = "#253 - Réaction — blobs doux",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                ReactionBlobsEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                ReactionBlobsStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
            autoSafe = false,
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.SoftTideRipples,
            stableId = "genart.softtideripples",
            title = "#254 - Rides de marée douce",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                SoftTideRipplesEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                SoftTideRipplesStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
        GenartEngineDescriptor(
            id = GenartEngineId.HarborMist,
            stableId = "genart.harbormist",
            title = "#255 - Brume de port",
            render = { isActive, palette, quality, brightness, speed, modifier ->
                HarborMistEngine(isActive, palette, quality, brightness, speed, modifier)
            },
            renderStill = { canvas, size, generation, phase, rotationDeg, pulse, palette ->
                HarborMistStill.draw(canvas, size, generation, phase, rotationDeg, pulse, palette)
            },
        ),
    )

    private val byStableId: Map<String, GenartEngineDescriptor> = all.associateBy { it.stableId }
    private val byEngineId: Map<GenartEngineId, GenartEngineDescriptor> = all.associateBy { it.id }

    fun catalogEntries(): List<GenartCatalogEntry> =
        all.map { GenartCatalogEntry(it.id, it.stableId, it.title) }

    fun engineForId(stableId: String): GenartEngineId? = byStableId[stableId]?.id

    fun descriptorFor(engineId: GenartEngineId): GenartEngineDescriptor = byEngineId.getValue(engineId)

    fun autoSafe(stableId: String): Boolean = byStableId[stableId]?.autoSafe != false

    fun tvDreamSafe(stableId: String): Boolean = byStableId[stableId]?.tvDreamSafe != false
}

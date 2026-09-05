package fr.geoking.arthur.genart

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import fr.geoking.arthur.genart.engines.AuroraEngine
import fr.geoking.arthur.genart.engines.ArcMosaicEngine
import fr.geoking.arthur.genart.engines.BirdFlockEngine
import fr.geoking.arthur.genart.engines.BreathCirclesEngine
import fr.geoking.arthur.genart.engines.BubblesEngine
import fr.geoking.arthur.genart.engines.CherryBlossomsEngine
import fr.geoking.arthur.genart.engines.CloudsEngine
import fr.geoking.arthur.genart.engines.ConstellationEngine
import fr.geoking.arthur.genart.engines.DunesEngine
import fr.geoking.arthur.genart.engines.FallingLeavesEngine
import fr.geoking.arthur.genart.engines.FireEmbersEngine
import fr.geoking.arthur.genart.engines.FirefliesEngine
import fr.geoking.arthur.genart.engines.FishSchoolEngine
import fr.geoking.arthur.genart.engines.FogEngine
import fr.geoking.arthur.genart.engines.GradientMeshEngine
import fr.geoking.arthur.genart.engines.GrassEngine
import fr.geoking.arthur.genart.engines.MeteorsEngine
import fr.geoking.arthur.genart.engines.MicroEngine
import fr.geoking.arthur.genart.engines.MorphingBlobsEngine
import fr.geoking.arthur.genart.engines.MountainsEngine
import fr.geoking.arthur.genart.engines.NebulaEngine
import fr.geoking.arthur.genart.engines.ParticlesEngine
import fr.geoking.arthur.genart.engines.PondRipplesEngine
import fr.geoking.arthur.genart.engines.Pseudo3DEngine
import fr.geoking.arthur.genart.engines.RainEngine
import fr.geoking.arthur.genart.engines.SilkFoldsEngine
import fr.geoking.arthur.genart.engines.SnowEngine
import fr.geoking.arthur.genart.engines.SoftNoiseFieldEngine
import fr.geoking.arthur.genart.engines.SoftRibbonsEngine
import fr.geoking.arthur.genart.engines.SoftShadowsEngine
import fr.geoking.arthur.genart.engines.SphereEngine
import fr.geoking.arthur.genart.engines.SunbeamsEngine
import fr.geoking.arthur.genart.engines.TonalGeometryEngine
import fr.geoking.arthur.genart.engines.TunnelEngine
import fr.geoking.arthur.genart.engines.VoronoiWashEngine
import fr.geoking.arthur.genart.engines.WavesEngine

/**
 * Dispatches to a procedural Canvas engine. Pauses visual intensity when [isActive] is false.
 */
@Composable
fun GenartEffectCanvas(
    engine: GenartEngineId,
    isActive: Boolean,
    modifier: Modifier = Modifier,
    paletteColors: List<Color> = TonalPalette.default(),
    quality: GenartQuality = GenartQuality.Medium,
) {
    val palette = paletteColors.ifEmpty { TonalPalette.default() }
    val brightness by animateFloatAsState(
        targetValue = if (isActive) 1.15f else 0.7f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "genart_brightness",
    )
    val speed by animateFloatAsState(
        targetValue = if (isActive) 1f else 0.35f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy),
        label = "genart_speed",
    )
    val canvasModifier = modifier.fillMaxSize()
    when (engine) {
        GenartEngineId.Particles -> ParticlesEngine(
            isActive = isActive,
            paletteColors = palette,
            quality = quality,
            brightness = brightness,
            speed = speed,
            modifier = canvasModifier,
        )
        GenartEngineId.Pseudo3D -> Pseudo3DEngine(
            isActive = isActive,
            paletteColors = palette,
            quality = quality,
            brightness = brightness,
            speed = speed,
            modifier = canvasModifier,
        )
        GenartEngineId.SoftShadows -> SoftShadowsEngine(
            isActive = isActive,
            paletteColors = palette,
            quality = quality,
            brightness = brightness,
            speed = speed,
            modifier = canvasModifier,
        )
        GenartEngineId.Tunnel -> TunnelEngine(
            isActive = isActive,
            paletteColors = palette,
            quality = quality,
            brightness = brightness,
            speed = speed,
            modifier = canvasModifier,
        )
        GenartEngineId.TonalGeometry -> TonalGeometryEngine(
            isActive = isActive,
            paletteColors = palette,
            quality = quality,
            brightness = brightness,
            speed = speed,
            modifier = canvasModifier,
        )
        GenartEngineId.Sphere -> SphereEngine(
            isActive = isActive,
            paletteColors = palette,
            quality = quality,
            brightness = brightness,
            speed = speed,
            modifier = canvasModifier,
        )
        GenartEngineId.Waves -> WavesEngine(
            isActive = isActive,
            paletteColors = palette,
            quality = quality,
            brightness = brightness,
            speed = speed,
            modifier = canvasModifier,
        )
        GenartEngineId.Micro -> MicroEngine(
            isActive = isActive,
            paletteColors = palette,
            quality = quality,
            brightness = brightness,
            speed = speed,
            modifier = canvasModifier,
        )
        GenartEngineId.Snow -> SnowEngine(
            isActive = isActive,
            paletteColors = palette,
            quality = quality,
            brightness = brightness,
            speed = speed,
            modifier = canvasModifier,
        )
        GenartEngineId.Grass -> GrassEngine(
            isActive = isActive,
            paletteColors = palette,
            quality = quality,
            brightness = brightness,
            speed = speed,
            modifier = canvasModifier,
        )
        GenartEngineId.BirdFlock -> BirdFlockEngine(
            isActive = isActive,
            paletteColors = palette,
            quality = quality,
            brightness = brightness,
            speed = speed,
            modifier = canvasModifier,
        )
        GenartEngineId.Mountains -> MountainsEngine(
            isActive = isActive,
            paletteColors = palette,
            quality = quality,
            brightness = brightness,
            speed = speed,
            modifier = canvasModifier,
        )
        GenartEngineId.Aurora -> AuroraEngine(
            isActive = isActive,
            paletteColors = palette,
            quality = quality,
            brightness = brightness,
            speed = speed,
            modifier = canvasModifier,
        )
        GenartEngineId.PondRipples -> PondRipplesEngine(
            isActive = isActive,
            paletteColors = palette,
            quality = quality,
            brightness = brightness,
            speed = speed,
            modifier = canvasModifier,
        )
        GenartEngineId.FallingLeaves -> FallingLeavesEngine(
            isActive = isActive,
            paletteColors = palette,
            quality = quality,
            brightness = brightness,
            speed = speed,
            modifier = canvasModifier,
        )
        GenartEngineId.BreathCircles -> BreathCirclesEngine(
            isActive = isActive,
            paletteColors = palette,
            quality = quality,
            brightness = brightness,
            speed = speed,
            modifier = canvasModifier,
        )
        GenartEngineId.FireEmbers -> FireEmbersEngine(
            isActive = isActive,
            paletteColors = palette,
            quality = quality,
            brightness = brightness,
            speed = speed,
            modifier = canvasModifier,
        )
        GenartEngineId.Dunes -> DunesEngine(
            isActive = isActive,
            paletteColors = palette,
            quality = quality,
            brightness = brightness,
            speed = speed,
            modifier = canvasModifier,
        )
        GenartEngineId.Constellation -> ConstellationEngine(
            isActive = isActive,
            paletteColors = palette,
            quality = quality,
            brightness = brightness,
            speed = speed,
            modifier = canvasModifier,
        )
        GenartEngineId.Clouds -> CloudsEngine(
            isActive = isActive,
            paletteColors = palette,
            quality = quality,
            brightness = brightness,
            speed = speed,
            modifier = canvasModifier,
        )
        GenartEngineId.Rain -> RainEngine(
            isActive = isActive,
            paletteColors = palette,
            quality = quality,
            brightness = brightness,
            speed = speed,
            modifier = canvasModifier,
        )
        GenartEngineId.Fog -> FogEngine(
            isActive = isActive,
            paletteColors = palette,
            quality = quality,
            brightness = brightness,
            speed = speed,
            modifier = canvasModifier,
        )
        GenartEngineId.FishSchool -> FishSchoolEngine(
            isActive = isActive,
            paletteColors = palette,
            quality = quality,
            brightness = brightness,
            speed = speed,
            modifier = canvasModifier,
        )
        GenartEngineId.Fireflies -> FirefliesEngine(
            isActive = isActive,
            paletteColors = palette,
            quality = quality,
            brightness = brightness,
            speed = speed,
            modifier = canvasModifier,
        )
        GenartEngineId.Sunbeams -> SunbeamsEngine(
            isActive = isActive,
            paletteColors = palette,
            quality = quality,
            brightness = brightness,
            speed = speed,
            modifier = canvasModifier,
        )
        GenartEngineId.Meteors -> MeteorsEngine(
            isActive = isActive,
            paletteColors = palette,
            quality = quality,
            brightness = brightness,
            speed = speed,
            modifier = canvasModifier,
        )
        GenartEngineId.Bubbles -> BubblesEngine(
            isActive = isActive,
            paletteColors = palette,
            quality = quality,
            brightness = brightness,
            speed = speed,
            modifier = canvasModifier,
        )
        GenartEngineId.CherryBlossoms -> CherryBlossomsEngine(
            isActive = isActive,
            paletteColors = palette,
            quality = quality,
            brightness = brightness,
            speed = speed,
            modifier = canvasModifier,
        )
        GenartEngineId.SoftRibbons -> SoftRibbonsEngine(
            isActive = isActive,
            paletteColors = palette,
            quality = quality,
            brightness = brightness,
            speed = speed,
            modifier = canvasModifier,
        )
        GenartEngineId.Nebula -> NebulaEngine(
            isActive = isActive,
            paletteColors = palette,
            quality = quality,
            brightness = brightness,
            speed = speed,
            modifier = canvasModifier,
        )
        GenartEngineId.MorphingBlobs -> MorphingBlobsEngine(
            isActive = isActive,
            paletteColors = palette,
            quality = quality,
            brightness = brightness,
            speed = speed,
            modifier = canvasModifier,
        )
        GenartEngineId.SoftNoiseField -> SoftNoiseFieldEngine(
            isActive = isActive,
            paletteColors = palette,
            quality = quality,
            brightness = brightness,
            speed = speed,
            modifier = canvasModifier,
        )
        GenartEngineId.VoronoiWash -> VoronoiWashEngine(
            isActive = isActive,
            paletteColors = palette,
            quality = quality,
            brightness = brightness,
            speed = speed,
            modifier = canvasModifier,
        )
        GenartEngineId.SilkFolds -> SilkFoldsEngine(
            isActive = isActive,
            paletteColors = palette,
            quality = quality,
            brightness = brightness,
            speed = speed,
            modifier = canvasModifier,
        )
        GenartEngineId.GradientMesh -> GradientMeshEngine(
            isActive = isActive,
            paletteColors = palette,
            quality = quality,
            brightness = brightness,
            speed = speed,
            modifier = canvasModifier,
        )
        GenartEngineId.ArcMosaic -> ArcMosaicEngine(
            isActive = isActive,
            paletteColors = palette,
            quality = quality,
            brightness = brightness,
            speed = speed,
            modifier = canvasModifier,
        )
    }
}

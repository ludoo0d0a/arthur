package fr.geoking.arthur.genart

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import fr.geoking.arthur.genart.engines.MicroEngine
import fr.geoking.arthur.genart.engines.ParticlesEngine
import fr.geoking.arthur.genart.engines.Pseudo3DEngine
import fr.geoking.arthur.genart.engines.SoftShadowsEngine
import fr.geoking.arthur.genart.engines.SphereEngine
import fr.geoking.arthur.genart.engines.TonalGeometryEngine
import fr.geoking.arthur.genart.engines.TunnelEngine
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
    }
}

package fr.geoking.arthur.genart

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import fr.geoking.arthur.genart.stills.AuroraStill
import fr.geoking.arthur.genart.stills.BirdFlockStill
import fr.geoking.arthur.genart.stills.BreathCirclesStill
import fr.geoking.arthur.genart.stills.BubblesStill
import fr.geoking.arthur.genart.stills.CherryBlossomsStill
import fr.geoking.arthur.genart.stills.CloudsStill
import fr.geoking.arthur.genart.stills.ConstellationStill
import fr.geoking.arthur.genart.stills.DunesStill
import fr.geoking.arthur.genart.stills.FallingLeavesStill
import fr.geoking.arthur.genart.stills.FireEmbersStill
import fr.geoking.arthur.genart.stills.FirefliesStill
import fr.geoking.arthur.genart.stills.FishSchoolStill
import fr.geoking.arthur.genart.stills.FogStill
import fr.geoking.arthur.genart.stills.GrassStill
import fr.geoking.arthur.genart.stills.MeteorsStill
import fr.geoking.arthur.genart.stills.MicroStill
import fr.geoking.arthur.genart.stills.MountainsStill
import fr.geoking.arthur.genart.stills.NebulaStill
import fr.geoking.arthur.genart.stills.ParticlesStill
import fr.geoking.arthur.genart.stills.PondRipplesStill
import fr.geoking.arthur.genart.stills.RainStill
import fr.geoking.arthur.genart.stills.SnowStill
import fr.geoking.arthur.genart.stills.SoftRibbonsStill
import fr.geoking.arthur.genart.stills.SphereStill
import fr.geoking.arthur.genart.stills.SunbeamsStill
import fr.geoking.arthur.genart.stills.WavesStill
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * android.graphics still bakers for Auto album art (no Compose / no animation).
 * [generation] selects palette index and phase/rotation so Ambient Rotation URIs look distinct.
 */
object GenartStillRenderer {
    fun draw(
        canvas: Canvas,
        engineId: GenartEngineId,
        size: Int,
        generation: Long,
        palette: AnimationPalette = AnimationPalettes.fromGeneration(generation),
    ) {
        val phase = ((generation % 360L).toFloat() / 360f) * (2f * PI.toFloat())
        val rotationDeg = ((generation * 37L) % 360L).toFloat()
        val pulse = 0.35f + ((generation % 100L).toFloat() / 100f) * 0.65f
        when (engineId) {
            GenartEngineId.Sphere -> SphereStill.draw(
                canvas = canvas,
                size = size,
                rotationDeg = rotationDeg,
                pulse = pulse,
                palette = palette,
            )
            GenartEngineId.Waves -> WavesStill.draw(
                canvas = canvas,
                size = size,
                phaseBase = phase,
                palette = palette,
            )
            GenartEngineId.Particles -> ParticlesStill.draw(
                canvas = canvas,
                size = size,
                seed = generation,
                time = phase * 0.02f,
                rotationDeg = rotationDeg,
                pulse = pulse,
                palette = palette,
            )
            GenartEngineId.Micro -> MicroStill.draw(
                canvas = canvas,
                size = size,
                rotationDeg = rotationDeg,
                pulseScale = 1f + pulse * 0.15f,
                palette = palette,
            )
            GenartEngineId.Snow -> SnowStill.draw(
                canvas, size, generation, phase, rotationDeg, pulse, palette,
            )
            GenartEngineId.Grass -> GrassStill.draw(
                canvas, size, generation, phase, rotationDeg, pulse, palette,
            )
            GenartEngineId.BirdFlock -> BirdFlockStill.draw(
                canvas, size, generation, phase, rotationDeg, pulse, palette,
            )
            GenartEngineId.Mountains -> MountainsStill.draw(
                canvas, size, generation, phase, rotationDeg, pulse, palette,
            )
            GenartEngineId.Aurora -> AuroraStill.draw(
                canvas, size, generation, phase, rotationDeg, pulse, palette,
            )
            GenartEngineId.PondRipples -> PondRipplesStill.draw(
                canvas, size, generation, phase, rotationDeg, pulse, palette,
            )
            GenartEngineId.FallingLeaves -> FallingLeavesStill.draw(
                canvas, size, generation, phase, rotationDeg, pulse, palette,
            )
            GenartEngineId.BreathCircles -> BreathCirclesStill.draw(
                canvas, size, generation, phase, rotationDeg, pulse, palette,
            )
            GenartEngineId.FireEmbers -> FireEmbersStill.draw(
                canvas, size, generation, phase, rotationDeg, pulse, palette,
            )
            GenartEngineId.Dunes -> DunesStill.draw(
                canvas, size, generation, phase, rotationDeg, pulse, palette,
            )
            GenartEngineId.Constellation -> ConstellationStill.draw(
                canvas, size, generation, phase, rotationDeg, pulse, palette,
            )
            GenartEngineId.Clouds -> CloudsStill.draw(
                canvas, size, generation, phase, rotationDeg, pulse, palette,
            )
            GenartEngineId.Rain -> RainStill.draw(
                canvas, size, generation, phase, rotationDeg, pulse, palette,
            )
            GenartEngineId.Fog -> FogStill.draw(
                canvas, size, generation, phase, rotationDeg, pulse, palette,
            )
            GenartEngineId.FishSchool -> FishSchoolStill.draw(
                canvas, size, generation, phase, rotationDeg, pulse, palette,
            )
            GenartEngineId.Fireflies -> FirefliesStill.draw(
                canvas, size, generation, phase, rotationDeg, pulse, palette,
            )
            GenartEngineId.Sunbeams -> SunbeamsStill.draw(
                canvas, size, generation, phase, rotationDeg, pulse, palette,
            )
            GenartEngineId.Meteors -> MeteorsStill.draw(
                canvas, size, generation, phase, rotationDeg, pulse, palette,
            )
            GenartEngineId.Bubbles -> BubblesStill.draw(
                canvas, size, generation, phase, rotationDeg, pulse, palette,
            )
            GenartEngineId.CherryBlossoms -> CherryBlossomsStill.draw(
                canvas, size, generation, phase, rotationDeg, pulse, palette,
            )
            GenartEngineId.SoftRibbons -> SoftRibbonsStill.draw(
                canvas, size, generation, phase, rotationDeg, pulse, palette,
            )
            GenartEngineId.Nebula -> NebulaStill.draw(
                canvas, size, generation, phase, rotationDeg, pulse, palette,
            )
            GenartEngineId.Pseudo3D,
            GenartEngineId.SoftShadows,
            GenartEngineId.Tunnel,
            GenartEngineId.TonalGeometry,
            -> drawFallback(canvas, size, generation, palette, engineId)
        }
    }

    private fun drawFallback(
        canvas: Canvas,
        size: Int,
        generation: Long,
        palette: AnimationPalette,
        engineId: GenartEngineId,
    ) {
        val rnd = Random(generation xor engineId.ordinal.toLong() * 0x9E3779B9L)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.shader = RadialGradient(
            size * 0.5f,
            size * 0.45f,
            size * 0.85f,
            intArrayOf(0xFF0B1220.toInt(), 0xFF020617.toInt()),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), paint)
        paint.shader = null

        val count = when (engineId) {
            GenartEngineId.Tunnel -> 28
            GenartEngineId.SoftShadows -> 36
            else -> 40
        }
        for (i in 0 until count) {
            val t = (i + rnd.nextFloat()) / count
            val angle = t * 2f * PI.toFloat() * (1.5f + rnd.nextFloat())
            val radius = size * (0.08f + 0.38f * t)
            val x = size * 0.5f + cos(angle) * radius
            val y = size * 0.48f + sin(angle * 0.9f) * radius * 0.85f
            val c = palette.colorAt(i)
            paint.color = Color.argb(
                90 + rnd.nextInt(120),
                Color.red(c),
                Color.green(c),
                Color.blue(c),
            )
            canvas.drawCircle(x, y, 3f + rnd.nextFloat() * 8f, paint)
        }
    }
}

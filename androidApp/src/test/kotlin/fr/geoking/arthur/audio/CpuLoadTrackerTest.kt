package fr.geoking.arthur.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CpuLoadTrackerTest {
    @Test
    fun lightLoadStaysFull() {
        val t = CpuLoadTracker(emaAlpha = 1f)
        // 10% of chunk budget
        t.observe(renderNs = 4_000_000L, chunkFrames = 2048, sampleRate = 48_000)
        assertEquals(SynthQuality.Full, t.quality)
        assertTrue(t.loadEma < 0.5f)
    }

    @Test
    fun heavyLoadDropsToMinimal() {
        val t = CpuLoadTracker(emaAlpha = 1f)
        // ~120% of chunk duration at 48k/2048 (~42.6ms)
        t.observe(renderNs = 52_000_000L, chunkFrames = 2048, sampleRate = 48_000)
        assertEquals(SynthQuality.Minimal, t.quality)
    }

    @Test
    fun mediumLoadIsReduced() {
        val t = CpuLoadTracker(emaAlpha = 1f)
        t.observe(renderNs = 34_000_000L, chunkFrames = 2048, sampleRate = 48_000)
        assertEquals(SynthQuality.Reduced, t.quality)
    }

    @Test
    fun combinedPadGainMergesWhenSinglePad() {
        val g = ProceduralMusicEngine.combinedPadGain(0.2f, 0.2f, dualPad = false)
        assertTrue(g > 0.2f)
        assertTrue(g <= 0.75f)
        assertEquals(0.2f, ProceduralMusicEngine.combinedPadGain(0.2f, 0.2f, dualPad = true), 0.001f)
    }

    @Test
    fun atmosphereUsesSingleTexture() {
        assertEquals(
            ProceduralMusicEngine.TextureKind.Waves,
            ProceduralMusicEngine.atmosphereTextureKind(
                MusicStyle.OceanWaves,
                atmosphere = true,
                textureGain = 0.3f,
            ),
        )
        assertEquals(
            ProceduralMusicEngine.TextureKind.Wind,
            ProceduralMusicEngine.atmosphereTextureKind(
                MusicStyle.JazzPiano,
                atmosphere = true,
                textureGain = 0.3f,
            ),
        )
        assertEquals(
            ProceduralMusicEngine.TextureKind.None,
            ProceduralMusicEngine.atmosphereTextureKind(
                MusicStyle.OceanWaves,
                atmosphere = false,
                textureGain = 0.3f,
            ),
        )
    }
}

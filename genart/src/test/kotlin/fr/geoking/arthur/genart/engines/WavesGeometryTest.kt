package fr.geoking.arthur.genart.engines

import fr.geoking.arthur.genart.GenartQuality
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.abs

class WavesGeometryTest {
    @Test
    fun tessellationIsBoundedByQualityAndHandlesEmptySizes() {
        assertEquals(2, wavesSegments(0f, GenartQuality.High))
        assertEquals(2, wavesSegments(Float.NaN, GenartQuality.High))
        assertEquals(128, wavesSegments(3840f, GenartQuality.Low))
        assertEquals(256, wavesSegments(3840f, GenartQuality.Medium))
        assertEquals(384, wavesSegments(3840f, GenartQuality.High))
        assertEquals(384, wavesSegments(100000f, GenartQuality.High))
        assertEquals(60, wavesSegments(360f, GenartQuality.High))
    }

    @Test
    fun crestPositionAndVelocityLoop() {
        val period = 2f * PI.toFloat()
        val dt = 0.001f
        for (x in listOf(0f, 120f, 1500f)) {
            fun height(phase: Float) = wavesHeight(x, 200f, 82.5f, 0.018f, phase)
            assertEquals(height(0f), height(period), 0.001f)
            val before = (height(0f) - height(-dt)) / dt
            val after = (height(period + dt) - height(period)) / dt
            assertEquals(before, after, 0.1f)
        }
    }

    @Test
    fun highQualityCrestInterpolationStaysCloseAt4k() {
        val width = 3840f
        val segments = wavesSegments(width, GenartQuality.High)
        val step = width / segments
        for (phase in listOf(0f, 1f, 3f, 5f)) {
            for (i in 0 until segments) {
                val x = i * step
                val left = wavesHeight(x, 0f, 82.5f, 0.018f, phase)
                val right = wavesHeight(x + step, 0f, 82.5f, 0.018f, phase)
                val middle = wavesHeight(x + step / 2f, 0f, 82.5f, 0.018f, phase)
                assertTrue(abs(middle - (left + right) / 2f) < 0.6f)
            }
        }
    }
}

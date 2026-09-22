package fr.geoking.arthur.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StillContentScaleTest {

    private val dstW = 1920f
    private val dstH = 1080f // 16:9

    @Test
    fun matchingRatio_scaleEqualsContainAndCover() {
        val srcW = 1600f
        val srcH = 900f
        val contain = minOf(dstW / srcW, dstH / srcH)
        val cover = maxOf(dstW / srcW, dstH / srcH)
        assertEquals(contain, cover, 1e-5f)
        assertEquals(contain, StillContentScale.scale(srcW, srcH, dstW, dstH), 1e-5f)
        assertEquals(0f, StillContentScale.coverCropFraction(srcW, srcH, dstW, dstH), 1e-5f)
    }

    @Test
    fun nearRatio_16x10_in_16x9_usesCover() {
        // 16:10 into 16:9 → cover crops height ≈ 10% ≤ 20%
        val srcW = 1600f
        val srcH = 1000f
        val cover = maxOf(dstW / srcW, dstH / srcH)
        val crop = StillContentScale.coverCropFraction(srcW, srcH, dstW, dstH)
        assertTrue("expected cover crop ~10%, got $crop", crop in 0.09f..0.11f)
        assertEquals(cover, StillContentScale.scale(srcW, srcH, dstW, dstH), 1e-5f)
    }

    @Test
    fun panoramic_3x1_in_16x9_capsAtContainOverPointEight() {
        val srcW = 3000f
        val srcH = 1000f
        val contain = minOf(dstW / srcW, dstH / srcH)
        val cover = maxOf(dstW / srcW, dstH / srcH)
        val crop = StillContentScale.coverCropFraction(srcW, srcH, dstW, dstH)
        assertTrue("expected cover crop ~41%, got $crop", crop in 0.35f..0.45f)
        val expected = contain / (1f - StillContentScale.MaxCropFraction)
        assertEquals(expected, StillContentScale.scale(srcW, srcH, dstW, dstH), 1e-5f)
        assertTrue(StillContentScale.scale(srcW, srcH, dstW, dstH) < cover)
    }

    @Test
    fun square_in_16x9_capsAtContainOverPointEight() {
        val srcW = 1000f
        val srcH = 1000f
        val contain = minOf(dstW / srcW, dstH / srcH)
        val cover = maxOf(dstW / srcW, dstH / srcH)
        val crop = StillContentScale.coverCropFraction(srcW, srcH, dstW, dstH)
        assertTrue("expected cover crop ~44%, got $crop", crop in 0.40f..0.50f)
        val expected = contain / (1f - StillContentScale.MaxCropFraction)
        assertEquals(expected, StillContentScale.scale(srcW, srcH, dstW, dstH), 1e-5f)
        assertTrue(StillContentScale.scale(srcW, srcH, dstW, dstH) < cover)
    }

    @Test
    fun portrait_9x16_in_16x9_capsAtTwentyPercentCrop() {
        val srcW = 900f
        val srcH = 1600f
        val contain = minOf(dstW / srcW, dstH / srcH)
        val cover = maxOf(dstW / srcW, dstH / srcH)
        val crop = StillContentScale.coverCropFraction(srcW, srcH, dstW, dstH)
        assertTrue("expected large cover crop, got $crop", crop > StillContentScale.MaxCropFraction)
        val expected = contain / (1f - StillContentScale.MaxCropFraction)
        assertEquals(expected, StillContentScale.scale(srcW, srcH, dstW, dstH), 1e-5f)
        assertTrue(StillContentScale.scale(srcW, srcH, dstW, dstH) < cover)
    }

    @Test
    fun zeroDimensions_returnsOne() {
        assertEquals(1f, StillContentScale.scale(0f, 100f, dstW, dstH), 0f)
        assertEquals(0f, StillContentScale.coverCropFraction(0f, 100f, dstW, dstH), 0f)
    }
}

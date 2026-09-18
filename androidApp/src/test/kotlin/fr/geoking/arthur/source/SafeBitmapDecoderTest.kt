package fr.geoking.arthur.source

import org.junit.Assert.assertEquals
import org.junit.Test

class SafeBitmapDecoderTest {
    @Test
    fun sampleSize_keepsSmallImagesUnsampled() {
        assertEquals(1, SafeBitmapDecoder.sampleSize(800, 600, 2048))
        assertEquals(1, SafeBitmapDecoder.sampleSize(2160, 2160, 2160))
    }

    @Test
    fun sampleSize_downscalesLargePhotos() {
        assertEquals(2, SafeBitmapDecoder.sampleSize(4000, 3000, 2048))
        assertEquals(4, SafeBitmapDecoder.sampleSize(8000, 6000, 2048))
        // 6000 / 4 = 1500 still > 2160? no — 6000/2=3000>2160, /4=1500≤2160 → 4
        assertEquals(4, SafeBitmapDecoder.sampleSize(6000, 4000, 2160))
    }
}

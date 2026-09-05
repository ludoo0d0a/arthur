package fr.geoking.arthur.source

import org.junit.Assert.assertEquals
import org.junit.Test

class SafeBitmapDecoderTest {
    @Test
    fun sampleSize_keepsSmallImagesUnsampled() {
        assertEquals(1, SafeBitmapDecoder.sampleSize(800, 600, 2048))
        assertEquals(1, SafeBitmapDecoder.sampleSize(720, 720, 720))
    }

    @Test
    fun sampleSize_downscalesLargePhotos() {
        assertEquals(2, SafeBitmapDecoder.sampleSize(4000, 3000, 2048))
        assertEquals(4, SafeBitmapDecoder.sampleSize(8000, 6000, 2048))
        // 6000 / 8 = 750 still > 720, so another step to 16
        assertEquals(16, SafeBitmapDecoder.sampleSize(6000, 4000, 720))
    }
}

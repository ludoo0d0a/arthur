package fr.geoking.arthur.auto

import android.support.v4.media.MediaBrowserCompat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pure mapping tests for Media browse ids (no device). */
class ArthurMediaMappingTest {
    @Test
    fun rootId_isStable() {
        assertEquals("arthur_root", ArthurMediaService.ROOT)
    }

    @Test
    fun playableFlag_isSet() {
        assertTrue(MediaBrowserCompat.MediaItem.FLAG_PLAYABLE != 0)
    }
}

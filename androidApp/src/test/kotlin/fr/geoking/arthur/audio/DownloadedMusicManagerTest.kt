package fr.geoking.arthur.audio

import androidx.test.core.app.ApplicationProvider
import fr.geoking.arthur.audio.remote.DownloadedMusicManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DownloadedMusicManagerTest {

    @Test
    fun getDownloadedTracksReturnsEmptyOnNewStore() {
        val manager = DownloadedMusicManager(ApplicationProvider.getApplicationContext())
        val tracks = manager.getDownloadedTracks()
        assertEquals(0, tracks.size)
        assertFalse(manager.isDownloaded("unknown-id"))
        assertNull(manager.getTrackFile("unknown-id"))
    }

    @Test
    fun deleteTrackRemovesTrackFromManifest() {
        val manager = DownloadedMusicManager(ApplicationProvider.getApplicationContext())
        manager.deleteTrack("non-existent-id")
        assertEquals(0, manager.getDownloadedTracks().size)
    }
}

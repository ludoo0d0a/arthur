package fr.geoking.arthur.source

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import fr.geoking.arthur.shared.domain.Artwork
import fr.geoking.arthur.shared.domain.ArtworkKind
import fr.geoking.arthur.shared.source.MetSource
import fr.geoking.arthur.shared.source.RijksmuseumSource
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class ArtworkImageCacheQueryTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun rememberArtworksAndLoadCachedArtworksBySourceIdsAndKind() {
        val cache = ArtworkImageCache(context)
        val metArt = Artwork(
            id = "met-100",
            title = "Met Painting",
            attribution = "Met Artist",
            sourceId = MetSource.ID,
            kind = ArtworkKind.Painting,
            remoteUrl = "https://example.com/met.jpg",
        )
        val rijksArt = Artwork(
            id = "rijks-200",
            title = "Rijks Sculpture",
            attribution = "Rijks Artist",
            sourceId = RijksmuseumSource.ID,
            kind = ArtworkKind.Sculpture,
            remoteUrl = "https://example.com/rijks.jpg",
        )

        cache.rememberArtworks(listOf(metArt, rijksArt))
        cache.putImage(metArt.id, ByteArray(2_000) { 0x11 })
        cache.putImage(rijksArt.id, ByteArray(2_000) { 0x22 })

        val metLoaded = cache.loadCachedArtworks(sourceIds = listOf(MetSource.ID))
        assertEquals(1, metLoaded.size)
        assertEquals(metArt.id, metLoaded[0].id)

        val sculptureLoaded = cache.loadCachedArtworks(kind = ArtworkKind.Sculpture)
        assertEquals(1, sculptureLoaded.size)
        assertEquals(rijksArt.id, sculptureLoaded[0].id)

        val allLoaded = cache.loadCachedArtworks()
        assertEquals(2, allLoaded.size)
    }
}

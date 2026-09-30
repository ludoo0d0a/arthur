package fr.geoking.arthur.ui.screens

import androidx.compose.ui.test.junit4.v2.createComposeRule
import fr.geoking.arthur.phone.theme.ArthurTheme
import fr.geoking.arthur.shared.domain.Artwork
import fr.geoking.arthur.shared.domain.ArtworkKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AmbientPrefetchRenewalTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val art1 = Artwork("art-1", "Title 1", "Author 1", "source", ArtworkKind.Photo, remoteUrl = "https://example.com/1.jpg")
    private val art2 = Artwork("art-2", "Title 2", "Author 2", "source", ArtworkKind.Photo, remoteUrl = "https://example.com/2.jpg")
    private val art3 = Artwork("art-3", "Title 3", "Author 3", "source", ArtworkKind.Photo, remoteUrl = "https://example.com/3.jpg")
    private val art4 = Artwork("art-4", "Title 4", "Author 4", "source", ArtworkKind.Photo, remoteUrl = "https://example.com/4.jpg")

    @Test
    fun poolRenewal_combinesDistinctArtworks() {
        val currentPool = listOf(art1, art2)
        val fetchedCatalog = listOf(art2, art3, art4)

        val currentPoolIds = currentPool.map { it.id }.toSet()
        val distinctNew = fetchedCatalog.filter { it.id !in currentPoolIds }
        val combinedPool = (currentPool + distinctNew).distinctBy { it.id }

        assertEquals(4, combinedPool.size)
        assertTrue(combinedPool.containsAll(listOf(art1, art2, art3, art4)))
    }

    @Test
    fun poolRenewal_fallbackWhenNoNewDistinctArtworks() {
        val currentPool = listOf(art1, art2)
        val fetchedCatalog = listOf(art1, art2)

        val currentPoolIds = currentPool.map { it.id }.toSet()
        val distinctNew = fetchedCatalog.filter { it.id !in currentPoolIds }

        assertTrue(distinctNew.isEmpty())
        val combinedPool = if (distinctNew.isNotEmpty()) {
            (currentPool + distinctNew).distinctBy { it.id }
        } else {
            fetchedCatalog
        }

        assertEquals(2, combinedPool.size)
        assertEquals(currentPool, combinedPool)
    }
}

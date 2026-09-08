package fr.geoking.arthur.shared.source

import fr.geoking.arthur.shared.domain.Artwork
import fr.geoking.arthur.shared.domain.ArtworkKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking

class MuseumLoadTest {
    private suspend fun loadOnce(cursor: () -> Int) = MuseumLoad.acrossTargets(
        kind = MuseumSearchKind.All,
        limit = 4,
        random = ZeroRandom,
        nextTargetIndex = cursor,
    ) { target, perKind ->
        List(perKind) { i ->
            Artwork(
                id = "${target.name}-$i",
                title = target.name,
                sourceId = "test",
                kind = target.artworkKind ?: ArtworkKind.Painting,
            )
        }
    }

    @Test
    fun acrossTargetsFetchesOnlyOneTargetPerCall() = runBlocking {
        var cursor = 0
        val loaded = loadOnce { cursor++ }
        // All → [Painting, Sculpture]; index 0 picks Painting, full limit from it alone.
        assertEquals(4, loaded.size)
        assertEquals(
            listOf("Painting-0", "Painting-1", "Painting-2", "Painting-3"),
            loaded.map { it.id },
        )
    }

    @Test
    fun acrossTargetsRotatesToTheNextTargetOnASecondCall() = runBlocking {
        var cursor = 0
        loadOnce { cursor++ }
        val second = loadOnce { cursor++ }
        assertEquals(
            listOf("Sculpture-0", "Sculpture-1", "Sculpture-2", "Sculpture-3"),
            second.map { it.id },
        )
    }
}

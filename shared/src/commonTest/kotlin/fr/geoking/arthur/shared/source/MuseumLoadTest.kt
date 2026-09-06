package fr.geoking.arthur.shared.source

import fr.geoking.arthur.shared.domain.Artwork
import fr.geoking.arthur.shared.domain.ArtworkKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking

class MuseumLoadTest {
    @Test
    fun acrossTargetsSplitsLimitAndSamples() = runBlocking {
        val loaded = MuseumLoad.acrossTargets(
            kind = MuseumSearchKind.All,
            limit = 4,
            random = ZeroRandom,
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
        // All → Painting + Sculpture; ZeroRandom keeps order then samples to 4.
        assertEquals(4, loaded.size)
        assertEquals(
            listOf("Painting-0", "Painting-1", "Sculpture-0", "Sculpture-1"),
            loaded.map { it.id },
        )
    }
}

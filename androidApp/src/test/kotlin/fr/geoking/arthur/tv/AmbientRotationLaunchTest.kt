package fr.geoking.arthur.tv

import fr.geoking.arthur.shared.domain.Artwork
import fr.geoking.arthur.shared.domain.ArtworkKind
import fr.geoking.arthur.shared.domain.PremiumEntitlement
import fr.geoking.arthur.shared.domain.Source
import fr.geoking.arthur.shared.engine.ContentEngine
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AmbientRotationLaunchTest {

    @Test
    fun prepare_storesPoolForAmbientActivity() {
        val pool = listOf(
            Artwork(id = "a", title = "A", sourceId = "bundled", kind = ArtworkKind.Photo),
            Artwork(id = "b", title = "B", sourceId = "genart", kind = ArtworkKind.Genart),
        )
        AmbientRotationLaunch.prepare(pool)
        assertEquals(pool, AmbientRotationLaunch.pool)
    }

    @Test
    fun loadRotatingAmbient_prefersStashedPoolAndRequestedSeed() = runBlocking {
        val engine = ContentEngine(
            sources = listOf(
                object : Source {
                    override val id = "empty"
                    override val displayName = "Empty"
                    override suspend fun load(): List<Artwork> = emptyList()
                },
            ),
            entitlement = object : PremiumEntitlement {
                override val isPremium = true
            },
        )
        val stashed = listOf(
            Artwork(
                id = "pexels-1",
                title = "Stock",
                sourceId = "pexels",
                kind = ArtworkKind.Photo,
                remoteUrl = "https://example.com/a.jpg",
            ),
            Artwork(
                id = "genart.snow",
                title = "Snow",
                sourceId = "genart",
                kind = ArtworkKind.Genart,
            ),
        )
        val requested = stashed[0]
        val (pool, artwork) = loadRotatingAmbient(engine, requested, stashed)
        assertEquals(stashed, pool)
        assertEquals(requested.id, artwork?.id)
    }

    @Test
    fun loadRotatingAmbient_emptyStashFallsBackToEngineCatalog() = runBlocking {
        val art = Artwork(
            id = "bundled-1",
            title = "Harbor",
            sourceId = "bundled",
            kind = ArtworkKind.Photo,
            localPath = "/tmp/a.jpg",
        )
        val engine = ContentEngine(
            sources = listOf(
                object : Source {
                    override val id = "bundled"
                    override val displayName = "Bundled"
                    override suspend fun load(): List<Artwork> = listOf(art)
                },
            ),
            entitlement = object : PremiumEntitlement {
                override val isPremium = true
            },
        )
        val (pool, artwork) = loadRotatingAmbient(engine, requested = null, stashedPool = emptyList())
        assertEquals(listOf(art), pool)
        assertTrue(artwork != null)
    }

    @Test
    fun loadPinnedAmbient_nullRequest_doesNotInventGenerative() = runBlocking {
        val particles = Artwork(
            id = "genart.particles",
            title = "Drifting Particles",
            sourceId = "genart",
            kind = ArtworkKind.Genart,
        )
        val engine = ContentEngine(
            sources = listOf(
                object : Source {
                    override val id = "genart"
                    override val displayName = "Genart"
                    override suspend fun load(): List<Artwork> = listOf(particles)
                },
            ),
            entitlement = object : PremiumEntitlement {
                override val isPremium = true
            },
        )
        assertEquals(null, loadPinnedAmbient(engine, requested = null))
    }

    @Test
    fun loadRotatingAmbient_nullRequest_picksFromPoolOnly() = runBlocking {
        val met = Artwork(
            id = "met-1",
            title = "Met",
            sourceId = "met",
            kind = ArtworkKind.Painting,
            remoteUrl = "https://example.com/m.jpg",
        )
        val engine = ContentEngine(
            sources = listOf(
                object : Source {
                    override val id = "empty"
                    override val displayName = "Empty"
                    override suspend fun load(): List<Artwork> = emptyList()
                },
            ),
            entitlement = object : PremiumEntitlement {
                override val isPremium = true
            },
        )
        val (pool, artwork) = loadRotatingAmbient(
            engine,
            requested = null,
            stashedPool = listOf(met),
        )
        assertEquals(listOf(met), pool)
        assertEquals(met.id, artwork?.id)
    }
}

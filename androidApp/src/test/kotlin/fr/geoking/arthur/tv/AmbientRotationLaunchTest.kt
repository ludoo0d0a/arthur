package fr.geoking.arthur.tv

import fr.geoking.arthur.shared.domain.Artwork
import fr.geoking.arthur.shared.domain.ArtworkKind
import fr.geoking.arthur.shared.domain.PremiumEntitlement
import fr.geoking.arthur.shared.domain.Source
import fr.geoking.arthur.shared.engine.ContentEngine
import fr.geoking.arthur.auto.AmbientAlbumArt
import fr.geoking.arthur.ui.components.PackFamily
import fr.geoking.arthur.ui.components.PackSelection
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
        AmbientRotationLaunch.prepare(pool, renewSourceIds = listOf("met"))
        assertEquals(pool.map { it.id }.toSet(), AmbientRotationLaunch.pool.map { it.id }.toSet())
        assertEquals(listOf("met"), AmbientRotationLaunch.renewSourceIds)
    }

    @Test
    fun prepare_capsPoolToMaxAutoRotation() {
        val pool = (1..8).map { i ->
            Artwork(id = "art-$i", title = "A$i", sourceId = "bundled", kind = ArtworkKind.Photo)
        }
        AmbientRotationLaunch.prepare(pool)
        assertEquals(AmbientAlbumArt.MAX_AUTO_ROTATION_POOL, AmbientRotationLaunch.pool.size)
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
        assertEquals(stashed.map { it.id }.toSet(), pool.map { it.id }.toSet())
        assertEquals(requested.id, artwork?.id)
        assertEquals(requested.id, pool.first().id)
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

    @Test
    fun loadDreamAmbient_withSelection_filtersForSelectedPack() = runBlocking {
        val painting = Artwork(
            id = "met-painting",
            title = "Mona Lisa",
            sourceId = "met",
            kind = ArtworkKind.Painting,
            remoteUrl = "https://example.com/p.jpg",
        )
        val photo = Artwork(
            id = "pexels-photo",
            title = "Forest",
            sourceId = "pexels",
            kind = ArtworkKind.Photo,
            remoteUrl = "https://example.com/ph.jpg",
        )
        val engine = ContentEngine(
            sources = listOf(
                object : Source {
                    override val id = "met"
                    override val displayName = "Met"
                    override suspend fun load(): List<Artwork> = listOf(painting)
                },
                object : Source {
                    override val id = "pexels"
                    override val displayName = "Pexels"
                    override suspend fun load(): List<Artwork> = listOf(photo)
                },
            ),
            entitlement = object : PremiumEntitlement {
                override val isPremium = true
            },
        )
        val selection = PackSelection(PackFamily.Painting)
        val (pool, artwork) = loadDreamAmbient(engine, selection)
        assertEquals(listOf(painting), pool)
        assertEquals(painting, artwork)
    }

    @Test
    fun loadDreamAmbient_nullSelection_usesDefaultPackSelection() = runBlocking {
        val museum = Artwork(
            id = "met-sculpture",
            title = "David",
            sourceId = "met",
            kind = ArtworkKind.Sculpture,
            remoteUrl = "https://example.com/s.jpg",
        )
        val genart = Artwork(
            id = "genart.particles",
            title = "Particles",
            sourceId = "genart",
            kind = ArtworkKind.Genart,
        )
        val engine = ContentEngine(
            sources = listOf(
                object : Source {
                    override val id = "met"
                    override val displayName = "Met"
                    override suspend fun load(): List<Artwork> = listOf(museum)
                },
                object : Source {
                    override val id = "genart"
                    override val displayName = "Genart"
                    override suspend fun load(): List<Artwork> = listOf(genart)
                },
            ),
            entitlement = object : PremiumEntitlement {
                override val isPremium = true
            },
        )
        val (pool, artwork) = loadDreamAmbient(engine, selection = null)
        assertEquals(listOf(museum), pool)
        assertEquals(museum, artwork)
    }
}

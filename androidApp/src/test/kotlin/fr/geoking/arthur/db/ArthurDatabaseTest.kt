package fr.geoking.arthur.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import fr.geoking.arthur.db.ArthurDatabase
import fr.geoking.arthur.db.ArtworkCacheEntity
import fr.geoking.arthur.db.CustomFractalEntity
import fr.geoking.arthur.db.InvalidArtworkEntity
import fr.geoking.arthur.db.QuoteEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ArthurDatabaseTest {

    private lateinit var db: ArthurDatabase

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, ArthurDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun artworkCacheDaoOperations() = runBlocking {
        val dao = db.artworkCacheDao()

        val entity1 = ArtworkCacheEntity(
            id = "art1",
            title = "Mona Lisa",
            attribution = "Da Vinci",
            sourceId = "louvre",
            kind = "Photo",
            remoteUrl = "https://example.com/art1.jpg",
            category = "paintings",
            lastAccess = 1000L,
        )
        val entity2 = ArtworkCacheEntity(
            id = "art2",
            title = "Starry Night",
            attribution = "Van Gogh",
            sourceId = "met",
            kind = "Photo",
            remoteUrl = "https://example.com/art2.jpg",
            category = "paintings",
            lastAccess = 2000L,
        )

        dao.insertOrUpdateAll(listOf(entity1, entity2))

        val retrieved = dao.getById("art1")
        assertNotNull(retrieved)
        assertEquals("Mona Lisa", retrieved?.title)

        val byCategory = dao.getByCategoryAndSource("paintings", "louvre")
        assertEquals(1, byCategory.size)
        assertEquals("art1", byCategory[0].id)

        val allFlow = dao.observeAll().first()
        assertEquals(2, allFlow.size)

        dao.deleteById("art1")
        assertNull(dao.getById("art1"))
    }

    @Test
    fun genartLruOperations() = runBlocking {
        val dao = db.artworkCacheDao()

        for (i in 1..5) {
            dao.insertOrUpdate(
                ArtworkCacheEntity(
                    id = "genart_$i",
                    title = "Genart $i",
                    attribution = "Arthur",
                    sourceId = "genart",
                    kind = "Genart",
                    remoteUrl = null,
                    category = "genart",
                    lastAccess = i * 1000L,
                ),
            )
        }

        val genartStills = dao.getGenartStills(3)
        assertEquals(3, genartStills.size)
        assertEquals("genart_5", genartStills[0].id)

        val lruList = dao.getAllGenartLru()
        assertEquals(5, lruList.size)
        assertEquals("genart_1", lruList[0].id)
    }

    @Test
    fun invalidArtworkDaoOperations() = runBlocking {
        val dao = db.invalidArtworkDao()

        assertFalse(dao.isInvalid("bad1"))

        dao.markInvalid(InvalidArtworkEntity("bad1", 100L))
        dao.markInvalid(InvalidArtworkEntity("bad2", 200L))

        assertTrue(dao.isInvalid("bad1"))
        assertTrue(dao.isInvalid("bad2"))

        val idsFlow = dao.observeInvalidIds().first()
        assertEquals(listOf("bad1", "bad2"), idsFlow)

        dao.removeInvalid("bad1")
        assertFalse(dao.isInvalid("bad1"))

        // Test trimOldest
        dao.markInvalid(InvalidArtworkEntity("item1", 1000L))
        dao.markInvalid(InvalidArtworkEntity("item2", 2000L))
        dao.markInvalid(InvalidArtworkEntity("item3", 3000L))

        dao.trimOldest(2)
        val remaining = dao.getAllIds()
        assertEquals(2, remaining.size)
        assertEquals(listOf("item2", "item3"), remaining)
    }

    @Test
    fun customFractalDaoOperations() = runBlocking {
        val dao = db.customFractalDao()

        val f1 = CustomFractalEntity("fractal_1", "Julia Alpha", 1000L)
        val f2 = CustomFractalEntity("fractal_2", "Burning Ship", 2000L)

        dao.insertOrUpdate(f1)
        dao.insertOrUpdate(f2)

        val all = dao.getAll()
        assertEquals(2, all.size)
        assertEquals("fractal_2", all[0].id) // Ordered by createdAt DESC

        val flowAll = dao.observeAll().first()
        assertEquals(2, flowAll.size)

        dao.deleteById("fractal_1")
        assertEquals(1, dao.getAll().size)

        dao.deleteAll()
        assertTrue(dao.getAll().isEmpty())
    }

    @Test
    fun quoteDaoOperations() = runBlocking {
        val dao = db.quoteDao()

        val q1 = QuoteEntity("zen_1", "Life is beauty.", "Unknown", "ZenQuotes", 1000L)
        val q2 = QuoteEntity("zen_2", "Art is freedom.", "Artist", "ZenQuotes", 1000L)

        dao.insertAll(listOf(q1, q2))

        val retrieved = dao.getQuotesForProvider("ZenQuotes")
        assertEquals(2, retrieved.size)
        assertEquals("Life is beauty.", retrieved[0].text)

        dao.deleteForProvider("ZenQuotes")
        assertTrue(dao.getQuotesForProvider("ZenQuotes").isEmpty())
    }
}

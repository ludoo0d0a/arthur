package fr.geoking.arthur.shared.marketplace

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import fr.geoking.arthur.shared.source.GenartSource

class MarketplaceCatalogTest {
    @Test
    fun catalog_includesPersonalAndMonetizedGenartTopics() {
        val ids = MarketplaceCatalog.all().map { it.id }.toSet()
        assertTrue(MarketplaceCatalog.PERSONAL_PHOTOS_ID in ids)
        assertTrue(MarketplaceCatalog.genartPackId(GenartPackTopics.TAPET) in ids)
        assertTrue(MarketplaceCatalog.genartPackId(GenartPackTopics.CUSTOM) in ids)
        assertEquals(
            1 + GenartPackTopics.monetizedTopicSuffixes.size,
            MarketplaceCatalog.all().size,
        )
    }

    @Test
    fun freeEngines_doNotRequireOwnership() {
        val ownership = FakePackOwnership()
        assertTrue(ownership.allowsGenartEngine(GenartSource.PARTICLES))
        assertFalse(ownership.allowsGenartEngine(GenartSource.BLOBS))
    }

    @Test
    fun owningTapet_unlocksTapetEngines() {
        val ownership = FakePackOwnership(
            owned = setOf(MarketplaceCatalog.genartPackId(GenartPackTopics.TAPET)),
        )
        assertTrue(ownership.allowsGenartEngine(GenartSource.BLOBS))
        assertTrue(ownership.ownsGenartTopic(GenartPackTopics.TAPET))
        assertFalse(ownership.ownsPersonalPhotos())
    }
}

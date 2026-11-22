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
        assertTrue(MarketplaceCatalog.DIAMOND_ID in ids)
        assertTrue(MarketplaceCatalog.PERSONAL_PHOTOS_ID in ids)
        assertTrue(MarketplaceCatalog.genartPackId(GenartPackTopics.ABSTRACT) in ids)
        assertTrue(MarketplaceCatalog.genartPackId(GenartPackTopics.CUSTOM) in ids)
        assertTrue(MarketplaceCatalog.genartPackId(GenartPackTopics.VINTAGE) in ids)
        assertTrue(MarketplaceCatalog.genartPackId(GenartPackTopics.ANIMALS) in ids)
        assertTrue(MarketplaceCatalog.genartPackId(GenartPackTopics.STORM_STROBE) in ids)
        assertTrue(MarketplaceCatalog.genartPackId(GenartPackTopics.PHYSARUM) in ids)
        assertTrue(MarketplaceCatalog.genartPackId(GenartPackTopics.REACTIONS) in ids)
        assertTrue(MarketplaceCatalog.audioPackId(AudioPackCatalog.HEARTH_WEATHER) in ids)
        assertTrue(MarketplaceCatalog.audioPackId(AudioPackCatalog.SOLO_VIOLIN) in ids)
        assertTrue(MarketplaceCatalog.audioPackId(AudioPackCatalog.ARCADE_CHIPS) in ids)
        assertEquals(
            1 + 1 + GenartPackTopics.monetizedTopicSuffixes.size +
                AudioPackCatalog.monetizedPackSuffixes.size,
            MarketplaceCatalog.all().size,
        )
        assertEquals(
            MarketplaceCatalog.all().size - 1,
            MarketplaceCatalog.contentPacks().size,
        )
    }

    @Test
    fun diamond_unlocksAllContentPacks() {
        val ownership = FakePackOwnership(owned = setOf(MarketplaceCatalog.DIAMOND_ID))
        assertTrue(ownership.ownsDiamond())
        assertTrue(ownership.ownsPersonalPhotos())
        assertTrue(ownership.ownsCustomFractal())
        assertTrue(ownership.ownsAudioPack(AudioPackCatalog.HEARTH_WEATHER))
        assertTrue(ownership.allowsMusicStyle("ocean_waves"))
        assertTrue(ownership.allowsGenartEngine(GenartSource.BLOBS))
        MarketplaceCatalog.contentPacks().forEach { pack ->
            assertTrue(ownership.owns(pack.id), "diamond should unlock ${pack.id}")
        }
    }

    @Test
    fun freeMusicStyles_doNotRequireOwnership() {
        val ownership = FakePackOwnership()
        assertTrue(ownership.allowsMusicStyle("jazz_piano"))
        assertTrue(ownership.allowsMusicStyle("zen"))
        assertTrue(ownership.allowsMusicStyle("soft_guitar"))
        assertFalse(ownership.allowsMusicStyle("ocean_waves"))
        assertFalse(ownership.allowsMusicStyle("violin_lead"))
    }

    @Test
    fun owningAudioPack_unlocksCoveredStyles() {
        val ownership = FakePackOwnership(
            owned = setOf(MarketplaceCatalog.audioPackId(AudioPackCatalog.TEMPLE_RESONANCE)),
        )
        assertTrue(ownership.allowsMusicStyle("tibetan_bowl"))
        assertFalse(ownership.allowsMusicStyle("songbirds"))
    }

    @Test
    fun freeEngines_doNotRequireOwnership() {
        val ownership = FakePackOwnership()
        assertTrue(ownership.allowsGenartEngine(GenartSource.PARTICLES))
        assertFalse(ownership.allowsGenartEngine(GenartSource.BLOBS))
    }

    @Test
    fun owningAbstract_unlocksAbstractEngines() {
        val ownership = FakePackOwnership(
            owned = setOf(MarketplaceCatalog.genartPackId(GenartPackTopics.ABSTRACT)),
        )
        assertTrue(ownership.allowsGenartEngine(GenartSource.BLOBS))
        assertTrue(ownership.ownsGenartTopic(GenartPackTopics.ABSTRACT))
        assertFalse(ownership.ownsPersonalPhotos())
    }

    @Test
    fun legacyWallpaperPack_unlocksAbstractEngines() {
        val ownership = FakePackOwnership(
            owned = setOf(MarketplaceCatalog.LEGACY_GENART_WALLPAPER_PACK_ID),
        )
        assertTrue(ownership.allowsGenartEngine(GenartSource.BLOBS))
        assertTrue(ownership.ownsGenartTopic(GenartPackTopics.ABSTRACT))
        assertFalse(ownership.ownsPersonalPhotos())
    }
}

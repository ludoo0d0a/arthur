package fr.geoking.arthur.ui.screens

import fr.geoking.arthur.shared.domain.ArtUniverse
import fr.geoking.arthur.shared.marketplace.MarketplaceCatalog
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MarketplaceUniverseTest {

    @Test
    fun marketplacePackTiles_gallery_excludesGenartIncludesDiamond() {
        val ids = marketplacePackTiles(ArtUniverse.Gallery).mapNotNull { it.sellablePackId }
        assertTrue(ids.contains(MarketplaceCatalog.DIAMOND_ID))
        assertFalse(ids.any { it.startsWith("genart_") })
    }

    @Test
    fun marketplacePackTiles_generative_includesGenartExcludesPersonal() {
        val ids = marketplacePackTiles(ArtUniverse.Generative).mapNotNull { it.sellablePackId }
        assertTrue(ids.contains(MarketplaceCatalog.DIAMOND_ID))
        assertTrue(ids.any { it.startsWith("genart_") })
        assertFalse(ids.contains(MarketplaceCatalog.PERSONAL_PHOTOS_ID))
        assertFalse(ids.any { it.startsWith("audio_") })
    }
}

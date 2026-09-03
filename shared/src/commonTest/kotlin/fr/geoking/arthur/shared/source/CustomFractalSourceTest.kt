package fr.geoking.arthur.shared.source

import fr.geoking.arthur.shared.domain.ArtworkKind
import fr.geoking.arthur.shared.domain.FakePremiumEntitlement
import fr.geoking.arthur.shared.domain.PreparedRotation
import fr.geoking.arthur.shared.engine.ContentEngine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking

class CustomFractalSourceTest {
    @Test
    fun premiumCatalog_includesCustomFractals() = runBlocking {
        val custom = CustomFractalSource {
            listOf(
                CustomFractalSource.artwork(
                    id = "customfractal.v1.c1.m0.p100_100_500_500_900_100",
                    title = "Custom Bezier 3p",
                ),
            )
        }
        val engine = ContentEngine(
            sources = listOf(custom),
            entitlement = FakePremiumEntitlement(isPremium = true),
        )
        val catalog = engine.catalog(
            PreparedRotation(sourceIds = listOf(CustomFractalSource.ID), artworkIds = emptyList()),
        )
        assertEquals(1, catalog.size)
        assertEquals(ArtworkKind.CustomFractal, catalog.first().kind)
        assertTrue(CustomFractalSource.isCustomId(catalog.first().id))
    }

    @Test
    fun freeTier_blocksCustomFractals() = runBlocking {
        val custom = CustomFractalSource {
            listOf(
                CustomFractalSource.artwork(
                    id = "customfractal.v1.c1.m0.p100_100_500_500_900_100",
                    title = "Custom Bezier 3p",
                ),
            )
        }
        val engine = ContentEngine(
            sources = listOf(custom),
            entitlement = FakePremiumEntitlement(isPremium = false),
        )
        val catalog = engine.catalog(
            PreparedRotation(sourceIds = listOf(CustomFractalSource.ID), artworkIds = emptyList()),
        )
        assertTrue(catalog.isEmpty())
    }
}

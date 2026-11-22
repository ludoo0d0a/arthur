package fr.geoking.arthur.shared.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ArtUniverseTest {
    @Test
    fun fromStorageKey_roundTrips() {
        assertEquals(ArtUniverse.Gallery, ArtUniverse.fromStorageKey("Gallery"))
        assertEquals(ArtUniverse.Generative, ArtUniverse.fromStorageKey("Generative"))
        assertNull(ArtUniverse.fromStorageKey(null))
        assertNull(ArtUniverse.fromStorageKey("unknown"))
    }

    @Test
    fun storageKey_matchesName() {
        assertEquals("Gallery", ArtUniverse.Gallery.storageKey)
        assertEquals("Generative", ArtUniverse.Generative.storageKey)
    }
}

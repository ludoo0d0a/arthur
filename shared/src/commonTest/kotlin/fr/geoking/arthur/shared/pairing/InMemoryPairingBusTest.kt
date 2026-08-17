package fr.geoking.arthur.shared.pairing

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import fr.geoking.arthur.shared.domain.Artwork
import fr.geoking.arthur.shared.domain.ArtworkKind

class InMemoryPairingBusTest {
    @Test
    fun pushPull_roundTrip() {
        val bus = InMemoryPairingBus()
        assertNull(bus.pullManifest())
        val json = PairingCodec.encodeManifest(
            listOf(
                Artwork(
                    id = "a1",
                    title = "A",
                    sourceId = "bundled",
                    kind = ArtworkKind.Painting,
                    remoteUrl = "https://example.com/a.jpg",
                ),
            ),
        )
        bus.pushManifest(json)
        assertEquals(json, bus.pullManifest())
        assertEquals("a1", PairingCodec.decodeManifest(bus.pullManifest()!!).rotationIds.single())
    }
}

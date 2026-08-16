package fr.geoking.arthur.shared.pairing

import fr.geoking.arthur.shared.domain.Artwork
import fr.geoking.arthur.shared.domain.ArtworkKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PairingCodecTest {

    @Test
    fun roundTrip_manifest() {
        val art = listOf(
            Artwork(
                id = "r1",
                title = "Night Watch",
                attribution = "Rijksmuseum",
                sourceId = "rijks",
                kind = ArtworkKind.Painting,
                remoteUrl = "https://example.com/r1.jpg",
            ),
        )
        val encoded = PairingCodec.encodeManifest(art)
        val decoded = PairingCodec.decodeManifest(encoded)
        assertEquals(listOf("r1"), decoded.rotationIds)
        assertEquals("Night Watch", decoded.entries.single().title)
        assertEquals("https://example.com/r1.jpg", decoded.entries.single().remoteUrl)
    }

    @Test
    fun needsBlob_personalAndLocalOnly() {
        val personal = Artwork("p", "Mine", sourceId = "perso", kind = ArtworkKind.PersonalPhoto, localPath = "/tmp/a.jpg")
        val remote = Artwork("r", "Remote", sourceId = "rijks", kind = ArtworkKind.Painting, remoteUrl = "https://x")
        assertTrue(PairingCodec.needsBlob(personal))
        assertFalse(PairingCodec.needsBlob(remote))
    }

    @Test
    fun roundTrip_blob() {
        val blob = PairingBlob(artworkId = "p", mimeType = "image/jpeg", base64Payload = "abc")
        val decoded = PairingCodec.decodeBlob(PairingCodec.encodeBlob(blob))
        assertEquals(blob, decoded)
    }
}

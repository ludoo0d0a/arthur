package fr.geoking.arthur.shared.pairing

import fr.geoking.arthur.shared.domain.Artwork
import fr.geoking.arthur.shared.domain.ArtworkKind
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** Manifest for Remote Sources — TV fetches URLs itself. */
@Serializable
data class PairingManifest(
    val rotationIds: List<String>,
    val entries: List<PairingManifestEntry>,
)

@Serializable
data class PairingManifestEntry(
    val id: String,
    val title: String,
    val attribution: String = "",
    val kind: String,
    val remoteUrl: String? = null,
)

/** Personal / bundled blobs pushed over LAN. */
@Serializable
data class PairingBlob(
    val artworkId: String,
    val mimeType: String,
    val base64Payload: String,
)

object PairingCodec {
    private val json = Json { ignoreUnknownKeys = true }

    fun encodeManifest(artworks: List<Artwork>): String {
        val manifest = PairingManifest(
            rotationIds = artworks.map { it.id },
            entries = artworks.map {
                PairingManifestEntry(
                    id = it.id,
                    title = it.title,
                    attribution = it.attribution,
                    kind = it.kind.name,
                    remoteUrl = it.remoteUrl,
                )
            },
        )
        return json.encodeToString(manifest)
    }

    fun decodeManifest(raw: String): PairingManifest = json.decodeFromString(raw)

    fun encodeBlob(blob: PairingBlob): String = json.encodeToString(blob)

    fun decodeBlob(raw: String): PairingBlob = json.decodeFromString(raw)

    fun needsBlob(artwork: Artwork): Boolean =
        artwork.kind == ArtworkKind.PersonalPhoto ||
            (artwork.remoteUrl == null && artwork.localPath != null)
}

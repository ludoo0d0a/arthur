package fr.geoking.arthur.pairing

import fr.geoking.arthur.shared.domain.Artwork
import fr.geoking.arthur.shared.domain.ArtworkKind
import fr.geoking.arthur.shared.pairing.PairingManifest
import fr.geoking.arthur.shared.pairing.PairingManifestEntry

/** Rebuild Artwork rows from a LAN pairing manifest (remote URLs / ids). */
object PairingManifestMapper {
    fun toArtworks(manifest: PairingManifest): List<Artwork> {
        val byId = manifest.entries.associateBy { it.id }
        return manifest.rotationIds.mapNotNull { id ->
            val entry = byId[id] ?: return@mapNotNull null
            entry.toArtwork()
        }
    }

    private fun PairingManifestEntry.toArtwork(): Artwork =
        Artwork(
            id = id,
            title = title,
            attribution = attribution,
            sourceId = "pairing",
            kind = kindFrom(kind),
            remoteUrl = remoteUrl,
        )

    private fun kindFrom(raw: String): ArtworkKind =
        runCatching { ArtworkKind.valueOf(raw) }.getOrDefault(ArtworkKind.Photo)
}

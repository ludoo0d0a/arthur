package fr.geoking.arthur.pairing

import fr.geoking.arthur.auto.AmbientAlbumArt
import fr.geoking.arthur.shared.domain.Artwork
import fr.geoking.arthur.shared.domain.PreparedRotation
import fr.geoking.arthur.shared.engine.ContentEngine
import fr.geoking.arthur.shared.pairing.PairingCodec

sealed interface PairingPushResult {
    data class Ok(val count: Int) : PairingPushResult
    data object Unreachable : PairingPushResult
    data object EmptyCatalog : PairingPushResult
    data object PushFailed : PairingPushResult
}

suspend fun pushPreparedRotation(
    host: String,
    port: Int,
    contentEngine: ContentEngine,
): PairingPushResult {
    val client = LanPairingClient(host = host, port = port)
    return try {
        if (!client.health()) return PairingPushResult.Unreachable
        val catalog = contentEngine.catalog(
            PreparedRotation(sourceIds = emptyList(), artworkIds = emptyList()),
        )
        val pool = AmbientAlbumArt.sampleRotationPool(catalog)
        if (pool.isEmpty()) return PairingPushResult.EmptyCatalog
        val json = PairingCodec.encodeManifest(pool)
        if (!client.pushManifest(json)) return PairingPushResult.PushFailed
        PairingPushResult.Ok(pool.size)
    } finally {
        client.close()
    }
}

/** Visible for unit tests — sample pool encoding path. */
fun artworksForPairingPush(catalog: List<Artwork>): List<Artwork> =
    AmbientAlbumArt.sampleRotationPool(catalog)

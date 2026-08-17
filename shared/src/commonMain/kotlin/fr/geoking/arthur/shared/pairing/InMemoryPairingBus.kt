package fr.geoking.arthur.shared.pairing

/**
 * In-memory pairing bus for unit tests and same-process TV/phone debug.
 * Production LAN uses [fr.geoking.arthur.pairing.LanPairingServer] on Android.
 */
class InMemoryPairingBus {
    @Volatile
    private var manifestJson: String? = null

    fun pushManifest(json: String) {
        PairingCodec.decodeManifest(json)
        manifestJson = json
    }

    fun pullManifest(): String? = manifestJson

    fun clear() {
        manifestJson = null
    }
}

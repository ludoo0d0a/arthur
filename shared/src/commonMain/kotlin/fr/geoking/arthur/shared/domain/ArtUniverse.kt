package fr.geoking.arthur.shared.domain

/**
 * Product universe: filters Control Plane home / Marketplace without splitting the APK.
 * Preference only — users can switch freely via the header chips.
 */
enum class ArtUniverse {
    /** Museum, painting, sculpture, photo, video, personal. */
    Gallery,

    /** Genart, fractals, vintage games. */
    Generative,
    ;

    val storageKey: String get() = name

    companion object {
        fun fromStorageKey(key: String?): ArtUniverse? =
            entries.firstOrNull { it.name == key }
    }
}

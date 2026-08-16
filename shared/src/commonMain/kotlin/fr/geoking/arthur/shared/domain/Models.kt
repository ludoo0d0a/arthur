package fr.geoking.arthur.shared.domain

/** One displayable piece in the Content Engine. */
data class Artwork(
    val id: String,
    val title: String,
    val attribution: String = "",
    val sourceId: String,
    val kind: ArtworkKind,
    val remoteUrl: String? = null,
    val localPath: String? = null,
)

enum class ArtworkKind {
    Photo,
    Painting,
    Sculpture,
    FractalPreset,
    CustomFractal,
    Genart,
    PersonalPhoto,
}

/** A feed that supplies Artwork. */
interface Source {
    val id: String
    val displayName: String
    suspend fun load(): List<Artwork>
}

/** Paid unlock gate — adapters (RevenueCat) live on Android. */
interface PremiumEntitlement {
    val isPremium: Boolean
}

data class FreeTierLimits(
    val maxPhotoArtwork: Int = 5,
    val maxFractalPresets: Int = 3,
    val maxGenart: Int = 2,
)

/** Scheduled sequence shown on a Canvas. */
data class AmbientRotation(
    val artworkIds: List<String>,
)

/** Control Plane selection of sources and order. */
data class PreparedRotation(
    val sourceIds: List<String>,
    val artworkIds: List<String>,
)

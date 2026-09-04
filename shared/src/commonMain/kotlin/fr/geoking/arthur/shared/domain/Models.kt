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

/** Live procedural kinds (genart / fractal) vs still image Artwork. */
val ArtworkKind.isGenerative: Boolean
    get() = when (this) {
        ArtworkKind.Genart,
        ArtworkKind.FractalPreset,
        ArtworkKind.CustomFractal,
        -> true
        ArtworkKind.Photo,
        ArtworkKind.Painting,
        ArtworkKind.Sculpture,
        ArtworkKind.PersonalPhoto,
        -> false
    }

val Artwork.isGenerative: Boolean
    get() = kind.isGenerative

/**
 * Return the explicitly requested piece if found in [catalog];
 * otherwise default to the first generative artwork, or first catalog item.
 */
fun resolveAmbientArtwork(catalog: List<Artwork>, artworkId: String?): Artwork? {
    val requested = catalog.firstOrNull { it.id == artworkId }
    if (requested != null) return requested
    return catalog.firstOrNull { it.isGenerative } ?: catalog.firstOrNull()
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

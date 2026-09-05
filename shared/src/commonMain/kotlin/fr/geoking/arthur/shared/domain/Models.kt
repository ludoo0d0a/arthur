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
 * Honor the requested artwork (still or generative) when it exists in [catalog].
 * When [artworkId] is null (no user selection), fall back to the first generative
 * piece, or the first artwork available. A non-null id that is missing from the
 * catalog returns null so callers can keep a stashed selection instead of swapping
 * to an unrelated animation.
 */
fun resolveAmbientArtwork(catalog: List<Artwork>, artworkId: String?): Artwork? {
    if (artworkId != null) {
        return catalog.firstOrNull { it.id == artworkId }
    }
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
    /** Enough for Genart Nature / Geometry / Planets subcategories to show results. */
    val maxGenart: Int = 12,
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

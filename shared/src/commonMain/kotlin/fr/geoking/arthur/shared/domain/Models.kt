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
    /** Longer museum/stock blurb beyond title + author; empty when unavailable. */
    val description: String = "",
    /** Creation / photograph / object date when the Source provides it. */
    val date: String = "",
    /** Medium / materials when provided (e.g. "Oil on canvas"). */
    val medium: String = "",
    /** License or rights notice (e.g. "Public Domain", "CC0"). */
    val license: String = "",
    /** Museum object page or stock photo page when available. */
    val externalUrl: String? = null,
)

/** True when Ambient should offer the detail page (⋯). */
fun Artwork.hasDetailContent(): Boolean =
    description.isNotBlank() ||
        date.isNotBlank() ||
        medium.isNotBlank() ||
        license.isNotBlank() ||
        !externalUrl.isNullOrBlank()


enum class ArtworkKind {
    Photo,
    Video,
    Painting,
    Sculpture,
    FractalPreset,
    CustomFractal,
    Genart,
    PersonalPhoto,
}

/** Live procedural kinds (genart / fractal) vs still / video Artwork. */
val ArtworkKind.isGenerative: Boolean
    get() = when (this) {
        ArtworkKind.Genart,
        ArtworkKind.FractalPreset,
        ArtworkKind.CustomFractal,
        -> true
        ArtworkKind.Photo,
        ArtworkKind.Video,
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
    suspend fun load(limit: Int): List<Artwork> = load()
}

/** Paid unlock gate for global UX (no ads, favorites) — adapters (RevenueCat) live on Android.
 * Content packs use [fr.geoking.arthur.shared.marketplace.PackOwnership], not Premium.
 */
interface PremiumEntitlement {
    val isPremium: Boolean
}

data class FreeTierLimits(
    /**
     * Cap on free-tier stills (photo / video / painting / sculpture). High enough
     * that Painting/Museum **All** can keep at least one piece from each remote
     * museum after fair round-robin — a tiny prefix cap emptied later Sources.
     */
    val maxPhotoArtwork: Int = 24,
    val maxFractalPresets: Int = 3,
    /**
     * Cap on free-tier Genart engines. Must cover the full shipped catalog so
     * late topics (Abstract Tapet engines, Weather, Planets) are not empty —
     * Genart is on-device procedural, so a tight prefix cap only hides packs.
     */
    val maxGenart: Int = 120,
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

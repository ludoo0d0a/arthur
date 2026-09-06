package fr.geoking.arthur.shared.source

import fr.geoking.arthur.shared.domain.ArtworkKind

/**
 * Declares what content a Source can supply, and which kinds support live remote search.
 * Curated Sources (e.g. Bundled) list [kinds] but leave [remoteSearchKinds] empty.
 */
data class SourceContentSupport(
    val kinds: Set<ArtworkKind>,
    val remoteSearchKinds: Set<ArtworkKind> = emptySet(),
)

/**
 * Registry of content / search availability for every Arthur Source id.
 * Pack ambient load and filters use this instead of ad-hoc source-id lists.
 */
object SourceCapabilities {

    private val museumPaintingSculpture = SourceContentSupport(
        kinds = setOf(ArtworkKind.Painting, ArtworkKind.Sculpture),
        remoteSearchKinds = setOf(ArtworkKind.Painting, ArtworkKind.Sculpture),
    )

    private val museumWithPhoto = SourceContentSupport(
        kinds = setOf(ArtworkKind.Painting, ArtworkKind.Sculpture, ArtworkKind.Photo),
        remoteSearchKinds = setOf(
            ArtworkKind.Painting,
            ArtworkKind.Sculpture,
            ArtworkKind.Photo,
        ),
    )

    private val byId: Map<String, SourceContentSupport> = mapOf(
        PexelsSource.ID to SourceContentSupport(
            kinds = setOf(ArtworkKind.Photo),
            remoteSearchKinds = setOf(ArtworkKind.Photo),
        ),
        UnsplashSource.ID to SourceContentSupport(
            kinds = setOf(ArtworkKind.Photo),
            remoteSearchKinds = setOf(ArtworkKind.Photo),
        ),
        PexelsVideoSource.ID to SourceContentSupport(
            kinds = setOf(ArtworkKind.Video),
            remoteSearchKinds = setOf(ArtworkKind.Video),
        ),
        PixabayVideoSource.ID to SourceContentSupport(
            kinds = setOf(ArtworkKind.Video),
            remoteSearchKinds = setOf(ArtworkKind.Video),
        ),
        CoverrSource.ID to SourceContentSupport(
            kinds = setOf(ArtworkKind.Video),
            remoteSearchKinds = setOf(ArtworkKind.Video),
        ),
        BundledPackSource.ID to SourceContentSupport(
            kinds = setOf(
                ArtworkKind.Photo,
                ArtworkKind.Painting,
                ArtworkKind.Sculpture,
            ),
        ),
        RijksmuseumSource.ID to museumWithPhoto,
        ClevelandSource.ID to museumWithPhoto,
        MetSource.ID to museumPaintingSculpture,
        ArticSource.ID to museumPaintingSculpture,
        EuropeanaSource.ID to museumPaintingSculpture,
        HarvardSource.ID to museumPaintingSculpture,
        SmithsonianSource.ID to museumPaintingSculpture,
        LouvreSource.ID to museumPaintingSculpture,
        WikimediaStreetArtSource.ID to SourceContentSupport(
            kinds = setOf(ArtworkKind.Painting),
        ),
        GenartSource.ID to SourceContentSupport(
            kinds = setOf(ArtworkKind.Genart),
        ),
        FractalSource.ID to SourceContentSupport(
            kinds = setOf(ArtworkKind.FractalPreset),
        ),
        CustomFractalSource.ID to SourceContentSupport(
            kinds = setOf(ArtworkKind.CustomFractal),
        ),
    )

    fun support(sourceId: String): SourceContentSupport? = byId[sourceId]

    /** Source ids that can supply [kind] (curated or remote). */
    fun sourceIdsSupporting(kind: ArtworkKind): List<String> =
        byId.filter { (_, support) -> kind in support.kinds }.keys.toList()

    /** Source ids that can remotely search for [kind]. */
    fun sourceIdsWithRemoteSearch(kind: ArtworkKind): List<String> =
        byId.filter { (_, support) -> kind in support.remoteSearchKinds }.keys.toList()
}

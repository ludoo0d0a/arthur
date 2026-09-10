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

    /** Museums currently expose Painting + Sculpture + Photo remote search. */
    private val museumStillKinds = SourceContentSupport(
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
            kinds = setOf(ArtworkKind.Photo, ArtworkKind.Video),
            remoteSearchKinds = setOf(ArtworkKind.Photo, ArtworkKind.Video),
        ),
        DeviantArtSource.ID to SourceContentSupport(
            kinds = setOf(ArtworkKind.Photo, ArtworkKind.Painting),
            remoteSearchKinds = setOf(ArtworkKind.Photo, ArtworkKind.Painting),
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
        RijksmuseumSource.ID to museumStillKinds,
        ClevelandSource.ID to museumStillKinds,
        MetSource.ID to museumStillKinds,
        ArticSource.ID to museumStillKinds,
        EuropeanaSource.ID to museumStillKinds,
        HarvardSource.ID to museumStillKinds,
        SmithsonianSource.ID to museumStillKinds,
        LouvreSource.ID to museumStillKinds,
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

    /**
     * Ambient Start for Painting / Sculpture / Photo **Random**: every Source that can
     * remotely search that kind (museums + stock).
     */
    fun sourceIdsForKindAmbient(kind: ArtworkKind): List<String> =
        sourceIdsWithRemoteSearch(kind)
}

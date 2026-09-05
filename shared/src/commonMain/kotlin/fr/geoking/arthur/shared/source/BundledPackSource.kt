package fr.geoking.arthur.shared.source

import fr.geoking.arthur.shared.domain.Artwork
import fr.geoking.arthur.shared.domain.ArtworkKind
import fr.geoking.arthur.shared.domain.Source

/** Small offline set shipped with the app (Suggestions for photo / painting / sculpture). */
class BundledPackSource(
    private val items: List<Artwork> = defaultPack(),
) : Source {
    override val id: String = ID
    override val displayName: String = "Bundled pack"

    override suspend fun load(): List<Artwork> = items

    companion object {
        const val ID = "bundled"

        fun defaultPack(): List<Artwork> =
            suggestionPaintings() + suggestionSculptures() + suggestionPhotos()

        /** Curated paintings under Painting → Suggestions. */
        fun suggestionPaintings(): List<Artwork> = listOf(
            Artwork(
                id = "bundled-1",
                title = "Study in Blue",
                attribution = "Arthur Bundled Pack",
                sourceId = ID,
                kind = ArtworkKind.Painting,
                remoteUrl = "https://iiif.micr.io/mPymb/full/max/0/default.jpg",
            ),
            Artwork(
                id = "bundled-p2",
                title = "Canvas Bloom",
                attribution = "Unsplash",
                sourceId = ID,
                kind = ArtworkKind.Painting,
                remoteUrl = "https://images.unsplash.com/photo-1579783902614-a3fb3927b6a5?w=1200",
            ),
            Artwork(
                id = "bundled-p3",
                title = "Oil Horizon",
                attribution = "Unsplash",
                sourceId = ID,
                kind = ArtworkKind.Painting,
                remoteUrl = "https://images.unsplash.com/photo-1578301978693-85fa9c0320b9?w=1200",
            ),
            Artwork(
                id = "bundled-p4",
                title = "Pigment Field",
                attribution = "Unsplash",
                sourceId = ID,
                kind = ArtworkKind.Painting,
                remoteUrl = "https://images.unsplash.com/photo-1549490349-8643362247b5?w=1200",
            ),
            Artwork(
                id = "bundled-p5",
                title = "Brush Silence",
                attribution = "Unsplash",
                sourceId = ID,
                kind = ArtworkKind.Painting,
                remoteUrl = "https://images.unsplash.com/photo-1515405295579-ba7b45403062?w=1200",
            ),
            Artwork(
                id = "bundled-p6",
                title = "Warm Impasto",
                attribution = "Unsplash",
                sourceId = ID,
                kind = ArtworkKind.Painting,
                remoteUrl = "https://images.unsplash.com/photo-1554188248-986adbb73be4?w=1200",
            ),
        )

        /** Curated sculptures under Sculpture → Suggestions. */
        fun suggestionSculptures(): List<Artwork> = listOf(
            Artwork(
                id = "bundled-2",
                title = "Marble Light",
                attribution = "Arthur Bundled Pack",
                sourceId = ID,
                kind = ArtworkKind.Sculpture,
                remoteUrl = "https://iiif.micr.io/tqMQL/full/max/0/default.jpg",
            ),
            Artwork(
                id = "bundled-s2",
                title = "Stone Gesture",
                attribution = "Unsplash",
                sourceId = ID,
                kind = ArtworkKind.Sculpture,
                remoteUrl = "https://images.unsplash.com/photo-1605721911519-3dfeb3be25e7?w=1200",
            ),
            Artwork(
                id = "bundled-s3",
                title = "Bronze Quiet",
                attribution = "Unsplash",
                sourceId = ID,
                kind = ArtworkKind.Sculpture,
                remoteUrl = "https://images.unsplash.com/photo-1550684848-fac1c5b4e853?w=1200",
            ),
            Artwork(
                id = "bundled-s4",
                title = "Form in Light",
                attribution = "Unsplash",
                sourceId = ID,
                kind = ArtworkKind.Sculpture,
                remoteUrl = "https://images.unsplash.com/photo-1610701596007-11502861dcfa?w=1200",
            ),
            Artwork(
                id = "bundled-s5",
                title = "Abstract Volume",
                attribution = "Unsplash",
                sourceId = ID,
                kind = ArtworkKind.Sculpture,
                remoteUrl = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=1200",
            ),
            Artwork(
                id = "bundled-s6",
                title = "Gallery Cast",
                attribution = "Unsplash",
                sourceId = ID,
                kind = ArtworkKind.Sculpture,
                remoteUrl = "https://images.unsplash.com/photo-1578301978018-3005759f48f7?w=1200",
            ),
        )

        /** Curated photos under Photo → Suggestions. */
        fun suggestionPhotos(): List<Artwork> = listOf(
            Artwork(
                id = "bundled-3",
                title = "Harbor Grain",
                attribution = "Arthur Bundled Pack",
                sourceId = ID,
                kind = ArtworkKind.Photo,
                remoteUrl = "https://iiif.micr.io/NYkLJ/full/max/0/default.jpg",
            ),
            Artwork(
                id = "bundled-4",
                title = "Alpine Ridge",
                attribution = "Unsplash",
                sourceId = ID,
                kind = ArtworkKind.Photo,
                remoteUrl = "https://images.unsplash.com/photo-1506905925346-21bda4d32df4?w=1200",
            ),
            Artwork(
                id = "bundled-5",
                title = "Forest Canopy",
                attribution = "Unsplash",
                sourceId = ID,
                kind = ArtworkKind.Photo,
                remoteUrl = "https://images.unsplash.com/photo-1441974231531-c6227db76b6e?w=1200",
            ),
            Artwork(
                id = "bundled-6",
                title = "Misty Hills",
                attribution = "Unsplash",
                sourceId = ID,
                kind = ArtworkKind.Photo,
                remoteUrl = "https://images.unsplash.com/photo-1470071459604-3b5ec3a7fe05?w=1200",
            ),
            Artwork(
                id = "bundled-7",
                title = "Meadow Light",
                attribution = "Unsplash",
                sourceId = ID,
                kind = ArtworkKind.Photo,
                remoteUrl = "https://images.unsplash.com/photo-1426604966848-d7adac402bff?w=1200",
            ),
            Artwork(
                id = "bundled-8",
                title = "Quiet Shore",
                attribution = "Unsplash",
                sourceId = ID,
                kind = ArtworkKind.Photo,
                remoteUrl = "https://images.unsplash.com/photo-1472214103451-9374bd1c798e?w=1200",
            ),
        )
    }
}

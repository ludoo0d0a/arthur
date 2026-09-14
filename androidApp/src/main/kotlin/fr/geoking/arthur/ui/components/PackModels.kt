package fr.geoking.arthur.ui.components

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import fr.geoking.arthur.R
import fr.geoking.arthur.shared.domain.Artwork
import fr.geoking.arthur.shared.domain.ArtworkKind
import fr.geoking.arthur.shared.source.SourceCapabilities
import fr.geoking.arthur.shared.source.StockPhotoCategory

/** Top-level Spotify-style packs on the Control Plane home grid. */
enum class PackFamily(
    @get:StringRes val titleRes: Int,
    @get:DrawableRes val coverRes: Int,
    val testTagSuffix: String,
) {
    Museum(R.string.pack_museum, R.drawable.pack_museum, "museum"),
    Genart(R.string.kind_genart, R.drawable.pack_genart, "genart"),
    Photo(R.string.kind_photo, R.drawable.pack_photo, "photo"),
    Video(R.string.kind_video, R.drawable.pack_video, "video"),
    Sculpture(R.string.kind_sculpture, R.drawable.pack_sculpture, "sculpture"),
    Painting(R.string.kind_painting, R.drawable.pack_painting, "painting"),
}

/**
 * Selected pack for Ambient: a [PackFamily] plus optional subcategory.
 * [subId] null means the family's home tile was started directly, with no sub-pack
 * chosen — this behaves exactly like that family's **Random** sub-pack.
 */
data class PackSelection(
    val family: PackFamily,
    val subId: String? = null,
) {
    val isAll: Boolean get() = subId == null
}

data class PackTile(
    val id: String,
    @get:StringRes val titleRes: Int,
    @get:DrawableRes val coverRes: Int,
    val selection: PackSelection,
    val testTagSuffix: String,
    val itemCount: Int? = null,
)

/**
 * Museum institution sub-packs (every [MuseumTopic] with a Source, i.e. excluding
 * [MuseumTopic.Random]).
 */
private val MuseumInstitutionTopics: List<MuseumTopic> =
    MuseumTopic.entries.filter { it.sourceId != null }

/** Genart sub-pack order for the grid ([GenartTopic.Random] leads). */
private val GenartSubTopics = listOf(
    GenartTopic.Random,
    GenartTopic.Tapet,
    GenartTopic.Nature,
    GenartTopic.Weather,
    GenartTopic.Water,
    GenartTopic.Life,
    GenartTopic.Earth,
    GenartTopic.Planets,
    GenartTopic.SciFi,
    GenartTopic.Abstract,
    GenartTopic.Geometry,
    GenartTopic.Fractal,
    GenartTopic.Custom,
)

fun PackFamily.homeTile(catalog: List<Artwork> = emptyList()): PackTile = PackTile(
    id = "family_${testTagSuffix}",
    titleRes = titleRes,
    coverRes = coverRes,
    selection = PackSelection(this),
    testTagSuffix = testTagSuffix,
    itemCount = if (this == PackFamily.Genart && catalog.isNotEmpty()) {
        resolvePackPool(catalog, PackSelection(PackFamily.Genart)).size
    } else null,
)

fun PackFamily.subPackTiles(catalog: List<Artwork> = emptyList()): List<PackTile> = when (this) {
    PackFamily.Museum -> MuseumTopic.entries.map { topic ->
        PackTile(
            id = "sub_museum_${topic.testTagSuffix}",
            titleRes = topic.labelRes,
            coverRes = PackCovers.museum(topic),
            selection = PackSelection(PackFamily.Museum, topic.testTagSuffix),
            testTagSuffix = "museum_${topic.testTagSuffix}",
        )
    }
    PackFamily.Genart -> GenartSubTopics.map { topic ->
        val selection = PackSelection(PackFamily.Genart, topic.testTagSuffix)
        PackTile(
            id = "sub_genart_${topic.testTagSuffix}",
            titleRes = topic.labelRes,
            coverRes = PackCovers.genart(topic),
            selection = selection,
            testTagSuffix = "genart_${topic.testTagSuffix}",
            itemCount = if (catalog.isNotEmpty()) resolvePackPool(catalog, selection).size else null,
        )
    }
    PackFamily.Photo -> {
        val providerSources = PhotoTopic.entries.map { topic ->
            PackTile(
                id = "sub_photo_${topic.testTagSuffix}",
                titleRes = topic.labelRes,
                coverRes = PackCovers.photoTopic(topic),
                selection = PackSelection(PackFamily.Photo, topic.testTagSuffix),
                testTagSuffix = "photo_${topic.testTagSuffix}",
            )
        }
        val topics = StockPhotoCategory.entries.map { topic ->
            PackTile(
                id = "sub_photo_${topic.query}",
                titleRes = topic.packLabelRes(),
                coverRes = PackCovers.photo(topic),
                selection = PackSelection(PackFamily.Photo, topic.query),
                testTagSuffix = "photo_${topic.query}",
            )
        }
        // Museums support Photo content too — offer them as sources searchable with
        // category=Photo, same "one source, 20 items" rule as any other pick.
        val museums = MuseumInstitutionTopics.map { topic ->
            PackTile(
                id = "sub_photo_museum_${topic.testTagSuffix}",
                titleRes = topic.labelRes,
                coverRes = PackCovers.museum(topic),
                selection = PackSelection(PackFamily.Photo, topic.testTagSuffix),
                testTagSuffix = "photo_museum_${topic.testTagSuffix}",
            )
        }
        providerSources + topics + museums
    }
    PackFamily.Video -> {
        val sources = VideoTopic.entries.map { topic ->
            PackTile(
                id = "sub_video_${topic.testTagSuffix}",
                titleRes = topic.labelRes,
                coverRes = PackCovers.video(topic),
                selection = PackSelection(PackFamily.Video, topic.testTagSuffix),
                testTagSuffix = "video_${topic.testTagSuffix}",
            )
        }
        val keywords = videoStockTopics().map { topic ->
            PackTile(
                id = "sub_video_${topic.query}",
                titleRes = topic.packLabelRes(),
                coverRes = PackCovers.photo(topic),
                selection = PackSelection(PackFamily.Video, topic.query),
                testTagSuffix = "video_${topic.query}",
            )
        }
        sources + keywords
    }
    PackFamily.Sculpture -> MuseumTopic.entries.map { topic ->
        PackTile(
            id = "sub_sculpture_${topic.testTagSuffix}",
            titleRes = topic.labelRes,
            coverRes = PackCovers.museum(topic),
            selection = PackSelection(PackFamily.Sculpture, topic.testTagSuffix),
            testTagSuffix = "sculpture_${topic.testTagSuffix}",
        )
    }
    PackFamily.Painting -> MuseumTopic.entries.map { topic ->
        PackTile(
            id = "sub_painting_${topic.testTagSuffix}",
            titleRes = topic.labelRes,
            coverRes = PackCovers.museum(topic),
            selection = PackSelection(PackFamily.Painting, topic.testTagSuffix),
            testTagSuffix = "painting_${topic.testTagSuffix}",
        )
    }
}

fun resolvePackPool(catalog: List<Artwork>, selection: PackSelection): List<Artwork> {
    val isRandom = when (selection.family) {
        PackFamily.Museum -> (selection.museumTopicOrNull() ?: MuseumTopic.Random) == MuseumTopic.Random
        PackFamily.Genart -> (selection.genartTopicOrNull() ?: GenartTopic.Random) == GenartTopic.Random
        PackFamily.Photo -> (selection.stockCategoryOrNull() ?: StockPhotoCategory.Random) == StockPhotoCategory.Random
        PackFamily.Video -> (selection.stockCategoryOrNull() ?: StockPhotoCategory.Random) == StockPhotoCategory.Random
        PackFamily.Sculpture -> (selection.museumTopicOrNull() ?: MuseumTopic.Random) == MuseumTopic.Random
        PackFamily.Painting -> (selection.museumTopicOrNull() ?: MuseumTopic.Random) == MuseumTopic.Random
    }

    if (isRandom) {
        return catalog.shuffled()
    }

    return when (selection.family) {
        PackFamily.Museum -> {
            val topic = selection.museumTopicOrNull() ?: MuseumTopic.Random
            val museumKinds = catalog.filter { art ->
                art.kind == ArtworkKind.Painting || art.kind == ArtworkKind.Sculpture
            }
            museumKinds.filter { matchesMuseumTopic(it, topic) }
        }
        PackFamily.Genart -> {
            val topic = selection.genartTopicOrNull() ?: GenartTopic.Random
            catalog.filterByCategoryAndSources(
                category = CategoryFilter.GENART,
                genartTopic = topic,
            )
        }
        PackFamily.Photo -> {
            val photoSource = selection.photoSourceOrNull()
            if (photoSource != null) {
                catalog.filter { art ->
                    CategoryFilter.PHOTO.matches(art.kind) && matchesPhotoSource(art, photoSource)
                }
            } else {
                val museum = selection.museumTopicOrNull()
                if (museum != null) {
                    catalog.filter { art ->
                        CategoryFilter.PHOTO.matches(art.kind) && matchesMuseumTopic(art, museum)
                    }
                } else {
                    val topic = selection.stockCategoryOrNull() ?: StockPhotoCategory.Random
                    catalog.filterByCategoryAndSources(
                        category = CategoryFilter.PHOTO,
                        stockCategory = topic,
                    )
                }
            }
        }
        PackFamily.Video -> {
            val source = selection.videoSourceOrNull()
            if (source != null) {
                catalog.filter { art ->
                    CategoryFilter.VIDEO.matches(art.kind) && matchesVideoSource(art, source)
                }
            } else {
                val topic = selection.stockCategoryOrNull() ?: StockPhotoCategory.Random
                catalog.filterByCategoryAndSources(
                    category = CategoryFilter.VIDEO,
                    stockCategory = topic,
                )
            }
        }
        PackFamily.Sculpture -> {
            val topic = selection.museumTopicOrNull() ?: MuseumTopic.Random
            catalog.filterByCategoryAndSources(
                category = CategoryFilter.SCULPTURE,
                museumTopic = topic,
            )
        }
        PackFamily.Painting -> {
            val topic = selection.museumTopicOrNull() ?: MuseumTopic.Random
            catalog.filterByCategoryAndSources(
                category = CategoryFilter.PAINTING,
                museumTopic = topic,
            )
        }
    }
}

fun PackSelection.stockCategoryOrNull(): StockPhotoCategory? =
    if (subId == null) {
        null
    } else {
        when (family) {
            // A museum institution id or provider source id under Photo is a Source pick, not a stock topic.
            PackFamily.Photo ->
                if (museumTopicOrNull() != null || photoSourceOrNull() != null) {
                    null
                } else {
                    StockPhotoCategory.fromQuery(subId)
                }
            PackFamily.Video -> {
                // Source tiles use VideoTopic ids; keywords use StockPhotoCategory.query.
                if (videoSourceOrNull() != null) {
                    null
                } else {
                    StockPhotoCategory.entries.firstOrNull {
                        it.query.equals(subId, ignoreCase = true)
                    }
                }
            }
            else -> null
        }
    }

fun PackSelection.photoSourceOrNull(): PhotoTopic? =
    if (family != PackFamily.Photo || subId == null) {
        null
    } else {
        PhotoTopic.entries.firstOrNull { it.testTagSuffix == subId }
    }

fun PackSelection.genartTopicOrNull(): GenartTopic? =
    if (family != PackFamily.Genart || subId == null) {
        null
    } else {
        GenartTopic.entries.firstOrNull { it.testTagSuffix == subId }
    }

fun PackSelection.museumTopicOrNull(): MuseumTopic? =
    if (subId == null) {
        null
    } else {
        when (family) {
            PackFamily.Museum,
            PackFamily.Sculpture,
            PackFamily.Painting,
            -> MuseumTopic.entries.firstOrNull { it.testTagSuffix == subId }
            // Photo only offers concrete museum Sources (its own Random tile is
            // StockPhotoCategory.Random) — never resolve MuseumTopic.Random here.
            PackFamily.Photo ->
                MuseumInstitutionTopics.firstOrNull { it.testTagSuffix == subId }
            else -> null
        }
    }

fun PackSelection.videoSourceOrNull(): VideoTopic? =
    if (family != PackFamily.Video || subId == null) {
        null
    } else {
        VideoTopic.entries.firstOrNull { it.testTagSuffix == subId }
    }

/**
 * Source ids to load for Ambient Start. Still packs (museum / photo / painting /
 * sculpture) load capability-matched providers so free-tier slots are not eaten by
 * unrelated Sources — and so we never fall back to a genart engine like Particles.
 * Genart uses the in-memory catalog (`null`).
 */
fun PackSelection.sourceIdsForAmbientLoad(): List<String>? = when (family) {
    PackFamily.Museum -> {
        when (val topic = museumTopicOrNull()) {
            null, MuseumTopic.Random -> MuseumInstitutionTopics.mapNotNull { it.sourceId }
            else -> listOfNotNull(topic.sourceId)
        }
    }
    PackFamily.Painting -> stillKindAmbientIds(ArtworkKind.Painting, museumTopicOrNull())
    PackFamily.Sculpture -> stillKindAmbientIds(ArtworkKind.Sculpture, museumTopicOrNull())
    PackFamily.Photo -> {
        val photoSource = photoSourceOrNull()
        if (photoSource != null) {
            listOf(photoSource.sourceId)
        } else {
            val museum = museumTopicOrNull()
            if (museum != null) {
                listOfNotNull(museum.sourceId)
            } else when (stockCategoryOrNull()) {
                null, StockPhotoCategory.Random ->
                    SourceCapabilities.sourceIdsForKindAmbient(ArtworkKind.Photo)
                else -> SourceCapabilities.sourceIdsWithRemoteSearch(ArtworkKind.Photo)
            }
        }
    }
    PackFamily.Video -> when (val source = videoSourceOrNull()) {
        null -> SourceCapabilities.sourceIdsWithRemoteSearch(ArtworkKind.Video)
        else -> listOf(source.sourceId)
    }
    PackFamily.Genart -> null
}

/** Painting / Sculpture pack: Random → every remote API for that kind. */
private fun stillKindAmbientIds(kind: ArtworkKind, topic: MuseumTopic?): List<String> =
    when (topic) {
        null, MuseumTopic.Random -> SourceCapabilities.sourceIdsForKindAmbient(kind)
        else -> listOfNotNull(topic.sourceId)
    }

fun PackSelection.isGenartCustom(): Boolean =
    family == PackFamily.Genart && genartTopicOrNull() == GenartTopic.Custom

/** True when an empty pool must not fall back to an unrelated generative Artwork. */
fun PackSelection.allowsGenerativeAmbientFallback(): Boolean =
    family == PackFamily.Genart

@StringRes
private fun StockPhotoCategory.packLabelRes(): Int = when (this) {
    StockPhotoCategory.Random -> R.string.stock_topic_random
    StockPhotoCategory.Nature -> R.string.stock_topic_nature
    StockPhotoCategory.City -> R.string.stock_topic_city
    StockPhotoCategory.Ocean -> R.string.stock_topic_ocean
    StockPhotoCategory.Mountains -> R.string.stock_topic_mountains
    StockPhotoCategory.Abstract -> R.string.stock_topic_abstract
    StockPhotoCategory.Architecture -> R.string.stock_topic_architecture
    StockPhotoCategory.Sky -> R.string.stock_topic_sky
    StockPhotoCategory.StreetArt -> R.string.stock_topic_streetart
}

/** Video keyword sub-packs: Random + remote topics (no curated fallback). */
private fun videoStockTopics(): List<StockPhotoCategory> =
    listOf(StockPhotoCategory.Random) + StockPhotoCategory.remoteSearchTopics

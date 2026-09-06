package fr.geoking.arthur.ui.components

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import fr.geoking.arthur.R
import fr.geoking.arthur.shared.domain.Artwork
import fr.geoking.arthur.shared.domain.ArtworkKind
import fr.geoking.arthur.shared.source.BundledPackSource
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
    Video(R.string.kind_video, R.drawable.pack_photo, "video"),
    Sculpture(R.string.kind_sculpture, R.drawable.pack_sculpture, "sculpture"),
    Painting(R.string.kind_painting, R.drawable.pack_painting, "painting"),
}

/**
 * Selected pack for Ambient: a [PackFamily] plus optional subcategory.
 * [subId] null means **All** within that family.
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
)

/** All remote museum Source ids — kept in sync with [MuseumTopic] institutions. */
private val MuseumSourceIds: Set<String> =
    MuseumTopic.entries.mapNotNull { it.sourceId }.toSet()

/** All remote video Source ids — kept in sync with [VideoTopic]. */
private val VideoSourceIds: Set<String> =
    VideoTopic.entries.map { it.sourceId }.toSet()

/**
 * Museum institution sub-packs (every [MuseumTopic] with a Source).
 * Suggestions stays on Painting / Sculpture only.
 */
private val MuseumInstitutionTopics: List<MuseumTopic> =
    MuseumTopic.entries.filter { it.sourceId != null }

/** Genart sub-pack order for the grid (All is prepended separately). */
private val GenartSubTopics = listOf(
    GenartTopic.Nature,
    GenartTopic.Weather,
    GenartTopic.Planets,
    GenartTopic.Abstract,
    GenartTopic.Geometry,
    GenartTopic.Fractal,
    GenartTopic.Custom,
)

fun PackFamily.homeTile(): PackTile = PackTile(
    id = "family_${testTagSuffix}",
    titleRes = titleRes,
    coverRes = coverRes,
    selection = PackSelection(this),
    testTagSuffix = testTagSuffix,
)

fun PackFamily.subPackTiles(): List<PackTile> {
    val all = PackTile(
        id = "sub_${testTagSuffix}_all",
        titleRes = R.string.category_all,
        coverRes = coverRes,
        selection = PackSelection(this),
        testTagSuffix = "${testTagSuffix}_all",
    )
    val rest = when (this) {
        PackFamily.Museum -> MuseumInstitutionTopics.map { topic ->
            PackTile(
                id = "sub_museum_${topic.testTagSuffix}",
                titleRes = topic.labelRes,
                coverRes = PackCovers.museum(topic),
                selection = PackSelection(PackFamily.Museum, topic.testTagSuffix),
                testTagSuffix = "museum_${topic.testTagSuffix}",
            )
        }
        PackFamily.Genart -> GenartSubTopics.map { topic ->
            PackTile(
                id = "sub_genart_${topic.testTagSuffix}",
                titleRes = topic.labelRes,
                coverRes = PackCovers.genart(topic),
                selection = PackSelection(PackFamily.Genart, topic.testTagSuffix),
                testTagSuffix = "genart_${topic.testTagSuffix}",
            )
        }
        PackFamily.Photo -> StockPhotoCategory.entries.map { topic ->
            PackTile(
                id = "sub_photo_${topic.query}",
                titleRes = topic.packLabelRes(),
                coverRes = PackCovers.photo(topic),
                selection = PackSelection(PackFamily.Photo, topic.query),
                testTagSuffix = "photo_${topic.query}",
            )
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
    return listOf(all) + rest
}

fun resolvePackPool(catalog: List<Artwork>, selection: PackSelection): List<Artwork> =
    when (selection.family) {
        PackFamily.Museum -> catalog.filter { art ->
            art.kind == ArtworkKind.Painting || art.kind == ArtworkKind.Sculpture
        }.let { museumKinds ->
            val topic = selection.museumTopicOrNull()
            if (topic == null) {
                museumKinds.filter { it.sourceId in MuseumSourceIds }
            } else {
                museumKinds.filter { matchesMuseumTopic(it, topic) }
            }
        }
        PackFamily.Genart -> {
            val topic = selection.genartTopicOrNull()
            if (topic == null) {
                catalog.filter { CategoryFilter.GENART.matches(it.kind) }
            } else {
                catalog.filterByCategoryAndSources(
                    category = CategoryFilter.GENART,
                    genartTopic = topic,
                )
            }
        }
        PackFamily.Photo -> {
            val topic = selection.stockCategoryOrNull()
            if (topic == null) {
                catalog.filter { CategoryFilter.PHOTO.matches(it.kind) }
            } else {
                catalog.filterByCategoryAndSources(
                    category = CategoryFilter.PHOTO,
                    stockCategory = topic,
                )
            }
        }
        PackFamily.Video -> {
            val source = selection.videoSourceOrNull()
            val topic = selection.stockCategoryOrNull()
            when {
                source != null -> catalog.filter { art ->
                    CategoryFilter.VIDEO.matches(art.kind) && matchesVideoSource(art, source)
                }
                topic != null -> catalog.filterByCategoryAndSources(
                    category = CategoryFilter.VIDEO,
                    stockCategory = topic,
                )
                else -> catalog.filter { art ->
                    CategoryFilter.VIDEO.matches(art.kind) && art.sourceId in VideoSourceIds
                }
            }
        }
        PackFamily.Sculpture -> {
            val topic = selection.museumTopicOrNull()
            if (topic == null) {
                catalog.filter { CategoryFilter.SCULPTURE.matches(it.kind) }
            } else {
                catalog.filterByCategoryAndSources(
                    category = CategoryFilter.SCULPTURE,
                    museumTopic = topic,
                )
            }
        }
        PackFamily.Painting -> {
            val topic = selection.museumTopicOrNull()
            if (topic == null) {
                catalog.filter { CategoryFilter.PAINTING.matches(it.kind) }
            } else {
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
            PackFamily.Photo -> StockPhotoCategory.fromQuery(subId)
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
            null -> MuseumSourceIds.toList()
            else -> listOfNotNull(topic.sourceId)
        }
    }
    PackFamily.Painting -> stillKindAmbientIds(ArtworkKind.Painting, museumTopicOrNull())
    PackFamily.Sculpture -> stillKindAmbientIds(ArtworkKind.Sculpture, museumTopicOrNull())
    PackFamily.Photo -> when (stockCategoryOrNull()) {
        StockPhotoCategory.Suggestions -> listOf(BundledPackSource.ID)
        null, StockPhotoCategory.Random ->
            SourceCapabilities.sourceIdsForKindAmbient(ArtworkKind.Photo)
        else -> SourceCapabilities.sourceIdsWithRemoteSearch(ArtworkKind.Photo)
    }
    PackFamily.Video -> {
        when (val source = videoSourceOrNull()) {
            null -> when (stockCategoryOrNull()) {
                StockPhotoCategory.Suggestions -> emptyList()
                else -> SourceCapabilities.sourceIdsWithRemoteSearch(ArtworkKind.Video)
            }
            else -> listOf(source.sourceId)
        }
    }
    PackFamily.Genart -> null
}

/** Painting / Sculpture pack: All → bundled + remote APIs for that kind keyword. */
private fun stillKindAmbientIds(kind: ArtworkKind, topic: MuseumTopic?): List<String> =
    when (topic) {
        null -> SourceCapabilities.sourceIdsForKindAmbient(kind)
        MuseumTopic.Suggestions -> listOf(BundledPackSource.ID)
        else -> listOfNotNull(topic.sourceId)
    }

fun PackSelection.isGenartCustom(): Boolean =
    family == PackFamily.Genart && genartTopicOrNull() == GenartTopic.Custom

/** True when an empty pool must not fall back to an unrelated generative Artwork. */
fun PackSelection.allowsGenerativeAmbientFallback(): Boolean =
    family == PackFamily.Genart

@StringRes
private fun StockPhotoCategory.packLabelRes(): Int = when (this) {
    StockPhotoCategory.Suggestions -> R.string.stock_topic_suggestions
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

/** Video has no curated Suggestions pack — Random + remote topics only. */
private fun videoStockTopics(): List<StockPhotoCategory> =
    listOf(StockPhotoCategory.Random) + StockPhotoCategory.remoteSearchTopics

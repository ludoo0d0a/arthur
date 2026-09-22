package fr.geoking.arthur.ui.components

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import fr.geoking.arthur.R
import fr.geoking.arthur.shared.domain.Artwork
import fr.geoking.arthur.shared.domain.ArtworkKind
import fr.geoking.arthur.shared.marketplace.MarketplaceCatalog
import fr.geoking.arthur.shared.marketplace.PackOwnership
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
    Personal(R.string.pack_personal, R.drawable.pack_photo, "personal"),
    Video(R.string.kind_video, R.drawable.pack_video, "video"),
    Sculpture(R.string.kind_sculpture, R.drawable.pack_sculpture, "sculpture"),
    Painting(R.string.kind_painting, R.drawable.pack_painting, "painting"),
}

/**
 * Selected pack for Ambient: a [PackFamily] plus optional subcategory.
 * [subId] null means the family's home tile was started directly, with no sub-pack
 * chosen — Museum defaults to Met; other families behave like **Random**.
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
    /** Marketplace SKU when this tile requires a purchase; null = free. */
    val sellablePackId: String? = null,
) {
    fun isLocked(ownership: PackOwnership): Boolean =
        sellablePackId != null && !ownership.owns(sellablePackId)
}

/** Genart sub-pack order for the grid ([GenartTopic.All] leads). */
private val GenartSubTopics = listOf(
    GenartTopic.All,
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

/** Default sub-pack id when opening a family on the Control Plane. */
fun PackFamily.defaultSubId(): String = when (this) {
    PackFamily.Genart -> GenartTopic.All.testTagSuffix
    PackFamily.Museum -> MuseumTopic.Met.testTagSuffix
    PackFamily.Personal -> "all"
    PackFamily.Photo,
    PackFamily.Video,
    PackFamily.Sculpture,
    PackFamily.Painting,
    -> "random"
}

fun PackFamily.homeTile(catalog: List<Artwork> = emptyList()): PackTile = PackTile(
    id = "family_${testTagSuffix}",
    titleRes = titleRes,
    coverRes = coverRes,
    selection = PackSelection(this),
    testTagSuffix = testTagSuffix,
    itemCount = if (this == PackFamily.Genart && catalog.isNotEmpty()) {
        resolvePackPool(catalog, PackSelection(PackFamily.Genart)).size
    } else null,
    sellablePackId = if (this == PackFamily.Personal) {
        MarketplaceCatalog.PERSONAL_PHOTOS_ID
    } else {
        null
    },
)

fun PackFamily.subPackTiles(catalog: List<Artwork> = emptyList()): List<PackTile> = when (this) {
    PackFamily.Museum -> {
        val random = PackTile(
            id = "sub_museum_${MuseumTopic.Random.testTagSuffix}",
            titleRes = MuseumTopic.Random.labelRes,
            coverRes = PackCovers.museum(MuseumTopic.Random),
            selection = PackSelection(PackFamily.Museum, MuseumTopic.Random.testTagSuffix),
            testTagSuffix = "museum_${MuseumTopic.Random.testTagSuffix}",
        )
        val institutions = MuseumTopic.institutions.map { topic ->
            PackTile(
                id = "sub_museum_${topic.testTagSuffix}",
                titleRes = topic.labelRes,
                coverRes = PackCovers.museum(topic),
                selection = PackSelection(PackFamily.Museum, topic.testTagSuffix),
                testTagSuffix = "museum_${topic.testTagSuffix}",
            )
        }
        listOf(random) + institutions
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
            sellablePackId = MarketplaceCatalog.sellablePackIdForGenartTopic(topic.testTagSuffix),
        )
    }
    PackFamily.Personal -> emptyList()
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
        providerSources + topics
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
    PackFamily.Sculpture -> {
        val random = PackTile(
            id = "sub_sculpture_${MuseumTopic.Random.testTagSuffix}",
            titleRes = MuseumTopic.Random.labelRes,
            coverRes = PackCovers.museum(MuseumTopic.Random),
            selection = PackSelection(PackFamily.Sculpture, MuseumTopic.Random.testTagSuffix),
            testTagSuffix = "sculpture_${MuseumTopic.Random.testTagSuffix}",
        )
        val institutions = MuseumTopic.paintingSculptureInstitutions.map { topic ->
            PackTile(
                id = "sub_sculpture_${topic.testTagSuffix}",
                titleRes = topic.labelRes,
                coverRes = PackCovers.museum(topic),
                selection = PackSelection(PackFamily.Sculpture, topic.testTagSuffix),
                testTagSuffix = "sculpture_${topic.testTagSuffix}",
            )
        }
        listOf(random) + institutions
    }
    PackFamily.Painting -> {
        val random = PackTile(
            id = "sub_painting_${MuseumTopic.Random.testTagSuffix}",
            titleRes = MuseumTopic.Random.labelRes,
            coverRes = PackCovers.museum(MuseumTopic.Random),
            selection = PackSelection(PackFamily.Painting, MuseumTopic.Random.testTagSuffix),
            testTagSuffix = "painting_${MuseumTopic.Random.testTagSuffix}",
        )
        val institutions = MuseumTopic.paintingSculptureInstitutions.map { topic ->
            PackTile(
                id = "sub_painting_${topic.testTagSuffix}",
                titleRes = topic.labelRes,
                coverRes = PackCovers.museum(topic),
                selection = PackSelection(PackFamily.Painting, topic.testTagSuffix),
                testTagSuffix = "painting_${topic.testTagSuffix}",
            )
        }
        listOf(random) + institutions
    }
}

fun resolvePackPool(catalog: List<Artwork>, selection: PackSelection): List<Artwork> {
    val pool = when (selection.family) {
        PackFamily.Museum -> {
            val topic = selection.museumTopicOrNull() ?: MuseumTopic.Met
            val museumKinds = catalog.filter { art ->
                art.kind == ArtworkKind.Painting || art.kind == ArtworkKind.Sculpture
            }
            when (topic) {
                // Museum Random spans every institution, including Louvre.
                MuseumTopic.Random ->
                    museumKinds.filter { it.sourceId in MuseumTopic.institutionSourceIds }
                else -> museumKinds.filter { matchesMuseumTopic(it, topic) }
            }
        }
        PackFamily.Genart -> {
            val topic = selection.genartTopicOrNull() ?: GenartTopic.Random
            catalog.filterByCategoryAndSources(
                category = CategoryFilter.GENART,
                genartTopic = topic,
            )
        }
        PackFamily.Personal -> catalog.filter { it.kind == ArtworkKind.PersonalPhoto }
        PackFamily.Photo -> {
            val photoSource = selection.photoSourceOrNull()
            if (photoSource != null) {
                catalog.filter { art ->
                    CategoryFilter.PHOTO.matches(art.kind) && matchesPhotoSource(art, photoSource)
                }
            } else {
                val topic = selection.stockCategoryOrNull() ?: StockPhotoCategory.Random
                catalog.filterByCategoryAndSources(
                    category = CategoryFilter.PHOTO,
                    stockCategory = topic,
                )
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

    val isRandom = when (selection.family) {
        PackFamily.Museum -> selection.museumTopicOrNull() == MuseumTopic.Random
        PackFamily.Personal -> true
        PackFamily.Genart -> (selection.genartTopicOrNull() ?: GenartTopic.Random) == GenartTopic.Random
        PackFamily.Photo ->
            selection.photoSourceOrNull() != null ||
                (selection.stockCategoryOrNull() ?: StockPhotoCategory.Random) == StockPhotoCategory.Random
        PackFamily.Video ->
            selection.videoSourceOrNull() != null ||
                (selection.stockCategoryOrNull() ?: StockPhotoCategory.Random) == StockPhotoCategory.Random
        PackFamily.Sculpture -> (selection.museumTopicOrNull() ?: MuseumTopic.Random) == MuseumTopic.Random
        PackFamily.Painting -> (selection.museumTopicOrNull() ?: MuseumTopic.Random) == MuseumTopic.Random
    }

    return if (isRandom) pool.shuffled() else pool
}

fun PackSelection.stockCategoryOrNull(): StockPhotoCategory? =
    if (subId == null) {
        null
    } else {
        when (family) {
            PackFamily.Photo ->
                if (photoSourceOrNull() != null) {
                    null
                } else {
                    StockPhotoCategory.fromQuery(subId)
                }
            PackFamily.Video -> {
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
 * Source ids to load for Ambient Start. Still packs load capability-matched providers
 * so free-tier slots are not eaten by unrelated Sources — and so we never fall back
 * to a genart engine like Particles. Genart uses the in-memory catalog (`null`).
 */
fun PackSelection.sourceIdsForAmbientLoad(): List<String>? = when (family) {
    PackFamily.Museum -> {
        when (val topic = museumTopicOrNull()) {
            null -> listOf(MuseumTopic.Met.sourceId!!)
            MuseumTopic.Random -> MuseumTopic.institutions.mapNotNull { it.sourceId }
            else -> listOfNotNull(topic.sourceId)
        }
    }
    PackFamily.Painting -> stillKindAmbientIds(museumTopicOrNull())
    PackFamily.Sculpture -> stillKindAmbientIds(museumTopicOrNull())
    PackFamily.Photo -> {
        val photoSource = photoSourceOrNull()
        if (photoSource != null) {
            listOf(photoSource.sourceId)
        } else {
            SourceCapabilities.sourceIdsForPhotoProviders()
        }
    }
    PackFamily.Video -> when (val source = videoSourceOrNull()) {
        null -> SourceCapabilities.sourceIdsWithRemoteSearch(ArtworkKind.Video)
        else -> listOf(source.sourceId)
    }
    PackFamily.Genart -> null
    PackFamily.Personal -> null
}

/** Painting / Sculpture pack: Random → museums without Louvre. */
private fun stillKindAmbientIds(topic: MuseumTopic?): List<String> =
    when (topic) {
        null, MuseumTopic.Random -> MuseumTopic.paintingSculptureInstitutions.mapNotNull { it.sourceId }
        else -> listOfNotNull(topic.sourceId)
    }

fun PackSelection.isGenartCustom(): Boolean =
    family == PackFamily.Genart && genartTopicOrNull() == GenartTopic.Custom

/** True when an empty pool may resolve a generative Artwork from the in-memory catalog. */
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

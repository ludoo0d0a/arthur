package fr.geoking.arthur.ui.components

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import fr.geoking.arthur.R
import fr.geoking.arthur.shared.domain.Artwork
import fr.geoking.arthur.shared.domain.ArtworkKind
import fr.geoking.arthur.shared.marketplace.GenartPackTopics
import fr.geoking.arthur.shared.source.ArticSource
import fr.geoking.arthur.shared.source.ClevelandSource
import fr.geoking.arthur.shared.source.CoverrSource
import fr.geoking.arthur.shared.source.EuropeanaSource
import fr.geoking.arthur.shared.source.HarvardSource
import fr.geoking.arthur.shared.source.LouvreSource
import fr.geoking.arthur.shared.source.DeviantArtSource
import fr.geoking.arthur.shared.source.MetSource
import fr.geoking.arthur.shared.source.PexelsSource
import fr.geoking.arthur.shared.source.PexelsVideoSource
import fr.geoking.arthur.shared.source.PixabayVideoSource
import fr.geoking.arthur.shared.source.RijksmuseumSource
import fr.geoking.arthur.shared.source.UnsplashSource
import fr.geoking.arthur.shared.source.SmithsonianSource
import fr.geoking.arthur.shared.source.SourceCapabilities
import fr.geoking.arthur.shared.source.StockPhotoCategory
import fr.geoking.arthur.shared.source.WikimediaStreetArtSource

/** Top-level chips: All → Genart → Painting → Photo → Video → Sculpture (Personal last). */
enum class CategoryFilter(@get:StringRes val labelRes: Int) {
    ALL(R.string.category_all),
    GENART(R.string.kind_genart),
    PAINTING(R.string.kind_painting),
    PHOTO(R.string.kind_photo),
    VIDEO(R.string.kind_video),
    SCULPTURE(R.string.kind_sculpture),
    PERSONAL(R.string.kind_personal);

    fun matches(kind: ArtworkKind): Boolean = when (this) {
        ALL -> true
        GENART ->
            kind == ArtworkKind.Genart ||
                kind == ArtworkKind.FractalPreset ||
                kind == ArtworkKind.CustomFractal
        PAINTING -> kind == ArtworkKind.Painting
        PHOTO -> kind == ArtworkKind.Photo
        VIDEO -> kind == ArtworkKind.Video
        SCULPTURE -> kind == ArtworkKind.Sculpture
        PERSONAL -> kind == ArtworkKind.PersonalPhoto
    }

    fun showsGenartTopics(): Boolean = this == GENART

    fun showsPhotoTopics(): Boolean = this == PHOTO

    fun showsVideoTopics(): Boolean = this == VIDEO

    fun showsMuseumTopics(): Boolean = this == PAINTING || this == SCULPTURE
}

/**
 * Genart subcategory: Random mix, Fractal presets, Custom fractal, Abstract, Nature,
 * Weather, Geometry, Planets.
 */
enum class GenartTopic(
    @get:StringRes val labelRes: Int,
    val testTagSuffix: String,
) {
    All(R.string.category_all, "all"),
    Random(R.string.stock_topic_random, "random"),
    Tapet(R.string.genart_topic_tapet, "tapet"),
    Nature(R.string.genart_topic_nature, "nature"),
    Weather(R.string.genart_topic_weather, "weather"),
    Water(R.string.genart_topic_water, "water"),
    Life(R.string.genart_topic_life, "life"),
    Earth(R.string.genart_topic_earth, "earth"),
    Planets(R.string.genart_topic_planets, "planets"),
    SciFi(R.string.genart_topic_scifi, "scifi"),
    Abstract(R.string.genart_topic_abstract, "abstract"),
    Geometry(R.string.genart_topic_geometry, "geometry"),
    Fractal(R.string.genart_topic_fractal, "fractal"),
    Custom(R.string.genart_topic_custom, "custom"),
}

/**
 * Painting / Sculpture subcategory: Random mix across every museum Source, or one
 * specific remote museum Source.
 */
enum class MuseumTopic(
    val sourceId: String?,
    @get:StringRes val labelRes: Int,
    val testTagSuffix: String,
) {
    Random(null, R.string.stock_topic_random, "random"),
    Met(MetSource.ID, R.string.source_met, MetSource.ID),
    Rijksmuseum(RijksmuseumSource.ID, R.string.source_rijksmuseum, RijksmuseumSource.ID),
    Artic(ArticSource.ID, R.string.source_artic, ArticSource.ID),
    Cleveland(ClevelandSource.ID, R.string.source_cleveland, ClevelandSource.ID),
    Europeana(EuropeanaSource.ID, R.string.source_europeana, EuropeanaSource.ID),
    Harvard(HarvardSource.ID, R.string.source_harvard, HarvardSource.ID),
    Smithsonian(SmithsonianSource.ID, R.string.source_smithsonian, SmithsonianSource.ID),
    Louvre(LouvreSource.ID, R.string.source_louvre, LouvreSource.ID),
    ;

    companion object {
        /** Every concrete museum institution (excludes [Random]). */
        val institutions: List<MuseumTopic> by lazy {
            entries.filter { it.sourceId != null }
        }

        /**
         * Painting / Sculpture institution tiles: all museums except Louvre
         * (Louvre stays under the Museum pack only — see navigation-structure.md).
         */
        val paintingSculptureInstitutions: List<MuseumTopic> by lazy {
            institutions.filter { it != Louvre }
        }

        /** Every concrete museum institution's Source id (excludes [Random]). */
        val institutionSourceIds: Set<String> by lazy {
            institutions.mapNotNull { it.sourceId }.toSet()
        }

        /** Painting / Sculpture Random ambient: institutions without Louvre. */
        val paintingSculptureSourceIds: Set<String> by lazy {
            paintingSculptureInstitutions.mapNotNull { it.sourceId }.toSet()
        }
    }
}

/**
 * Photo pack Source tiles — one Remote Source each (same role as [MuseumTopic] institutions).
 */
enum class PhotoTopic(
    val sourceId: String,
    @get:StringRes val labelRes: Int,
    val testTagSuffix: String,
) {
    Pexels(PexelsSource.ID, R.string.source_pexels_video, PexelsSource.ID),
    Unsplash(UnsplashSource.ID, R.string.source_unsplash, UnsplashSource.ID),
    DeviantArt(DeviantArtSource.ID, R.string.source_deviantart, DeviantArtSource.ID),
    Wikimedia(WikimediaStreetArtSource.ID, R.string.source_wikimedia, WikimediaStreetArtSource.ID),
}

/**
 * Video pack Source tiles — one Remote Source each (same role as [MuseumTopic] institutions).
 * Keyword topics stay on [StockPhotoCategory] and search across all video Sources.
 */
enum class VideoTopic(
    val sourceId: String,
    @get:StringRes val labelRes: Int,
    val testTagSuffix: String,
) {
    Pexels(PexelsVideoSource.ID, R.string.source_pexels_video, PexelsVideoSource.ID),
    Unsplash(UnsplashSource.ID, R.string.source_unsplash, UnsplashSource.ID),
    Pixabay(PixabayVideoSource.ID, R.string.source_pixabay_video, PixabayVideoSource.ID),
    Coverr(CoverrSource.ID, R.string.source_coverr, CoverrSource.ID),
}

fun List<Artwork>.filterByCategoryAndSources(
    category: CategoryFilter,
    museumTopic: MuseumTopic? = null,
    stockCategory: StockPhotoCategory? = null,
    genartTopic: GenartTopic? = null,
): List<Artwork> = filter { art ->
    if (!category.matches(art.kind)) return@filter false
    when {
        category.showsGenartTopics() && genartTopic != null ->
            matchesGenartTopic(art, genartTopic)
        category == CategoryFilter.PHOTO && stockCategory != null ->
            matchesPhotoTopic(art, stockCategory)
        category == CategoryFilter.VIDEO && stockCategory != null ->
            matchesVideoTopic(art, stockCategory)
        category.showsMuseumTopics() && museumTopic != null ->
            matchesMuseumTopic(art, museumTopic)
        else -> true
    }
}

/**
 * Applies subcategory filters when they match; otherwise hides the subcategory row
 * and falls back to the parent category matches.
 */
data class ResolvedCategoryCatalog(
    val items: List<Artwork>,
    val showSubfilters: Boolean,
)

fun resolveCategoryCatalog(
    catalog: List<Artwork>,
    category: CategoryFilter,
    museumTopic: MuseumTopic? = null,
    stockCategory: StockPhotoCategory? = null,
    genartTopic: GenartTopic? = null,
): ResolvedCategoryCatalog {
    val categoryMatches = catalog.filter { category.matches(it.kind) }
    val usesSubfilter = category.showsGenartTopics() ||
        category.showsPhotoTopics() ||
        category.showsVideoTopics() ||
        category.showsMuseumTopics()
    if (!usesSubfilter) {
        return ResolvedCategoryCatalog(items = categoryMatches, showSubfilters = false)
    }
    val subMatches = catalog.filterByCategoryAndSources(
        category = category,
        museumTopic = museumTopic,
        stockCategory = stockCategory,
        genartTopic = genartTopic,
    )
    // Photo / video topics drive remote search — keep chips visible even while results load.
    if (category.showsPhotoTopics() || category.showsVideoTopics()) {
        return ResolvedCategoryCatalog(
            items = subMatches.ifEmpty { categoryMatches },
            showSubfilters = true,
        )
    }
    return if (subMatches.isNotEmpty()) {
        ResolvedCategoryCatalog(items = subMatches, showSubfilters = true)
    } else {
        // Genart / museum subcategory has no hits — hide chips, show category matches.
        ResolvedCategoryCatalog(items = categoryMatches, showSubfilters = false)
    }
}

const val CatalogPageSize = 20

fun List<Artwork>.takeCatalogPage(visibleCount: Int): List<Artwork> =
    take(visibleCount.coerceAtLeast(0))

fun List<Artwork>.canLoadMoreCatalog(visibleCount: Int): Boolean =
    visibleCount < size

fun matchesGenartTopic(art: Artwork, topic: GenartTopic): Boolean = when (topic) {
    GenartTopic.All -> true
    GenartTopic.Random -> true
    GenartTopic.Tapet -> art.id in GenartPackTopics.TAPET_IDS
    GenartTopic.Nature -> art.id in GenartPackTopics.NATURE_IDS
    GenartTopic.Weather -> art.id in GenartPackTopics.WEATHER_IDS
    GenartTopic.Water -> art.id in GenartPackTopics.WATER_IDS
    GenartTopic.Life -> art.id in GenartPackTopics.LIFE_IDS
    GenartTopic.Earth -> art.id in GenartPackTopics.EARTH_IDS
    GenartTopic.Planets -> art.id in GenartPackTopics.PLANETS_IDS
    GenartTopic.SciFi -> art.id in GenartPackTopics.SCIFI_IDS
    GenartTopic.Abstract -> art.id in GenartPackTopics.ABSTRACT_IDS
    GenartTopic.Geometry -> art.id in GenartPackTopics.GEOMETRY_IDS
    GenartTopic.Fractal -> art.kind == ArtworkKind.FractalPreset
    GenartTopic.Custom -> art.kind == ArtworkKind.CustomFractal
}

fun genartTopicsFor(art: Artwork): List<GenartTopic> =
    GenartTopic.entries.filter { topic ->
        topic != GenartTopic.All && topic != GenartTopic.Random && matchesGenartTopic(art, topic)
    }

@Composable
fun Artwork.genartCategorySubtitle(): String? {
    if (!CategoryFilter.GENART.matches(kind)) return null
    val topics = genartTopicsFor(this)
    if (topics.isEmpty()) return null
    val labels = topics.map { stringResource(it.labelRes) }
    return labels.joinToString(", ")
}

/** Random / other topics = remote photo search across capable Sources. */
fun matchesPhotoTopic(art: Artwork, stockCategory: StockPhotoCategory): Boolean =
    when (stockCategory) {
        StockPhotoCategory.Random,
        StockPhotoCategory.Nature,
        StockPhotoCategory.City,
        StockPhotoCategory.Ocean,
        StockPhotoCategory.Mountains,
        StockPhotoCategory.Abstract,
        StockPhotoCategory.Architecture,
        StockPhotoCategory.Sky,
        StockPhotoCategory.StreetArt,
        -> art.sourceId in SourceCapabilities.sourceIdsForPhotoProviders()
    }

fun matchesVideoTopic(art: Artwork, stockCategory: StockPhotoCategory): Boolean =
    when (stockCategory) {
        StockPhotoCategory.Random,
        StockPhotoCategory.Nature,
        StockPhotoCategory.City,
        StockPhotoCategory.Ocean,
        StockPhotoCategory.Mountains,
        StockPhotoCategory.Abstract,
        StockPhotoCategory.Architecture,
        StockPhotoCategory.Sky,
        StockPhotoCategory.StreetArt,
        -> art.sourceId in SourceCapabilities.sourceIdsWithRemoteSearch(ArtworkKind.Video)
    }

/** Random = every museum Source; other topics = that museum Source only. */
fun matchesMuseumTopic(art: Artwork, museumTopic: MuseumTopic): Boolean =
    when (museumTopic) {
        // Sculpture / Painting Random excludes Louvre (Museum pack only).
        MuseumTopic.Random -> art.sourceId in MuseumTopic.paintingSculptureSourceIds
        else -> art.sourceId == museumTopic.sourceId
    }

fun matchesPhotoSource(art: Artwork, photoTopic: PhotoTopic): Boolean =
    art.sourceId == photoTopic.sourceId

fun matchesVideoSource(art: Artwork, videoTopic: VideoTopic): Boolean =
    art.sourceId == videoTopic.sourceId

@Composable
fun CategoryFilterRow(
    selectedCategory: CategoryFilter,
    onCategorySelected: (CategoryFilter) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 20.dp),
    horizontalSpacing: Dp = 8.dp,
    chipModifier: (CategoryFilter, Int) -> Modifier = { _, _ -> Modifier },
) {
    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .testTag("category_filter_row"),
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(horizontalSpacing),
    ) {
        itemsIndexed(CategoryFilter.entries) { index, category ->
            FilterChip(
                selected = selectedCategory == category,
                onClick = { onCategorySelected(category) },
                label = { Text(stringResource(category.labelRes)) },
                modifier = chipModifier(category, index)
                    .testTag("filter_chip_${category.name.lowercase()}"),
            )
        }
    }
}

@Composable
fun ContextualSubFilterRow(
    selectedCategory: CategoryFilter,
    museumTopic: MuseumTopic,
    onMuseumTopicChange: (MuseumTopic) -> Unit,
    modifier: Modifier = Modifier,
    stockCategory: StockPhotoCategory? = null,
    onStockCategoryChange: ((StockPhotoCategory) -> Unit)? = null,
    genartTopic: GenartTopic = GenartTopic.Fractal,
    onGenartTopicChange: (GenartTopic) -> Unit = {},
    contentPadding: PaddingValues = PaddingValues(horizontal = 20.dp),
    horizontalSpacing: Dp = 8.dp,
) {
    val showGenart = selectedCategory.showsGenartTopics()
    val showTopics = (selectedCategory.showsPhotoTopics() || selectedCategory.showsVideoTopics()) &&
        stockCategory != null &&
        onStockCategoryChange != null
    val showMuseum = selectedCategory.showsMuseumTopics()
    Column(modifier = modifier) {
        AnimatedVisibility(
            visible = showGenart,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            GenartTopicFilterRow(
                genartTopic = genartTopic,
                onGenartTopicChange = onGenartTopicChange,
                contentPadding = contentPadding,
                horizontalSpacing = horizontalSpacing,
            )
        }
        AnimatedVisibility(
            visible = showTopics,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            if (stockCategory != null && onStockCategoryChange != null) {
                StockTopicFilterRow(
                    stockCategory = stockCategory,
                    onStockCategoryChange = onStockCategoryChange,
                    contentPadding = contentPadding,
                    horizontalSpacing = horizontalSpacing,
                )
            }
        }
        AnimatedVisibility(
            visible = showMuseum,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            MuseumTopicFilterRow(
                museumTopic = museumTopic,
                onMuseumTopicChange = onMuseumTopicChange,
                contentPadding = contentPadding,
                horizontalSpacing = horizontalSpacing,
            )
        }
    }
}

@Composable
private fun GenartTopicFilterRow(
    genartTopic: GenartTopic,
    onGenartTopicChange: (GenartTopic) -> Unit,
    contentPadding: PaddingValues,
    horizontalSpacing: Dp,
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .testTag("genart_topic_row"),
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(horizontalSpacing),
    ) {
        items(GenartTopic.entries) { topic ->
            FilterChip(
                selected = genartTopic == topic,
                onClick = { onGenartTopicChange(topic) },
                label = { Text(stringResource(topic.labelRes)) },
                modifier = Modifier.testTag("genart_topic_${topic.testTagSuffix}"),
            )
        }
    }
}

@Composable
private fun StockTopicFilterRow(
    stockCategory: StockPhotoCategory,
    onStockCategoryChange: (StockPhotoCategory) -> Unit,
    contentPadding: PaddingValues,
    horizontalSpacing: Dp,
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .testTag("stock_topic_row"),
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(horizontalSpacing),
    ) {
        items(StockPhotoCategory.entries) { topic ->
            FilterChip(
                selected = stockCategory == topic,
                onClick = { onStockCategoryChange(topic) },
                label = { Text(stringResource(topic.labelRes())) },
                modifier = Modifier.testTag("stock_topic_${topic.query}"),
            )
        }
    }
}

@Composable
private fun MuseumTopicFilterRow(
    museumTopic: MuseumTopic,
    onMuseumTopicChange: (MuseumTopic) -> Unit,
    contentPadding: PaddingValues,
    horizontalSpacing: Dp,
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .testTag("museum_topic_row"),
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(horizontalSpacing),
    ) {
        items(listOf(MuseumTopic.Random) + MuseumTopic.paintingSculptureInstitutions) { topic ->
            FilterChip(
                selected = museumTopic == topic,
                onClick = { onMuseumTopicChange(topic) },
                label = { Text(stringResource(topic.labelRes)) },
                modifier = Modifier.testTag("museum_topic_${topic.testTagSuffix}"),
            )
        }
    }
}

@StringRes
private fun StockPhotoCategory.labelRes(): Int = when (this) {
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

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
import fr.geoking.arthur.shared.source.ArticSource
import fr.geoking.arthur.shared.source.ClevelandSource
import fr.geoking.arthur.shared.source.CoverrSource
import fr.geoking.arthur.shared.source.EuropeanaSource
import fr.geoking.arthur.shared.source.GenartSource
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
    WikimediaStreetArt(
        WikimediaStreetArtSource.ID,
        R.string.source_wikimedia_streetart,
        WikimediaStreetArtSource.ID,
    ),
    ;

    companion object {
        /** Every concrete museum institution's Source id (excludes [Random]). */
        val institutionSourceIds: Set<String> by lazy {
            entries.mapNotNull { it.sourceId }.toSet()
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
    GenartTopic.Tapet -> art.id in GENART_TAPET_IDS
    GenartTopic.Nature -> art.id in GENART_NATURE_IDS
    GenartTopic.Weather -> art.id in GENART_WEATHER_IDS
    GenartTopic.Water -> art.id in GENART_WATER_IDS
    GenartTopic.Life -> art.id in GENART_LIFE_IDS
    GenartTopic.Earth -> art.id in GENART_EARTH_IDS
    GenartTopic.Planets -> art.id in GENART_PLANETS_IDS
    GenartTopic.SciFi -> art.id in GENART_SCIFI_IDS
    GenartTopic.Abstract -> art.id in GENART_ABSTRACT_IDS
    GenartTopic.Geometry -> art.id in GENART_GEOMETRY_IDS
    GenartTopic.Fractal -> art.kind == ArtworkKind.FractalPreset
    GenartTopic.Custom -> art.kind == ArtworkKind.CustomFractal
}

private val GENART_TAPET_IDS = setOf(
    GenartSource.GRADIENT_MESH,
    GenartSource.BLOBS,
    GenartSource.VORONOI,
    GenartSource.SILK,
    GenartSource.ARC_MOSAIC,
    GenartSource.RIBBONS,
    GenartSource.NOISE_FIELD,
    GenartSource.LOW_FREQ_NOISE_FIELD,
    GenartSource.TONAL_GEOMETRY,
    GenartSource.PAPER_CUT_PACK,
    GenartSource.DIAMOND_WEAVE,
    GenartSource.CHROMATIC_BLOBS,
)

private val GENART_WEATHER_IDS = setOf(
    GenartSource.SNOW,
    GenartSource.AURORA,
    GenartSource.CLOUDS,
    GenartSource.RAIN,
    GenartSource.FOG,
    GenartSource.SUNBEAMS,
    GenartSource.STORM,
    GenartSource.LIGHT_DRIZZLE,
    GenartSource.RAINBOW,
    GenartSource.SMOG,
    GenartSource.SMOKE,
    GenartSource.HEAT_HAZE,
    GenartSource.SUNSHINE,
    GenartSource.STEAM_CURL,
    GenartSource.RAIN_ON_GLASS,
    GenartSource.WATERFALL_MIST,
    GenartSource.SOFT_WIND_STREAKS,
    GenartSource.DAY_NIGHT_WASH,
    GenartSource.FROST_CRYSTALS,
    GenartSource.ECLIPSE_CORONA,
    GenartSource.FIRE,
    GenartSource.AURORA_WASH,
    GenartSource.FIREWORKS,
)

private val GENART_NATURE_IDS = setOf(
    GenartSource.GRASS,
    GenartSource.BIRD_FLOCK,
    GenartSource.MOUNTAINS,
    GenartSource.POND_RIPPLES,
    GenartSource.FALLING_LEAVES,
    GenartSource.FIRE_EMBERS,
    GenartSource.DUNES,
    GenartSource.FISH_SCHOOL,
    GenartSource.FIREFLIES,
    GenartSource.BUBBLES,
    GenartSource.CHERRY_BLOSSOMS,
    GenartSource.WAVES,
    GenartSource.TREE,
    GenartSource.FLOWER,
    GenartSource.LAKE,
    GenartSource.FIELDS,
    GenartSource.REEDS,
    GenartSource.MOSS_GROWTH,
    GenartSource.RIVERS,
    GenartSource.CANYON_DUNES,
    GenartSource.DRIFTING_POLLEN,
    GenartSource.WIND_CHIME,
)

private val GENART_WATER_IDS = setOf(
    GenartSource.WAVES,
    GenartSource.POND_RIPPLES,
    GenartSource.RAIN,
    GenartSource.FISH_SCHOOL,
    GenartSource.BUBBLES,
    GenartSource.LIGHT_DRIZZLE,
    GenartSource.PEBBLE_SHORE_WASH,
    GenartSource.FROST_CRYSTALS,
    GenartSource.MOONLIGHT_RIPPLES,
    GenartSource.INK_IN_WATER,
    GenartSource.TERRARIUM_DRIP,
    GenartSource.AQUARIUM,
    GenartSource.RAIN_ON_GLASS,
    GenartSource.REEDS,
    GenartSource.RIVERS,
    GenartSource.LAKE,
    GenartSource.WATERFALL_MIST,
    GenartSource.GERSTNER_OCEAN,
    GenartSource.SOFT_CAUSTICS,
)

private val GENART_LIFE_IDS = setOf(
    GenartSource.BIRD_FLOCK,
    GenartSource.FISH_SCHOOL,
    GenartSource.FIREFLIES,
    GenartSource.ANT_TRAILS,
    GenartSource.SLEEPING_PET,
    GenartSource.DISTANT_DINOSAURS,
    GenartSource.MOSS_GROWTH,
    GenartSource.TERRARIUM_DRIP,
    GenartSource.AQUARIUM,
    GenartSource.TREE,
    GenartSource.FLOWER,
    GenartSource.CHERRY_BLOSSOMS,
    GenartSource.DRIFTING_POLLEN,
    GenartSource.CANDLE_EMBER,
    GenartSource.WIND_CHIME,
)

private val GENART_EARTH_IDS = setOf(
    GenartSource.GRASS,
    GenartSource.MOUNTAINS,
    GenartSource.FIRE_EMBERS,
    GenartSource.DUNES,
    GenartSource.HEAT_HAZE,
    GenartSource.LANDSLIDE_DUST,
    GenartSource.PEBBLE_SHORE_WASH,
    GenartSource.TUMBLEWEED_DRIFT,
    GenartSource.DISTANT_DINOSAURS,
    GenartSource.MOSS_GROWTH,
    GenartSource.FIELDS,
    GenartSource.CANYON_DUNES,
    GenartSource.CONTINENTS,
    GenartSource.FIRE,
)

private val GENART_PLANETS_IDS = setOf(
    GenartSource.SPHERE,
    GenartSource.CONSTELLATION,
    GenartSource.METEORS,
    GenartSource.NEBULA,
    GenartSource.STAR_FIELD,
    GenartSource.SOLAR_SYSTEM,
    GenartSource.ECLIPSE_CORONA,
    GenartSource.SPACE_STATION_DRIFT,
    GenartSource.SPIRAL_GALAXY,
    GenartSource.ASTEROIDS,
    GenartSource.CONTINENTS,
    GenartSource.HALO_ECLIPSE,
    GenartSource.ATMOSPHERIC_ASTEROID,
    GenartSource.SATURN_RINGS,
    GenartSource.LAVA_SUN,
    GenartSource.MOON,
)

private val GENART_SCIFI_IDS = setOf(
    GenartSource.PSEUDO3D,
    GenartSource.TUNNEL,
    GenartSource.STAR_FIELD,
    GenartSource.SOLAR_SYSTEM,
    GenartSource.ION_TRAIL,
    GenartSource.WARP_STREAK,
    GenartSource.SPACE_STATION_DRIFT,
    GenartSource.CITY_LIGHTS,
    GenartSource.ROADS,
    GenartSource.ASTEROIDS,
    GenartSource.SPIRAL_GALAXY,
    GenartSource.DATA_HORIZON,
    GenartSource.VORTEX_GLOW,
    GenartSource.MATRIX,
    GenartSource.SUPERDRIVE,
    GenartSource.FIREWORKS,
    GenartSource.ATMOSPHERIC_ASTEROID,
    GenartSource.SATURN_RINGS,
    GenartSource.LAVA_SUN,
)

private val GENART_ABSTRACT_IDS = setOf(
    GenartSource.BREATH_CIRCLES,
    GenartSource.RIBBONS,
    GenartSource.BLOBS,
    GenartSource.NOISE_FIELD,
    GenartSource.VORONOI,
    GenartSource.SILK,
    GenartSource.GRADIENT_MESH,
    GenartSource.ARC_MOSAIC,
    GenartSource.LOW_FREQ_NOISE_FIELD,
    GenartSource.INK_IN_WATER,
    GenartSource.SOFT_SHADOWS,
    GenartSource.PARTICLES,
    GenartSource.SOFT_CAUSTICS,
    GenartSource.PAPER_CUT_PACK,
    GenartSource.DIAMOND_WEAVE,
    GenartSource.CHROMATIC_BLOBS,
    GenartSource.PRISMATIC_SHADOWS,
    GenartSource.AURORA_WASH,
    GenartSource.VORTEX_GLOW,
    GenartSource.SPECTRAL_FOLDS,
    GenartSource.HALO_ECLIPSE,
    GenartSource.MATRIX,
    GenartSource.DRIFTING_HALOS,
    GenartSource.SUPERDRIVE,
)

private val GENART_GEOMETRY_IDS = setOf(
    GenartSource.PARTICLES,
    GenartSource.PSEUDO3D,
    GenartSource.SOFT_SHADOWS,
    GenartSource.TUNNEL,
    GenartSource.TONAL_GEOMETRY,
    GenartSource.MICRO,
    GenartSource.ROADS,
    GenartSource.ARC_MOSAIC,
    GenartSource.SPHERE,
    GenartSource.CITY_LIGHTS,
    GenartSource.DATA_HORIZON,
    GenartSource.WARP_STREAK,
    GenartSource.DIAMOND_WEAVE,
    GenartSource.PRISMATIC_SHADOWS,
    GenartSource.DRIFTING_HALOS,
)

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
        -> art.sourceId in SourceCapabilities.sourceIdsWithRemoteSearch(ArtworkKind.Photo)
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
        MuseumTopic.Random -> art.sourceId in MuseumTopic.institutionSourceIds
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
        items(MuseumTopic.entries) { topic ->
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

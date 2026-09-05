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
import fr.geoking.arthur.shared.source.BundledPackSource
import fr.geoking.arthur.shared.source.ClevelandSource
import fr.geoking.arthur.shared.source.GenartSource
import fr.geoking.arthur.shared.source.MetSource
import fr.geoking.arthur.shared.source.PexelsSource
import fr.geoking.arthur.shared.source.RijksmuseumSource
import fr.geoking.arthur.shared.source.StockPhotoCategory
import fr.geoking.arthur.shared.source.UnsplashSource

/** Top-level chips: All → Genart → Painting → Photo → Sculpture (Personal last). */
enum class CategoryFilter(@get:StringRes val labelRes: Int) {
    ALL(R.string.category_all),
    GENART(R.string.kind_genart),
    PAINTING(R.string.kind_painting),
    PHOTO(R.string.kind_photo),
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
        SCULPTURE -> kind == ArtworkKind.Sculpture
        PERSONAL -> kind == ArtworkKind.PersonalPhoto
    }

    fun showsGenartTopics(): Boolean = this == GENART

    fun showsPhotoTopics(): Boolean = this == PHOTO

    fun showsMuseumTopics(): Boolean = this == PAINTING || this == SCULPTURE
}

/**
 * Genart subcategory: Fractal presets, Custom fractal, Nature, Geometry, Planets.
 */
enum class GenartTopic(
    @get:StringRes val labelRes: Int,
    val testTagSuffix: String,
) {
    Fractal(R.string.genart_topic_fractal, "fractal"),
    Custom(R.string.genart_topic_custom, "custom"),
    Nature(R.string.genart_topic_nature, "nature"),
    Geometry(R.string.genart_topic_geometry, "geometry"),
    Planets(R.string.genart_topic_planets, "planets"),
}

/**
 * Painting / Sculpture subcategory: curated Suggestions, or a remote museum Source.
 */
enum class MuseumTopic(
    val sourceId: String?,
    @get:StringRes val labelRes: Int,
    val testTagSuffix: String,
) {
    Suggestions(null, R.string.stock_topic_suggestions, "suggestions"),
    Met(MetSource.ID, R.string.source_met, MetSource.ID),
    Rijksmuseum(RijksmuseumSource.ID, R.string.source_rijksmuseum, RijksmuseumSource.ID),
    Artic(ArticSource.ID, R.string.source_artic, ArticSource.ID),
    Cleveland(ClevelandSource.ID, R.string.source_cleveland, ClevelandSource.ID),
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
        category.showsMuseumTopics() && museumTopic != null ->
            matchesMuseumTopic(art, museumTopic)
        else -> true
    }
}

fun matchesGenartTopic(art: Artwork, topic: GenartTopic): Boolean = when (topic) {
    GenartTopic.Fractal -> art.kind == ArtworkKind.FractalPreset
    GenartTopic.Custom -> art.kind == ArtworkKind.CustomFractal
    GenartTopic.Nature -> art.id in GENART_NATURE_IDS
    GenartTopic.Geometry -> art.id in GENART_GEOMETRY_IDS
    GenartTopic.Planets -> art.id in GENART_PLANETS_IDS
}

private val GENART_NATURE_IDS = setOf(
    GenartSource.SNOW,
    GenartSource.GRASS,
    GenartSource.BIRD_FLOCK,
    GenartSource.MOUNTAINS,
    GenartSource.AURORA,
    GenartSource.POND_RIPPLES,
    GenartSource.FALLING_LEAVES,
    GenartSource.FIRE_EMBERS,
    GenartSource.DUNES,
    GenartSource.CLOUDS,
    GenartSource.RAIN,
    GenartSource.FOG,
    GenartSource.FISH_SCHOOL,
    GenartSource.FIREFLIES,
    GenartSource.SUNBEAMS,
    GenartSource.BUBBLES,
    GenartSource.CHERRY_BLOSSOMS,
    GenartSource.WAVES,
)

private val GENART_GEOMETRY_IDS = setOf(
    GenartSource.PARTICLES,
    GenartSource.PSEUDO3D,
    GenartSource.SOFT_SHADOWS,
    GenartSource.TUNNEL,
    GenartSource.TONAL_GEOMETRY,
    GenartSource.MICRO,
    GenartSource.BREATH_CIRCLES,
    GenartSource.RIBBONS,
)

private val GENART_PLANETS_IDS = setOf(
    GenartSource.SPHERE,
    GenartSource.CONSTELLATION,
    GenartSource.METEORS,
    GenartSource.NEBULA,
)

/** Suggestions = curated bundled photos; other topics = remote stock (Pexels / Unsplash). */
fun matchesPhotoTopic(art: Artwork, stockCategory: StockPhotoCategory): Boolean =
    if (stockCategory == StockPhotoCategory.Suggestions) {
        art.sourceId == BundledPackSource.ID
    } else {
        art.sourceId == PexelsSource.ID || art.sourceId == UnsplashSource.ID
    }

/** Suggestions = curated bundled; other topics = that museum Source only. */
fun matchesMuseumTopic(art: Artwork, museumTopic: MuseumTopic): Boolean =
    when (museumTopic) {
        MuseumTopic.Suggestions -> art.sourceId == BundledPackSource.ID
        else -> art.sourceId == museumTopic.sourceId
    }

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
    val showTopics = selectedCategory.showsPhotoTopics() &&
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
    StockPhotoCategory.Suggestions -> R.string.stock_topic_suggestions
    StockPhotoCategory.Nature -> R.string.stock_topic_nature
    StockPhotoCategory.City -> R.string.stock_topic_city
    StockPhotoCategory.Ocean -> R.string.stock_topic_ocean
    StockPhotoCategory.Mountains -> R.string.stock_topic_mountains
    StockPhotoCategory.Abstract -> R.string.stock_topic_abstract
    StockPhotoCategory.Architecture -> R.string.stock_topic_architecture
    StockPhotoCategory.Sky -> R.string.stock_topic_sky
}

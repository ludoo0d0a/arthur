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
import fr.geoking.arthur.shared.source.BundledPackSource
import fr.geoking.arthur.shared.source.MetSource
import fr.geoking.arthur.shared.source.RijksmuseumSource
import fr.geoking.arthur.shared.source.StockPhotoCategory

enum class CategoryFilter(@get:StringRes val labelRes: Int) {
    ALL(R.string.category_all),
    PHOTO(R.string.kind_photo),
    SCULPTURE(R.string.kind_sculpture),
    GENART(R.string.kind_genart),
    FRACTAL(R.string.kind_fractal),
    PAINTING(R.string.kind_painting),
    PERSONAL(R.string.kind_personal);

    fun matches(kind: ArtworkKind): Boolean = when (this) {
        ALL -> true
        PHOTO -> kind == ArtworkKind.Photo
        SCULPTURE -> kind == ArtworkKind.Sculpture
        GENART -> kind == ArtworkKind.Genart
        FRACTAL -> kind == ArtworkKind.FractalPreset || kind == ArtworkKind.CustomFractal
        PAINTING -> kind == ArtworkKind.Painting
        PERSONAL -> kind == ArtworkKind.PersonalPhoto
    }

    fun showsPhotoTopics(): Boolean = this == PHOTO

    fun showsMuseumSources(): Boolean = this == PAINTING || this == SCULPTURE
}

enum class MuseumSourceFilter(
    val sourceId: String,
    @get:StringRes val labelRes: Int,
) {
    MET(MetSource.ID, R.string.source_met),
    RIJKSMUSEUM(RijksmuseumSource.ID, R.string.source_rijksmuseum),
    BUNDLED(BundledPackSource.ID, R.string.source_bundled),
}

fun List<Artwork>.filterByCategoryAndSources(
    category: CategoryFilter,
    selectedSourceIds: Set<String>,
): List<Artwork> = filter { art ->
    category.matches(art.kind) &&
        (selectedSourceIds.isEmpty() || art.sourceId in selectedSourceIds)
}

fun Set<String>.toggleSource(sourceId: String): Set<String> =
    if (sourceId in this) this - sourceId else this + sourceId

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
    selectedSourceIds: Set<String>,
    onToggleSource: (String) -> Unit,
    modifier: Modifier = Modifier,
    stockCategory: StockPhotoCategory? = null,
    onStockCategoryChange: ((StockPhotoCategory) -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(horizontal = 20.dp),
    horizontalSpacing: Dp = 8.dp,
) {
    val showTopics = selectedCategory.showsPhotoTopics() &&
        stockCategory != null &&
        onStockCategoryChange != null
    val showSources = selectedCategory.showsMuseumSources()
    Column(modifier = modifier) {
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
            visible = showSources,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            MuseumSourceFilterRow(
                selectedSourceIds = selectedSourceIds,
                onToggleSource = onToggleSource,
                contentPadding = contentPadding,
                horizontalSpacing = horizontalSpacing,
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
private fun MuseumSourceFilterRow(
    selectedSourceIds: Set<String>,
    onToggleSource: (String) -> Unit,
    contentPadding: PaddingValues,
    horizontalSpacing: Dp,
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .testTag("source_filter_row"),
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(horizontalSpacing),
    ) {
        items(MuseumSourceFilter.entries) { source ->
            FilterChip(
                selected = source.sourceId in selectedSourceIds,
                onClick = { onToggleSource(source.sourceId) },
                label = { Text(stringResource(source.labelRes)) },
                modifier = Modifier.testTag("source_chip_${source.sourceId}"),
            )
        }
    }
}

@StringRes
private fun StockPhotoCategory.labelRes(): Int = when (this) {
    StockPhotoCategory.Nature -> R.string.stock_topic_nature
    StockPhotoCategory.City -> R.string.stock_topic_city
    StockPhotoCategory.Ocean -> R.string.stock_topic_ocean
    StockPhotoCategory.Mountains -> R.string.stock_topic_mountains
    StockPhotoCategory.Abstract -> R.string.stock_topic_abstract
    StockPhotoCategory.Architecture -> R.string.stock_topic_architecture
    StockPhotoCategory.Sky -> R.string.stock_topic_sky
}

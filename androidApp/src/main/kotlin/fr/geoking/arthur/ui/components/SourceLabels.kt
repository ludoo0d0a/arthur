package fr.geoking.arthur.ui.components

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import fr.geoking.arthur.R
import fr.geoking.arthur.shared.domain.Artwork
import fr.geoking.arthur.shared.source.ArticSource
import fr.geoking.arthur.shared.source.BundledPackSource
import fr.geoking.arthur.shared.source.ClevelandSource
import fr.geoking.arthur.shared.source.CoverrSource
import fr.geoking.arthur.shared.source.CustomFractalSource
import fr.geoking.arthur.shared.source.EuropeanaSource
import fr.geoking.arthur.shared.source.FractalSource
import fr.geoking.arthur.shared.source.GenartSource
import fr.geoking.arthur.shared.source.HarvardSource
import fr.geoking.arthur.shared.source.LouvreSource
import fr.geoking.arthur.shared.source.MetSource
import fr.geoking.arthur.shared.source.PexelsSource
import fr.geoking.arthur.shared.source.PexelsVideoSource
import fr.geoking.arthur.shared.source.PixabayVideoSource
import fr.geoking.arthur.shared.source.RijksmuseumSource
import fr.geoking.arthur.shared.source.SmithsonianSource
import fr.geoking.arthur.shared.source.UnsplashSource
import fr.geoking.arthur.shared.source.WikimediaStreetArtSource

/** Localized Source label for chrome (Ambient overlay, cards). */
@StringRes
fun sourceLabelRes(sourceId: String): Int? = when (sourceId) {
    MetSource.ID -> R.string.source_met
    RijksmuseumSource.ID -> R.string.source_rijksmuseum
    ArticSource.ID -> R.string.source_artic
    ClevelandSource.ID -> R.string.source_cleveland
    EuropeanaSource.ID -> R.string.source_europeana
    HarvardSource.ID -> R.string.source_harvard
    SmithsonianSource.ID -> R.string.source_smithsonian
    LouvreSource.ID -> R.string.source_louvre
    WikimediaStreetArtSource.ID -> R.string.source_wikimedia_streetart
    BundledPackSource.ID -> R.string.source_bundled
    PexelsSource.ID, PexelsVideoSource.ID -> R.string.source_pexels_video
    PixabayVideoSource.ID -> R.string.source_pixabay_video
    CoverrSource.ID -> R.string.source_coverr
    UnsplashSource.ID -> R.string.source_unsplash
    GenartSource.ID -> R.string.kind_genart
    FractalSource.ID -> R.string.kind_fractal
    CustomFractalSource.ID -> R.string.kind_custom_fractal
    else -> null
}

@Composable
fun Artwork.sourceLabel(): String {
    val res = sourceLabelRes(sourceId)
    return if (res != null) stringResource(res) else sourceId
}

/**
 * Author line for Ambient: [attribution] with a trailing " / Source" suffix removed
 * when it duplicates the Source label.
 */
fun Artwork.authorForDisplay(sourceLabel: String): String {
    val raw = attribution.trim()
    if (raw.isEmpty()) return ""
    val suffixes = buildList {
        add(sourceLabel)
        addAll(KnownAttributionSourceSuffixes)
    }.distinct()
    for (suffix in suffixes) {
        val marker = " / $suffix"
        if (raw.endsWith(marker, ignoreCase = true)) {
            return raw.dropLast(marker.length).trim()
        }
    }
    return raw
}

private val KnownAttributionSourceSuffixes = listOf(
    "Unsplash",
    "Pexels",
    "Pixabay",
    "Coverr",
    "Musée du Louvre",
    "Wikimedia Commons",
    "The Met",
    "Art Institute of Chicago",
    "Rijksmuseum",
    "Arthur Genart",
    "Arthur Fractal Presets",
    "Arthur Custom Fractal",
    "Arthur Bundled Pack",
)

package fr.geoking.arthur.auto

import android.os.Bundle
import androidx.media.utils.MediaConstants
import fr.geoking.arthur.shared.domain.Artwork
import fr.geoking.arthur.shared.source.ArticSource
import fr.geoking.arthur.shared.source.BundledPackSource
import fr.geoking.arthur.shared.source.ClevelandSource
import fr.geoking.arthur.shared.source.CustomFractalSource
import fr.geoking.arthur.shared.source.DeviantArtSource
import fr.geoking.arthur.shared.source.EuropeanaSource
import fr.geoking.arthur.shared.source.FractalSource
import fr.geoking.arthur.shared.source.GenartSource
import fr.geoking.arthur.shared.source.HarvardSource
import fr.geoking.arthur.shared.source.LouvreSource
import fr.geoking.arthur.shared.source.MetSource
import fr.geoking.arthur.shared.source.PexelsSource
import fr.geoking.arthur.shared.source.RijksmuseumSource
import fr.geoking.arthur.shared.source.SmithsonianSource
import fr.geoking.arthur.shared.source.UnsplashSource
import fr.geoking.arthur.shared.source.WikimediaStreetArtSource

/**
 * Auto Media browse helpers: ≤2 levels (root folders → playable art), content-style
 * hints so genart/fractal previews show as a grid of stills (IU-1 / SA-1 safe).
 */
object ArthurMediaBrowse {
    const val ROOT = "arthur_root"
    const val FOLDER_PREFIX = "folder:"

    private val preferredSourceOrder = listOf(
        GenartSource.ID,
        FractalSource.ID,
        CustomFractalSource.ID,
        BundledPackSource.ID,
        RijksmuseumSource.ID,
        MetSource.ID,
        ArticSource.ID,
        ClevelandSource.ID,
        EuropeanaSource.ID,
        HarvardSource.ID,
        SmithsonianSource.ID,
        LouvreSource.ID,
        WikimediaStreetArtSource.ID,
        PexelsSource.ID,
        UnsplashSource.ID,
        DeviantArtSource.ID,
    )

    fun folderId(sourceId: String): String = FOLDER_PREFIX + sourceId

    fun sourceIdFromFolder(parentId: String): String? =
        parentId.takeIf { it.startsWith(FOLDER_PREFIX) }?.removePrefix(FOLDER_PREFIX)

    fun isFolder(mediaId: String): Boolean = mediaId.startsWith(FOLDER_PREFIX)

    /** Root extras: category folders as list; playable artwork as grid (previews). */
    fun rootExtras(): Bundle = Bundle().apply {
        putInt(
            MediaConstants.DESCRIPTION_EXTRAS_KEY_CONTENT_STYLE_BROWSABLE,
            MediaConstants.DESCRIPTION_EXTRAS_VALUE_CONTENT_STYLE_LIST_ITEM,
        )
        putInt(
            MediaConstants.DESCRIPTION_EXTRAS_KEY_CONTENT_STYLE_PLAYABLE,
            MediaConstants.DESCRIPTION_EXTRAS_VALUE_CONTENT_STYLE_GRID_ITEM,
        )
    }

    /** Genart / fractal folders: force grid playables so still previews dominate. */
    fun previewGridExtras(): Bundle = Bundle().apply {
        putInt(
            MediaConstants.DESCRIPTION_EXTRAS_KEY_CONTENT_STYLE_PLAYABLE,
            MediaConstants.DESCRIPTION_EXTRAS_VALUE_CONTENT_STYLE_GRID_ITEM,
        )
        putInt(
            MediaConstants.DESCRIPTION_EXTRAS_KEY_CONTENT_STYLE_BROWSABLE,
            MediaConstants.DESCRIPTION_EXTRAS_VALUE_CONTENT_STYLE_LIST_ITEM,
        )
    }

    fun folderTitle(sourceId: String): String = when (sourceId) {
        GenartSource.ID -> "Genart"
        FractalSource.ID -> "Fractal Presets"
        CustomFractalSource.ID -> "Custom Fractals"
        BundledPackSource.ID -> "Bundled Pack"
        RijksmuseumSource.ID -> "Rijksmuseum"
        MetSource.ID -> "The Met"
        ArticSource.ID -> "Art Institute of Chicago"
        ClevelandSource.ID -> "Cleveland Museum of Art"
        EuropeanaSource.ID -> "Europeana"
        HarvardSource.ID -> "Harvard Art Museums"
        SmithsonianSource.ID -> "Smithsonian"
        LouvreSource.ID -> "Musée du Louvre"
        WikimediaStreetArtSource.ID -> "Wikimedia Street Art"
        PexelsSource.ID -> "Pexels"
        UnsplashSource.ID -> "Unsplash"
        else -> sourceId
    }

    /** Stable source order for root navigation. */
    fun rootSourceIds(catalog: List<Artwork>): List<String> {
        val present = catalog.map { it.sourceId }.toSet()
        return preferredSourceOrder.filter { it in present } +
            present.filterNot { it in preferredSourceOrder }.sorted()
    }

    fun childrenOf(parentId: String, catalog: List<Artwork>): List<Artwork> {
        val sourceId = sourceIdFromFolder(parentId) ?: return emptyList()
        return catalog.filter { it.sourceId == sourceId }
    }

    fun usesPreviewGrid(sourceId: String): Boolean =
        sourceId == GenartSource.ID ||
            sourceId == FractalSource.ID ||
            sourceId == CustomFractalSource.ID
}

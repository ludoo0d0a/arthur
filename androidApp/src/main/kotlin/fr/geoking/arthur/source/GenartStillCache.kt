package fr.geoking.arthur.source

import fr.geoking.arthur.auto.AmbientStillRenderer
import fr.geoking.arthur.shared.domain.Artwork
import fr.geoking.arthur.shared.domain.ArtworkKind
import fr.geoking.arthur.shared.source.GenartSource

/**
 * Bakes genart engines to [ArtworkImageCache] stills (max [ArtworkImageCache.MAX_GENART]).
 */
object GenartStillCache {
    fun warm(cache: ArtworkImageCache, catalog: List<Artwork>): List<Artwork> {
        val genart = catalog
            .filter { it.kind == ArtworkKind.Genart || it.sourceId == GenartSource.ID }
            .distinctBy { it.id }
            .take(ArtworkImageCache.MAX_GENART)
        for (art in genart) {
            if (cache.hasImage(art.id)) {
                cache.touchGenart(art.id)
                continue
            }
            val file = cache.imageFile(art.id)
            runCatching {
                AmbientStillRenderer.renderToFile(art, generation = 0L, file = file)
                if (cache.hasImage(art.id)) {
                    cache.rememberGenart(art)
                }
            }
        }
        return catalog.map { art ->
            if (art.kind != ArtworkKind.Genart && art.sourceId != GenartSource.ID) return@map art
            val path = cache.localPathOrNull(art.id) ?: return@map art
            art.copy(localPath = path)
        }
    }
}

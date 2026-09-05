package fr.geoking.arthur.source

import fr.geoking.arthur.auto.AmbientStillRenderer
import fr.geoking.arthur.shared.domain.Artwork
import fr.geoking.arthur.shared.domain.ArtworkKind
import fr.geoking.arthur.shared.source.GenartSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Bakes genart engines to [ArtworkImageCache] stills (max [ArtworkImageCache.MAX_GENART]).
 * Runs on [Dispatchers.Default] — never call from the main thread for bulk warm.
 */
object GenartStillCache {
    suspend fun warm(cache: ArtworkImageCache, catalog: List<Artwork>): List<Artwork> =
        withContext(Dispatchers.Default) {
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
            catalog.map { art ->
                if (art.kind != ArtworkKind.Genart && art.sourceId != GenartSource.ID) return@map art
                val path = cache.localPathOrNull(art.id) ?: return@map art
                art.copy(localPath = path)
            }
        }
}

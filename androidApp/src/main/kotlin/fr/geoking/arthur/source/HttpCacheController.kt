package fr.geoking.arthur.source

import fr.geoking.tools.debugbar.model.CacheStatRow
import fr.geoking.tools.debugbar.model.CacheStats
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.Cache
import java.net.URI

/** Debug-only control surface for the shared OkHttp HTTP response cache. */
class HttpCacheController(private val cache: Cache) {
    private val _disabled = MutableStateFlow(false)
    val disabled: StateFlow<Boolean> = _disabled.asStateFlow()

    fun setDisabled(value: Boolean) {
        _disabled.value = value
    }

    fun clear() {
        runCatching { cache.evictAll() }
    }

    fun stats(): HttpCacheStats {
        val sizeBytes = runCatching { cache.size() }.getOrDefault(0L)
        val urls = runCatching { cache.urls().asSequence().toList() }.getOrDefault(emptyList())
        val byHost = urls
            .mapNotNull { url -> runCatching { URI(url).host }.getOrNull()?.takeIf { it.isNotBlank() } }
            .groupingBy { it }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .map { (host, count) -> CacheStatRow(label = host, itemCount = count) }
        return HttpCacheStats(
            sizeBytes = sizeBytes,
            maxSizeBytes = cache.maxSize(),
            entryCount = urls.size,
            byHost = byHost,
        )
    }

    fun toDebugBarStats(imageCache: ArtworkImageCache? = null): CacheStats {
        val http = stats()
        val imageBytes = imageCache?.totalBytes() ?: 0L
        val imageCount = imageCache?.entryCount() ?: 0
        val byType = buildList {
            add(
                CacheStatRow(
                    label = "HTTP",
                    sizeBytes = http.sizeBytes,
                    itemCount = http.entryCount,
                )
            )
            if (imageCache != null) {
                add(
                    CacheStatRow(
                        label = "Images",
                        sizeBytes = imageBytes,
                        itemCount = imageCount,
                    )
                )
            }
        }
        return CacheStats(
            totalSizeBytes = http.sizeBytes + imageBytes,
            totalItemCount = http.entryCount + imageCount,
            byType = byType,
            byHost = http.byHost,
        )
    }
}

data class HttpCacheStats(
    val sizeBytes: Long,
    val maxSizeBytes: Long,
    val entryCount: Int = 0,
    val byHost: List<CacheStatRow> = emptyList(),
)

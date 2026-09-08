package fr.geoking.arthur.source

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.Cache

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

    fun stats(): HttpCacheStats = HttpCacheStats(
        sizeBytes = runCatching { cache.size() }.getOrDefault(0L),
        maxSizeBytes = cache.maxSize(),
    )
}

data class HttpCacheStats(val sizeBytes: Long, val maxSizeBytes: Long)

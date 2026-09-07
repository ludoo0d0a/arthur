package fr.geoking.arthur.shared.debug

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class DebugQueryItem(
    val id: Long,
    val sourceId: String,
    val url: String,
    val durationMs: Long,
    val isCached: Boolean,
    val statusCode: Int? = null,
    val timestamp: Long = System.currentTimeMillis(),
)

data class DebugStats(
    val totalQueries: Int = 0,
    val activeQueries: Int = 0,
    val cacheHits: Int = 0,
    val cacheMisses: Int = 0,
    val lastLoadDurationMs: Long = 0L,
    val recentQueries: List<DebugQueryItem> = emptyList(),
)

class DebugLogger(
    private val clock: () -> Long = { System.currentTimeMillis() },
    private val maxRecentQueries: Int = 50,
) {
    private val _stats = MutableStateFlow(DebugStats())
    val stats: StateFlow<DebugStats> = _stats.asStateFlow()

    private var nextId = 1L

    fun recordQueryStart() {
        _stats.update { current ->
            current.copy(activeQueries = current.activeQueries + 1)
        }
    }

    fun recordQueryEnd(
        sourceId: String,
        url: String,
        durationMs: Long,
        isCached: Boolean,
        statusCode: Int? = null,
    ) {
        val now = clock()
        val item = DebugQueryItem(
            id = nextId++,
            sourceId = sourceId,
            url = url,
            durationMs = durationMs,
            isCached = isCached,
            statusCode = statusCode,
            timestamp = now,
        )
        _stats.update { current ->
            val updatedRecent = (listOf(item) + current.recentQueries).take(maxRecentQueries)
            current.copy(
                totalQueries = current.totalQueries + 1,
                activeQueries = (current.activeQueries - 1).coerceAtLeast(0),
                cacheHits = if (isCached) current.cacheHits + 1 else current.cacheHits,
                cacheMisses = if (!isCached) current.cacheMisses + 1 else current.cacheMisses,
                recentQueries = updatedRecent,
            )
        }
    }

    fun recordLoadDuration(durationMs: Long) {
        _stats.update { current ->
            current.copy(lastLoadDurationMs = durationMs)
        }
    }

    fun clear() {
        _stats.value = DebugStats()
    }
}

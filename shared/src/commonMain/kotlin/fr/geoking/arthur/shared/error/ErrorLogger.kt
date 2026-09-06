package fr.geoking.arthur.shared.error

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class ErrorCategory {
    Authentication, // 401, 403, missing API key
    RateLimit,      // 429 Too Many Requests
    Network,        // Timeout, host unreachable, wrong URL, 404
    Payload,        // JSON decoding, serialization, missing fields
    Unknown,        // Other unclassified exceptions
}

data class ErrorItem(
    val id: String,
    val timestamp: Long,
    val sourceId: String,
    val category: ErrorCategory,
    val message: String,
    val details: String? = null,
    val url: String? = null,
    val statusCode: Int? = null,
)

class ErrorLogger(
    private val maxCapacity: Int = 200,
    private val clock: () -> Long = { 0L },
) {
    private val _errors = MutableStateFlow<List<ErrorItem>>(emptyList())
    val errors: StateFlow<List<ErrorItem>> = _errors.asStateFlow()

    fun log(
        sourceId: String,
        category: ErrorCategory,
        message: String,
        details: String? = null,
        url: String? = null,
        statusCode: Int? = null,
        throwable: Throwable? = null,
    ) {
        val extraDetails = if (throwable != null) {
            val causeMsg = throwable.message ?: throwable::class.simpleName ?: "Exception"
            if (details.isNullOrBlank()) causeMsg else "$details\n$causeMsg"
        } else details

        val now = clock()
        val item = ErrorItem(
            id = "${now}_${_errors.value.size}_${(0..9999).random()}",
            timestamp = now,
            sourceId = sourceId,
            category = category,
            message = message,
            details = extraDetails,
            url = url,
            statusCode = statusCode,
        )

        _errors.update { current ->
            (listOf(item) + current).take(maxCapacity)
        }
    }

    fun clearAll() {
        _errors.value = emptyList()
    }
}

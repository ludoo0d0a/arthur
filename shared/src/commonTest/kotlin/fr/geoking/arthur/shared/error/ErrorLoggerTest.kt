package fr.geoking.arthur.shared.error

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ErrorLoggerTest {

    @Test
    fun logsAndClearsErrors() {
        var currentTime = 1000L
        val logger = ErrorLogger(clock = { currentTime })

        assertEquals(0, logger.errors.value.size)

        logger.log(
            sourceId = "europeana",
            category = ErrorCategory.Authentication,
            message = "403 Forbidden",
            url = "https://api.europeana.eu/record/v2/search.json",
            statusCode = 403,
        )

        assertEquals(1, logger.errors.value.size)
        val first = logger.errors.value.first()
        assertEquals("europeana", first.sourceId)
        assertEquals(ErrorCategory.Authentication, first.category)
        assertEquals("403 Forbidden", first.message)
        assertEquals(403, first.statusCode)
        assertEquals(1000L, first.timestamp)

        currentTime = 2000L
        logger.log(
            sourceId = "harvard",
            category = ErrorCategory.RateLimit,
            message = "429 Too Many Requests",
            statusCode = 429,
        )

        assertEquals(2, logger.errors.value.size)
        assertEquals("harvard", logger.errors.value.first().sourceId)

        logger.clearAll()
        assertEquals(0, logger.errors.value.size)
    }

    @Test
    fun classifiesErrorsCorrectly() {
        assertEquals(ErrorCategory.Authentication, ErrorClassifier.classify(403, null))
        assertEquals(ErrorCategory.Authentication, ErrorClassifier.classify(401, null))
        assertEquals(ErrorCategory.RateLimit, ErrorClassifier.classify(429, null))
        assertEquals(ErrorCategory.Network, ErrorClassifier.classify(404, null))
        assertEquals(ErrorCategory.Network, ErrorClassifier.classify(500, null))

        val parseException = kotlinx.serialization.SerializationException("Bad JSON")
        assertEquals(ErrorCategory.Payload, ErrorClassifier.classify(null, parseException))

        val ioException = java.io.IOException("Connection reset")
        assertEquals(ErrorCategory.Network, ErrorClassifier.classify(null, ioException))
    }
}

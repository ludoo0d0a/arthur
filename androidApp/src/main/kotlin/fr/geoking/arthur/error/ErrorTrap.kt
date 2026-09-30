package fr.geoking.arthur.error

import com.google.firebase.crashlytics.FirebaseCrashlytics
import fr.geoking.arthur.shared.error.ErrorCategory
import fr.geoking.arthur.shared.error.ErrorClassifier
import fr.geoking.arthur.shared.error.ErrorItem
import fr.geoking.arthur.shared.error.ErrorLogger
import kotlinx.coroutines.CoroutineExceptionHandler

/**
 * Bridges caught/uncaught failures into the in-app ErrorLogger (Settings → Developer → Errors)
 * and Crashlytics non-fatals. Fatals still go through Crashlytics' own handler.
 */
object ErrorTrap {
    fun install(errorLogger: ErrorLogger) {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching {
                errorLogger.log(
                    sourceId = "crash",
                    category = ErrorClassifier.classify(null, throwable),
                    message = throwable.message ?: "Uncaught on ${thread.name}",
                    details = throwable.stackTraceToString().take(2000),
                    throwable = throwable,
                )
            }
            previous?.uncaughtException(thread, throwable)
                ?: throw throwable
        }
    }

    fun coroutineHandler(errorLogger: ErrorLogger, sourceId: String = "coroutine"): CoroutineExceptionHandler =
        CoroutineExceptionHandler { _, throwable ->
            if (throwable is kotlinx.coroutines.CancellationException) return@CoroutineExceptionHandler
            errorLogger.log(
                sourceId = sourceId,
                category = ErrorClassifier.classify(null, throwable),
                message = throwable.message ?: "Coroutine failure ($sourceId)",
                details = throwable.stackTraceToString().take(2000),
                throwable = throwable,
            )
        }

    fun crashlyticsSink(): (ErrorItem, Throwable?) -> Unit = { item, throwable ->
        runCatching {
            val crashlytics = FirebaseCrashlytics.getInstance()
            crashlytics.setCustomKey("error_source", item.sourceId)
            crashlytics.setCustomKey("error_category", item.category.name)
            item.artworkId?.let { crashlytics.setCustomKey("artwork_id", it) }
            item.statusCode?.let { crashlytics.setCustomKey("status_code", it) }
            item.url?.let { crashlytics.log("url=$it") }
            crashlytics.log("${item.category}: ${item.message}")
            // Non-fatals for recovered runtime failures (skip noisy network/auth logs).
            // Uncaught fatals (sourceId=crash) are already reported by the SDK handler.
            if (
                throwable != null &&
                item.sourceId != "crash" &&
                item.category in setOf(ErrorCategory.Unknown, ErrorCategory.Payload)
            ) {
                crashlytics.recordException(throwable)
            }
        }
    }
}

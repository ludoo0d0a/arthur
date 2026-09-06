package fr.geoking.arthur.shared.error

import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ResponseException
import io.ktor.client.plugins.ServerResponseException
import kotlinx.serialization.SerializationException
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

object ErrorClassifier {
    fun classify(statusCode: Int?, throwable: Throwable?): ErrorCategory {
        if (statusCode != null) {
            when (statusCode) {
                401, 403 -> return ErrorCategory.Authentication
                429 -> return ErrorCategory.RateLimit
                in 400..499 -> return ErrorCategory.Network
                in 500..599 -> return ErrorCategory.Network
            }
        }

        return when (throwable) {
            is ClientRequestException -> {
                when (throwable.response.status.value) {
                    401, 403 -> ErrorCategory.Authentication
                    429 -> ErrorCategory.RateLimit
                    else -> ErrorCategory.Network
                }
            }
            is ServerResponseException -> ErrorCategory.Network
            is ResponseException -> ErrorCategory.Network
            is SerializationException, is IllegalArgumentException -> ErrorCategory.Payload
            is UnknownHostException, is SocketTimeoutException, is ConnectException, is IOException -> ErrorCategory.Network
            else -> ErrorCategory.Unknown
        }
    }
}

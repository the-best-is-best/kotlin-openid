package io.github.openid.ktor_feature

import io.github.openid.AuthOpenId
import io.ktor.client.HttpClient
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.request
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class AppAutInterceptor(private val authOpenId: AuthOpenId, private val httpClient: HttpClient) {

    suspend fun intercept(request: HttpRequestBuilder): HttpResponse {
        try {
            // Obtain the token and add it to the request headers
            val token = getToken()
            request.headers[HttpHeaders.Authorization] = "Bearer $token"

            // Execute the request
            val response = httpClient.request(request)

            // Handle the response normally
            return handleResponse(response, request)

        } catch (e: ClientRequestException) {
            // This handles HTTP 4xx errors such as 401 or 403
            return handleResponse(e.response, request)
        } catch (e: ServerResponseException) {
            // Handle 5xx HTTP errors (e.g., Internal Server Error)
            println("ServerResponseException occurred: ${e.response.status} - ${e.message}")
            throw e
        } catch (e: Exception) {
            // Handle any other exceptions
            println("An unexpected error occurred: ${e.message}")
            throw e
        }
    }


    private suspend fun getToken(): String {
        return suspendCancellableCoroutine { continuation ->
            authOpenId.getLastAuth { result ->
                result.onSuccess { authResult ->
                    val accessToken = authResult?.accessToken
                    if (accessToken != null) {
                        continuation.resume(accessToken)
                    } else {
                        continuation.resumeWithException(IllegalStateException("Access token is null"))
                    }
                }.onFailure { exception ->
                    continuation.resumeWithException(exception)
                }
            }
        }
    }

    private suspend fun handleResponse(
        response: HttpResponse,
        originalRequest: HttpRequestBuilder
    ): HttpResponse {
        return when (response.status) {
            HttpStatusCode.Forbidden, HttpStatusCode.Unauthorized -> {
                val refreshed = refreshToken()
                if (refreshed) {
                    val newToken = getToken()
                    originalRequest.headers[HttpHeaders.Authorization] = "Bearer $newToken"
                    httpClient.request(originalRequest) // Re-execute the original request
                } else {
                    response
                }
            }

            else -> response
        }
    }

    private suspend fun refreshToken(): Boolean {
        return runCatching {
            suspendCancellableCoroutine { continuation ->
                authOpenId.refreshToken { result ->
                    result.onSuccess {
                        continuation.resume(true)
                    }.onFailure {
                        continuation.resume(false)
                    }
                }
            }
        }.isSuccess
    }
}
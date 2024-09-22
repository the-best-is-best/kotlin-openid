package io.github.sample.api

import io.github.openid.AuthOpenId
import io.github.openid.ktor_feature.AppAutInterceptor
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.DEFAULT
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header
import io.ktor.http.path
import io.ktor.http.takeFrom
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

internal abstract class KtorApi {

    val client = HttpClient {
        expectSuccess = true
        defaultRequest {
            header("Content-Type", "application/json")
        }
        install(Logging) {
            logger = Logger.DEFAULT
            level = LogLevel.ALL
        }

        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                    useAlternativeNames = false
                }
            )
        }

    }


    fun HttpRequestBuilder.pathUrl(path: String) {
        url {
            takeFrom("https://demo.duendesoftware.com")
                .path("api", path)
        }
    }

    private val tokenInterceptor = AppAutInterceptor(AuthOpenId(), client)

    suspend fun interceptRequest(request: HttpRequestBuilder) {
        tokenInterceptor.intercept(request)
    }
}
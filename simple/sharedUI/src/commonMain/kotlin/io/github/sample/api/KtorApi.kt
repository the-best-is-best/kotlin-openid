package io.github.sample.api

import io.github.openid.AuthOpenId
import io.github.sample.services.OpenIdService
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.DEFAULT
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.timeout
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header
import io.ktor.http.path
import io.ktor.http.takeFrom
import io.ktor.serialization.kotlinx.json.json
import kotlinx.io.IOException
import kotlinx.serialization.json.Json

internal abstract class KtorApi {
    private val auth = AuthOpenId()
    private val openIdService = OpenIdService()
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
        install(HttpRequestRetry) {
            maxRetries = 3
            retryIf { _, response ->
                response.status.value == 401 || response.status.value in 500..599
            }
            retryOnExceptionIf { _, cause -> cause is IOException }
            delayMillis { retry -> retry * 1000L }
            modifyRequest { request ->
                request.timeout {
                    requestTimeoutMillis = 10000
                }
            }
        }

        install(Auth) {
            bearer {
                loadTokens {
                    try {
                        val res = auth.getLastAuth()
                        var token: BearerTokens? = null
                        res.onSuccess {
                            println("res token ${it?.accessToken}")
                            if (it != null && it.accessToken.isNotBlank()) {
                                token = BearerTokens(it.accessToken, it.refreshToken)
                            }
                        }
                        res.onFailure {
                            println("res token fail $it")
                        }
                        token
                    } catch (e: Exception) {
                        null
                    }
                }
                refreshTokens {
                    var token: BearerTokens? = null
                    val res = auth.refreshToken(openIdService.getTokenRequest())
                    res.onSuccess {
                        if (it.accessToken.isNotBlank()) {
                            println("res token after refresh ${it.accessToken}")
                            token = BearerTokens(it.accessToken, it.refreshToken)
                        }
                    }
                    res.onFailure {
                        println("res token fail $it")
                    }
                    println("refresh token is $token")
                    token
                }
            }
        }


    }


    fun HttpRequestBuilder.pathUrl(path: String) {
        url {
            takeFrom("https://demo.duendesoftware.com")
                .path("api", path)
        }
    }


}
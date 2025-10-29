package io.github.sample.api

import io.ktor.client.request.post
import io.ktor.client.statement.bodyAsText

internal class KtorServices : KtorApi() {
    suspend fun testApi(): String = client.post {
        pathUrl("test")
    }.bodyAsText()
}
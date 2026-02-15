package io.github.kmmopenid.api

import io.ktor.client.request.post
import io.ktor.client.statement.bodyAsText

class KtorServices : KtorApi() {
    suspend fun testApi(): String = client.post {
        pathUrl("test")
    }.bodyAsText()
}
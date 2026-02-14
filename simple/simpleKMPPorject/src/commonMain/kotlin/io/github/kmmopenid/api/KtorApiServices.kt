package io.github.kmmopenid.api

import io.github.tbib.koingeneratorannotations.Factory
import io.ktor.client.request.post
import io.ktor.client.statement.bodyAsText

@Factory
class KtorServices : KtorApi() {
    suspend fun testApi(): String = client.post {
        pathUrl("test")
    }.bodyAsText()
}
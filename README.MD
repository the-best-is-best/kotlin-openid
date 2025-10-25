<h1 align="center">KAppAuth</h1><br>

<div align="center">
<a href="https://opensource.org/licenses/Apache-2.0"><img alt="License" src="https://img.shields.io/badge/License-Apache%202.0-blue.svg"/></a>
<a href="https://android-arsenal.com/api?level=23" rel="nofollow">
    <img alt="API" src="https://img.shields.io/badge/API-23%2B-brightgreen.svg?style=flat" style="max-width: 100%;">
</a>
  <img src="https://img.shields.io/badge/Platform-Android-brightgreen.svg?logo=android" alt="Badge Android" />
  <img src="https://img.shields.io/badge/Platform-iOS%20%2F%20macOS-lightgrey.svg?logo=apple" alt="Badge iOS" />

<a href="https://github.com/the-best-is-best/"><img alt="Profile" src="https://img.shields.io/badge/github-%23181717.svg?&style=for-the-badge&logo=github&logoColor=white" height="20"/></a>
</div>

### This package provides an abstraction around the Android and iOS AppAuth SDKs so it can be used to communicate with OAuth 2.0 and OpenID Connect providers

<hr>

[![Maven Central](https://img.shields.io/maven-central/v/io.github.the-best-is-best/kapp-auth)](https://central.sonatype.com/artifact/io.github.the-best-is-best/kapp-auth)

KApp Auth is available on `mavenCentral()`.

```kotlin
implementation("io.github.the-best-is-best:kapp-auth:2.2.0")

```

### ✅ **Install via SPM**

You can install **AppAuth** directly using **Swift Package Manager (SPM)** from:  
🔗 [AppAuth-iOS on GitHub](https://github.com/openid/AppAuth-iOS)

You need install **IOSCrypto** directly using **Swift Package Manager (SPM)** from:  
🔗 [IOSCryptoManager-iOS on GitHub](https://github.com/the-best-is-best/IOSCrypto)

## How to use it

### First in androidMain

<br>

```kotlin
AndroidKMMCrypto.init("key0") // add alias key name
```

### Note: this package login and saved data so auth or refresh token will return true or false or exception

### Auth

```kotlin
       val authRes = RememberAuthOpenId {
        println("Auth result $it")
        it?.let { isAuthenticated ->
            if (isAuthenticated) {
                scope.launch {

                    auth.getLastAuth { res ->
                        res.onSuccess { it ->
                            accessToken = it!!.accessToken
                            refreshToken = it.refreshToken
                            idToken = it.idToken
                        }
                    }
                    println("Authentication successful!")
                }
            } else {
                println("Authentication failed!")

            }
        }
    }

   ...
   Button(onClick:(){
     authRes.launch()
   }){
    Text("login")
   }
```

### Refresh token

```kotlin
val auth = AuthOpenId()
 try {
    println("Attempting to refresh token")
    auth.refreshToken { result ->
        result.onSuccess { isAuthenticated ->
            if (isAuthenticated) {
                println("Authentication successful!")
                


            } else {
                println("Authentication failed!")
            }
        }.onFailure { error ->
            println("Authentication error: ${error.message}")
        }
    }

} catch (e: Exception) {
    println("Refresh token failed: ${e.message}")
}
```

### Get data

```kotlin
val auth = AuthOpenId()
   auth.getLastAuth(callback = {
    it.onSuccess {
        println("last auth token ${it?.accessToken}")
        if (it != null) {
            accessToken = it.accessToken
            refreshToken = it.refreshToken
            idToken = it.idToken
        }
    }
})
```

### logout

```kotlin
   val authLogout = RememberLogoutOpenId {
        println("Logout result $it")
        it?.let { isLogout ->
            if (isLogout) {
                accessToken = ""
                refreshToken = ""
                idToken = ""

                println("Logout successful!")
            } else {
                println("Logout failed!")

            }


        }
    }
    Button(onClick:(){
        authLogout.launch()
    }){
        Text("Logout")
    }

```

### Finally we add interceptor for get token and refresh token auto in 401 or 403 and re call api ` But there is no test yet. `

```kotlin
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

```

### And

```kotlin
package io.github.sample.api

import io.ktor.client.request.post
import io.ktor.client.statement.bodyAsText

internal class KtorServices : KtorApi() {
    suspend fun testApi(): String = client.post {
        pathUrl("/test")
        interceptRequest(this)
    }.bodyAsText()
}
```

<h1 align="center">KAppAuth</h1><br>

<div align="center">
<a href="https://opensource.org/licenses/Apache-2.0"><img alt="License" src="https://img.shields.io/badge/License-Apache%202.0-blue.svg"/></a>
<a href="https://android-arsenal.com/api?level=21" rel="nofollow">
    <img alt="API" src="https://img.shields.io/badge/API-21%2B-brightgreen.svg?style=flat" style="max-width: 100%;">
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
implementation("io.github.the-best-is-best:kapp-auth:1.0.9")

// Not need add it with v 1.0.9
implementation("io.github.the-best-is-best:kmm-crypto:1.1.4")

```

## 📢 Kapp-Auth 1.0.8 – Now Supports SPM! 🚀

### ✅ **Install via SPM (No Need for CocoaPods)**

Starting from **Kapp-Auth 1.0.8**, you no longer need a `Podfile`.  
You can install **AppAuth** directly using **Swift Package Manager (SPM)** from:  
🔗 [AppAuth-iOS on GitHub](https://github.com/openid/AppAuth-iOS)

### ⚠️ **For Versions Before 1.0.8 (e.g., 1.0.7 and earlier)**

If you are using an **older version** of `Kapp-Auth` (before 1.0.8), you still need to add **AppAuth
** to your `Podfile`.

If you don’t have a `Podfile`, create one using:

```sh
pod init
// not need it in kmm-crypto:1.1.4
// pod 'KServices' , '0.2.2'
pod 'AppAuth' , '1.7.6'
```

## How to use it

### First in androidMain

<br>

```kotlin
AndroidKMMCrypto.init(this, "key0") // add alias key name
AndroidOpenId.init(this)
```

### Note: this package login and saved data so auth or refresh token will return true or false or exception

### Auth

```kotlin
    val auth = AuthOpenId()
  try {

    println("Attempting to login")
    auth.auth { result ->
        result.onSuccess { isAuthenticated ->
            if (isAuthenticated) {
                println("Authentication successful!")
            }
        }.onFailure { error ->
            println("Authentication error: ${error.message}")
        }
    }
} catch (e: Exception) {
    println("Login failed: ${e.message}")
}
```

### Refresh token

```kotlin

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
   auth.logout { result ->
        result.onSuccess {
        
        }

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

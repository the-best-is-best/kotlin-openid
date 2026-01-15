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
implementation("io.github.the-best-is-best:kapp-auth:6.0.0")

```

### ✅ **Install via SPM**

You need install **IOSCrypto** directly using **Swift Package Manager (SPM)** from:  
🔗 [AppAuthInterop-iOS on GitHub](https://github.com/the-best-is-best/AppAuthInterop)

### Note: v5 dep on v3.0.0

You can install **AppAuth** directly using **Swift Package Manager (SPM)** from:  
🔗 [AppAuth-iOS on GitHub](https://github.com/openid/AppAuth-iOS)

You need install **IOSCrypto** directly using **Swift Package Manager (SPM)** from:  
🔗 [IOSCryptoManager-iOS on GitHub](https://github.com/the-best-is-best/IOSCryptoInterop)

## How to use it

### First in androidMain

<br>

```kotlin
AndroidKMMCrypto.init("key0") // add alias key name
```

### Second in commonMain

#### 1 -

```kotlin
 val auth = AuthOpenId()
 LaunchedEffect(Unit) {
        auth.init("key", "service")
    }
```

#### 2 -

```kotlin

import io.github.openid.AuthorizationRequest
import io.github.openid.AuthorizationServiceConfig
import io.github.openid.TokenRequest

class OpenIdService {


    fun getAuthorizationRequest(): AuthorizationRequest {
        return AuthorizationRequest(
            issuer = "https://demo.duendesoftware.com",
            discoveryUrl = ".well-known/openid-configuration",
            scope = listOf("openid", "profile", "offline_access", "email"),
            clientId = "interactive.public",
            redirectUrl = "com.duendesoftware.demo:/oauthredirect",
            authorizationServiceConfiguration = AuthorizationServiceConfig(
                authorizationEndpoint = "connect/authorize",
                tokenEndpoint = "connect/token",
                endSessionEndpoint = "connect/endsession",
                postLogoutRedirectURL = "com.duendesoftware.demo:/postlogout",
                registerEndPoint = null
            ),
        )
    }


    fun getTokenRequest(): TokenRequest {
        return TokenRequest(
            issuer = "https://demo.duendesoftware.com",
            tokenEndpoint = "connect/token",
            clientId = "interactive.public",
        )
    }

}
```

### Note: this package login and saved data so auth or refresh token will return true or false or exception

### Auth

```kotlin
       val authRes = RememberAuthOpenId(
            authorizationRequest = openIdServices.getAuthorizationRequest()
        ) {
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
 try {
    println("Attempting to refresh token")
     val result =
        auth.refreshToken(openIdServices.getTokenRequest())
        result.onSuccess { isAuthenticated ->
            if (isAuthenticated) {
                println("Authentication successful!")
                
            } else {
                println("Authentication failed!")
            }
        }.onFailure { error ->
            println("Authentication error: ${error.message}")
        }
    
    } catch (e: Exception) {
        println("Refresh token failed: ${e.message}")
    }
```

### Get data

```kotlin
     val res = auth.getLastAuth()
        res.onSuccess { result ->
            accessToken = result!!.accessToken
            refreshToken = result.refreshToken
            idToken = result.idToken
        }

```

### logout

```kotlin
   val authLogout = RememberLogoutOpenId(
            openIdServices.getAuthorizationRequest()
        ) {
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

```

### And

```kotlin
import io.ktor.client.request.post
import io.ktor.client.statement.bodyAsText

internal class KtorServices : KtorApi() {
    suspend fun testApi(): String = client.post {
        pathUrl("/test")
        interceptRequest(this)
    }.bodyAsText()
}
```

// iosMain/src/AuthOpenId.kt
package io.github.openid

import cocoapods.AppAuth.OIDAuthState
import cocoapods.AppAuth.OIDAuthorizationRequest
import cocoapods.AppAuth.OIDAuthorizationService
import cocoapods.AppAuth.OIDEndSessionRequest
import cocoapods.AppAuth.OIDExternalUserAgentIOS
import cocoapods.AppAuth.OIDResponseTypeCode
import cocoapods.AppAuth.OIDServiceConfiguration
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSData
import platform.Foundation.NSKeyedArchiver
import platform.Foundation.NSKeyedUnarchiver
import platform.Foundation.NSURL
import platform.Foundation.NSUserDefaults
import platform.UIKit.UIApplication
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException


@OptIn(ExperimentalForeignApi::class)
actual class AuthOpenId {
    private var authState: OIDAuthState? = null

    private val suiteName = "group.net.openid.appauth.Example"
    private val key = "kAppAuthExampleAuthStateKey"


    init {
        authState = loadState()
    }

    actual suspend fun auth(): Boolean? = suspendCancellableCoroutine { continuation ->
        val authRequest = createAuthRequest()
        val viewController = UIApplication.sharedApplication.keyWindow?.rootViewController

        if (viewController == null) {
            continuation.resumeWithException(Exception("No root view controller available"))
            return@suspendCancellableCoroutine
        }

        val externalUserAgent = OIDExternalUserAgentIOS(
            presentingViewController = viewController
        )

        println("Starting authentication request...")
        OIDAuthState.authStateByPresentingAuthorizationRequest(
            authorizationRequest = authRequest,
            externalUserAgent = externalUserAgent,
            callback = { authState, error ->
                if (authState != null) {
                    this.authState = authState
                    saveState(authState)
                    val accessToken = authState.lastTokenResponse?.accessToken ?: ""
                    val refreshToken = authState.lastTokenResponse?.refreshToken ?: ""
                    val idToken = authState.lastTokenResponse?.idToken ?: ""  // Extract ID token
                    println("Authentication successful: Access Token: $accessToken, Refresh Token: $refreshToken, ID Token: $idToken")
                    continuation.resume(true)
                } else {
                    println("Authorization error: ${error?.localizedDescription}")
                    continuation.resumeWithException(Exception("Authorization failed: ${error?.localizedDescription}"))
                }
            }
        )

        continuation.invokeOnCancellation {
            println("Authorization flow was cancelled")
        }
    }


    actual suspend fun refreshToken(refreshToken: String): Boolean? =
        suspendCancellableCoroutine { continuation ->
            val authState = this.authState ?: run {
                continuation.resumeWithException(Exception("No auth state available"))
                return@suspendCancellableCoroutine
            }


            authState.performActionWithFreshTokens { _, _, error ->
                if (error == null) {
                    saveState(authState)
                    continuation.resume(true)
                } else {
                    println("Token refresh error: ${error.localizedDescription}")
                    continuation.resumeWithException(Exception("Token refresh failed: ${error.localizedDescription}"))
                }
            }

            continuation.invokeOnCancellation {
                println("Token refresh flow was cancelled")
            }
        }


    actual suspend fun logout(idToken: String): Boolean? =
        suspendCancellableCoroutine { continuation ->

            val authConfig = getAuthConfig()

            val endSessionRequest = OIDEndSessionRequest(
                configuration = authConfig,
                idTokenHint = idToken,

                postLogoutRedirectURL = NSURL(string = OpenIdConfig.postLogoutRedirectURL),
                additionalParameters = null
            )

            // Present the logout request in a web view (or default browser)
            val viewController = UIApplication.sharedApplication.keyWindow!!.rootViewController!!

            val externalUserAgent = OIDExternalUserAgentIOS(
                presentingViewController = viewController
            )

            OIDAuthorizationService.presentEndSessionRequest(
                endSessionRequest,
                externalUserAgent,
                callback = { endSessionResponse, error ->
                    if (endSessionResponse != null) {
                        authState = null
                        // Handle successful logout
                        continuation.resume(null) // Return null or any specific result if needed
                    } else {
                        println("Logout error: ${error?.localizedDescription}")
                        continuation.resumeWithException(Exception("Logout failed: ${error?.localizedDescription}"))
                    }
                }
            )

            continuation.invokeOnCancellation {
                println("Logout flow was cancelled")
            }
        }

    private fun createAuthRequest(): OIDAuthorizationRequest {
        val authConfig = getAuthConfig()
        val clientId = OpenIdConfig.clientId
        val scopesList: List<String> = OpenIdConfig.scope.split(" ")
        val redirectUrl = NSURL(string = OpenIdConfig.redirectUrl)

        return OIDAuthorizationRequest(
            configuration = authConfig,
            clientId = clientId,
            clientSecret = null,
            scopes = scopesList,
            redirectURL = redirectUrl,
            responseType = OIDResponseTypeCode!!,
            additionalParameters = null
        )
    }

    private fun getAuthConfig(): OIDServiceConfiguration = OIDServiceConfiguration(
        authorizationEndpoint = NSURL(string = OpenIdConfig.authEndPoint),
        tokenEndpoint = NSURL(string = OpenIdConfig.tokenEndPoint),
        null,
        null,
        NSURL(string = OpenIdConfig.endSessionEndPoint),


        )


    private fun saveState(authState: OIDAuthState?) {
        val data = authState?.let {
            NSKeyedArchiver.archivedDataWithRootObject(it)
        }

        val userDefaults = NSUserDefaults(suiteName)
        if (data != null) {

            userDefaults.setObject(data, key)
        } else {
            userDefaults.removeObjectForKey(key)
        }
        println("data saved is $data")
        userDefaults.synchronize()
    }

    private fun loadState(): OIDAuthState? {
        val userDefaults = NSUserDefaults(suiteName)
        val data = userDefaults.objectForKey(key) as? NSData
        println("Data retrieved: $data")

        return try {
            data?.let {
                NSKeyedUnarchiver.unarchiveObjectWithData(it) as? OIDAuthState
            }
        } catch (e: Exception) {
            println("Error during unarchiving: ${e.message}")
            null
        }
    }


    actual fun getLastAuth(): AuthResult? {
        if (authState != null) {
            val lastTokenResponse = authState!!.lastTokenResponse
            if (lastTokenResponse != null) {
                return AuthResult(
                    accessToken = lastTokenResponse.accessToken!!,
                    refreshToken = lastTokenResponse.refreshToken!!,
                    idToken = lastTokenResponse.idToken!!,
                )
            }
        }
        println("authState is null")
        return null
    }

}





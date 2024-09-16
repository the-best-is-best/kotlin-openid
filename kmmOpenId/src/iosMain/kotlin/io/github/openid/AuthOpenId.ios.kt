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
import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException


@OptIn(ExperimentalForeignApi::class)
actual class AuthOpenId {
    private var authState: OIDAuthState? = null

    actual suspend fun auth(): AuthResult? = suspendCancellableCoroutine { continuation ->
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
                    val accessToken = authState.lastTokenResponse?.accessToken ?: ""
                    val refreshToken = authState.lastTokenResponse?.refreshToken ?: ""
                    val idToken = authState.lastTokenResponse?.idToken ?: ""  // Extract ID token

                    println("Authentication successful: Access Token: $accessToken, Refresh Token: $refreshToken, ID Token: $idToken")
                    continuation.resume(AuthResult(accessToken, refreshToken, idToken))
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


    actual suspend fun refreshToken(refreshToken: String): AuthResult? =
        suspendCancellableCoroutine { continuation ->
            val authState = this.authState ?: run {
                continuation.resumeWithException(Exception("No auth state available"))
                return@suspendCancellableCoroutine
            }

            authState.performActionWithFreshTokens { accessToken, idToken, error ->
                if (error == null) {
                    val newAccessToken = accessToken ?: ""
                    val newIdToken = idToken ?: ""
                    val newRefreshToken = authState.lastTokenResponse?.refreshToken
                        ?: ""  // Retrieve new refresh token
                    continuation.resume(AuthResult(newAccessToken, newRefreshToken, newIdToken))
                } else {
                    println("Token refresh error: ${error.localizedDescription}")
                    continuation.resumeWithException(Exception("Token refresh failed: ${error.localizedDescription}"))
                }
            }

            continuation.invokeOnCancellation {
                println("Token refresh flow was cancelled")
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

    private fun getAuthConfig() = OIDServiceConfiguration(
        authorizationEndpoint = NSURL(string = OpenIdConfig.authEndPoint),
        tokenEndpoint = NSURL(string = OpenIdConfig.tokenEndPoint),
        null,
        null,
        NSURL(string = OpenIdConfig.endSessionEndPoint),

        )


    actual suspend fun logout(idToken: String): AuthResult? =
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

}





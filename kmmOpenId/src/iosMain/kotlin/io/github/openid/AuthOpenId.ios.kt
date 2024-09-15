// iosMain/src/AuthOpenId.kt
package io.github.openid

import cocoapods.AppAuth.OIDAuthState
import cocoapods.AppAuth.OIDAuthorizationRequest
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
        val viewController = UIApplication.sharedApplication.keyWindow!!.rootViewController!!

        val externalUserAgent = OIDExternalUserAgentIOS(
            presentingViewController = viewController
        )
        OIDAuthState.authStateByPresentingAuthorizationRequest(
            authorizationRequest = authRequest,
            externalUserAgent = externalUserAgent,
            callback = { authState, error ->
                if (authState != null) {
                    this.authState = authState
                    val accessToken = authState.lastTokenResponse?.accessToken ?: ""
                    val refreshToken = authState.lastTokenResponse?.refreshToken ?: ""
                    continuation.resume(AuthResult(accessToken, refreshToken))
                } else {
                    println("Authorization error: ${error?.localizedDescription}")
                    continuation.resumeWithException(Exception("Authorization failed: ${error?.localizedDescription}"))
                }
            }
        )

        continuation.invokeOnCancellation {
            // Handle cancellation if needed
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
                    val newRefreshToken = idToken ?: ""
                    continuation.resume(AuthResult(newAccessToken, newRefreshToken))
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
        val authConfig = OIDServiceConfiguration(
            authorizationEndpoint = NSURL(string = OpenIdConfig.authEndPoint),
            tokenEndpoint = NSURL(string = OpenIdConfig.tokenEndPoint)
        )
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
}





package io.github.openid

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import io.github.appauth.OIDAuthState
import io.github.appauth.OIDAuthorizationRequest
import io.github.appauth.OIDExternalUserAgentIOS
import io.github.appauth.OIDExternalUserAgentSessionProtocol
import io.github.appauth.OIDResponseTypeCode
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSURL
import platform.UIKit.UIApplication


@Composable
actual fun RememberAuthOpenId(onAuthResult: (Boolean?) -> Unit): AuthOpenIdState {
    return AuthOpenIdState(
        authLauncher = remember {
            onAuthResult
        }
    )
}

@OptIn(ExperimentalForeignApi::class)
actual class AuthOpenIdState actual constructor(authLauncher: Any) {
    private var currentSession: OIDExternalUserAgentSessionProtocol? = null
    val onAuthResult = (authLauncher as (Boolean?) -> Unit)

    @OptIn(ExperimentalForeignApi::class)
    actual suspend fun launch() {

        val authRequest = createAuthRequest()
        val viewController = UIApplication.sharedApplication.keyWindow?.rootViewController

        if (viewController == null) {
            onAuthResult(false)
            return@launch
        }

        val externalUserAgent = OIDExternalUserAgentIOS(
            presentingViewController = viewController
        )

        currentSession = OIDAuthState.authStateByPresentingAuthorizationRequest(
            authorizationRequest = authRequest,
            externalUserAgent = externalUserAgent,
            callback = { authState, _ ->
                if (authState != null) {

                    val accessToken = authState.lastTokenResponse?.accessToken ?: ""
                    val refreshToken = authState.lastTokenResponse?.refreshToken ?: ""
                    val idToken =
                        authState.lastTokenResponse?.idToken ?: ""  // Extract ID token
                    println("Authentication successful: Access Token: $accessToken, Refresh Token: $refreshToken, ID Token: $idToken")
                    AuthOpenId().saveState(authState)
                    onAuthResult(true)
                } else {
                    onAuthResult(false)
                }
            }

        )

    }

    @OptIn(ExperimentalForeignApi::class)
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

}
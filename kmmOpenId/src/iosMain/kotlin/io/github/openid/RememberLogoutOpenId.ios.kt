package io.github.openid

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import io.github.appauth.OIDAuthorizationService
import io.github.appauth.OIDEndSessionRequest
import io.github.appauth.OIDExternalUserAgentIOS
import io.github.appauth.OIDExternalUserAgentSessionProtocol
import io.github.kmmcrypto.KMMCrypto
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSURL
import platform.UIKit.UIApplication

@Composable
actual fun RememberLogoutOpenId(onLogoutResult: (Boolean?) -> Unit): LogoutOpenIdState {
    return LogoutOpenIdState(
        logoutLauncher = remember {
            onLogoutResult
        }
    )
}

@OptIn(ExperimentalForeignApi::class)
actual class LogoutOpenIdState actual constructor(logoutLauncher: Any) {
    private var currentSession: OIDExternalUserAgentSessionProtocol? = null
    val callback = (logoutLauncher as (Boolean?) -> Unit)


    @OptIn(ExperimentalForeignApi::class)
    actual suspend fun launch() {
        val authConfig = getAuthConfig()

        val idToken = AuthOpenId().loadState()?.lastTokenResponse?.idToken
        if (idToken == null) {
            callback(false)
            return@launch
        }

        val endSessionRequest = OIDEndSessionRequest(
            configuration = authConfig,
            idTokenHint = idToken,
            postLogoutRedirectURL = NSURL(string = OpenIdConfig.postLogoutRedirectURL),
            additionalParameters = null
        )

        // Present the logout request in a web view (or default browser)
        val viewController = UIApplication.sharedApplication.keyWindow?.rootViewController
        if (viewController == null) {
            callback(false)
            return@launch
        }

        val externalUserAgent = OIDExternalUserAgentIOS(
            presentingViewController = viewController
        )

        currentSession = OIDAuthorizationService.presentEndSessionRequest(
            endSessionRequest,
            externalUserAgent,
            callback = { endSessionResponse, error ->
                if (endSessionResponse != null) {
                    // Handle successful logout
                    KMMCrypto().deleteData(AuthOpenId.service, AuthOpenId.group)
                    callback(true) // Return null or any specific result if needed
                } else {
                    callback(false)
                }
            }
        )
    }
}
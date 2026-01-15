package io.github.openid

import android.annotation.SuppressLint
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.core.net.toUri
import io.github.kmmcrypto.KMMCrypto
import io.github.openid.AuthOpenId.Companion.group
import io.github.openid.AuthOpenId.Companion.key
import net.openid.appauth.AuthorizationService
import net.openid.appauth.EndSessionRequest

@SuppressLint("ComposableNaming")
@Composable
actual fun RememberLogoutOpenId(
    endSessionRequest: AuthorizationRequest,
    onLogoutResult: (Boolean?) -> Unit
): LogoutOpenIdState {
    val kmmCrypto = KMMCrypto()

    val logoutLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val isSuccess = AndroidOpenId().handleLogoutResult(result) // Handle logout result
            if (isSuccess) {
                kmmCrypto.deleteData(key, group)

            }
            onLogoutResult(isSuccess)
        }
    return remember { LogoutOpenIdState(endSessionRequest, logoutLauncher) }
}

actual class LogoutOpenIdState actual constructor(
    private val authorizationRequest: AuthorizationRequest,
    logoutLauncher: Any
) {
    private val logoutLauncher = logoutLauncher as ActivityResultLauncher<Intent>

    private val authService = AuthorizationService(applicationContext)


    actual suspend fun launch() {
        val result = AuthOpenId().getLastAuth()
        println("launching logout ${authorizationRequest.authorizationServiceConfiguration.postLogoutRedirectURL!!.toUri()}")
        result.onSuccess { authData ->
            val idToken = authData?.idToken ?: ""
            if (idToken.isEmpty()) return@onSuccess

            val serviceConfig = getAuthServicesConfig(
                authorizationRequest.issuer,
                authorizationRequest.authorizationServiceConfiguration
            )
            val endSessionRequest = EndSessionRequest.Builder(serviceConfig)
                .setIdTokenHint(idToken)
                .setPostLogoutRedirectUri(authorizationRequest.authorizationServiceConfiguration.postLogoutRedirectURL.toUri())
                .build()

            val endSessionIntent = authService.getEndSessionRequestIntent(
                endSessionRequest,
                CustomTabsIntent.Builder()
                    .setShowTitle(false)
                    .setUrlBarHidingEnabled(true)
                    .build()
                    .apply {
                        intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
            )

            logoutLauncher.launch(endSessionIntent)
        }.onFailure {
            // handle failure if needed
        }
    }

}

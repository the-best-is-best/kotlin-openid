package io.github.openid

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

@Composable
actual fun RememberLogoutOpenId(onLogoutResult: (Boolean?) -> Unit): LogoutOpenIdState {
    val kmmCrypto = KMMCrypto()

    val logoutLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val isSuccess = AndroidOpenId().handleLogoutResult(result) // Handle logout result
            if (isSuccess) {
                kmmCrypto.deleteData(key, group)

            }
            onLogoutResult(isSuccess)
        }
    return remember { LogoutOpenIdState(logoutLauncher) }
}

actual class LogoutOpenIdState actual constructor(logoutLauncher: Any) {
    private val logoutLauncher = logoutLauncher as ActivityResultLauncher<Intent>

    private val authService = AuthorizationService(applicationContext)


    actual suspend fun launch() {
        AuthOpenId().getLastAuth { result ->
            result.onSuccess { authData ->
                val idToken = authData?.idToken ?: ""
                if (idToken.isEmpty()) {
                    return@onSuccess
                }

                val serviceConfig = getAuthServicesConfig()
                val endSessionRequest = EndSessionRequest.Builder(serviceConfig)
                    .setIdTokenHint(idToken)
                    .setPostLogoutRedirectUri(OpenIdConfig.postLogoutRedirectURL.toUri())
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
            }
        }
    }
}

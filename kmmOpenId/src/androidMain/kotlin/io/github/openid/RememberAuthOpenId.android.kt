package io.github.openid

import android.annotation.SuppressLint
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.core.net.toUri
import kotlinx.coroutines.launch
import net.openid.appauth.AuthorizationRequest
import net.openid.appauth.AuthorizationService
import net.openid.appauth.ResponseTypeValues

@SuppressLint("ComposableNaming")
@Composable
actual fun RememberAuthOpenId(
    authorizationRequest: io.github.openid.AuthorizationRequest,
    onAuthResult: (Boolean?) -> Unit
): AuthOpenIdState {
    val scope = rememberCoroutineScope()
    val authLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            scope.launch {
                val isSuccess =
                    AndroidOpenId().handleAuthResult(result) // Check if login was successful
                onAuthResult(isSuccess != null) // Pass result to callback
            }


        }

    // Return the state object with the launcher
    return remember { AuthOpenIdState(authorizationRequest, authLauncher) }
}


actual class AuthOpenIdState actual constructor(
    private val authorizationRequest: io.github.openid.AuthorizationRequest,
    private val authLauncher: Any
) {
    private val authService = AuthorizationService(applicationContext)
    actual suspend fun launch() {

        // Configure OpenID service
        val serviceConfig = getAuthServicesConfig(
            authorizationRequest.issuer,
            authorizationRequest.authorizationServiceConfiguration
        )

        // Build authorization request
        val authRequest = AuthorizationRequest.Builder(
            serviceConfig,
            authorizationRequest.clientId,
            ResponseTypeValues.CODE,
            authorizationRequest.redirectUrl.toUri()
        )
            .setScopes(authorizationRequest.scope)
            .build()

        // Create intent with CustomTabs
        val authRequestIntent = authService.getAuthorizationRequestIntent(
            authRequest,
            CustomTabsIntent.Builder()
                .setShowTitle(false)
                .setUrlBarHidingEnabled(true)
                .build()
                .apply {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
        )

        // Launch the authentication flow
        (authLauncher as ActivityResultLauncher<Intent>).launch(authRequestIntent)

    }
}
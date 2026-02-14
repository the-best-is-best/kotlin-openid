package io.github.kmmopenid.kappauthcmp

import androidx.compose.runtime.Composable
import io.github.openid.AuthorizationRequest

@Composable
expect fun RememberLogoutOpenId(
    authorizationRequest: AuthorizationRequest,
    onLogoutResult: (Boolean?) -> Unit
): LogoutOpenIdState

expect class LogoutOpenIdState(authorizationRequest: AuthorizationRequest, logoutLauncher: Any) {
    suspend fun launch()
}

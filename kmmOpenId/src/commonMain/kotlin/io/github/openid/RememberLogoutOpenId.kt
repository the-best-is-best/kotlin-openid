package io.github.openid

import androidx.compose.runtime.Composable

@Composable
expect fun RememberLogoutOpenId(
    endSessionRequest: AuthorizationRequest,
    onLogoutResult: (Boolean?) -> Unit
): LogoutOpenIdState

expect class LogoutOpenIdState(authorizationRequest: AuthorizationRequest, logoutLauncher: Any) {
    suspend fun launch()
}

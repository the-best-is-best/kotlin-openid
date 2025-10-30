package io.github.openid

import androidx.compose.runtime.Composable

@Composable
expect fun RememberLogoutOpenId(onLogoutResult: (Boolean?) -> Unit): LogoutOpenIdState
expect class LogoutOpenIdState(logoutLauncher: Any) {
    suspend fun launch(auth: AuthOpenId)
}

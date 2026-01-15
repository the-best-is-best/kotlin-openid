package io.github.openid

import androidx.compose.runtime.Composable

@Composable
expect fun RememberAuthOpenId(
    authorizationRequest: AuthorizationRequest,
    onAuthResult: (Boolean?) -> Unit
): AuthOpenIdState

expect class AuthOpenIdState(authorizationRequest: AuthorizationRequest, authLauncher: Any) {
    suspend fun launch()
}

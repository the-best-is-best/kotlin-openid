package io.github.kmmopenid.kappauthcmp

import androidx.compose.runtime.Composable
import io.github.openid.AuthorizationRequest

@Composable
expect fun RememberAuthOpenId(
    authorizationRequest: AuthorizationRequest,
    onAuthResult: (Boolean?) -> Unit
): AuthOpenIdState

expect class AuthOpenIdState(authorizationRequest: AuthorizationRequest, authLauncher: Any) {
    suspend fun launch()
}

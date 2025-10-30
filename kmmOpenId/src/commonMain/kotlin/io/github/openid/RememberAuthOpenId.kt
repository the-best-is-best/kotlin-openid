package io.github.openid

import androidx.compose.runtime.Composable

@Composable
expect fun RememberAuthOpenId(onAuthResult: (Boolean?) -> Unit): AuthOpenIdState
expect class AuthOpenIdState(authLauncher: Any) {
    suspend fun launch(auth: AuthOpenId)
}

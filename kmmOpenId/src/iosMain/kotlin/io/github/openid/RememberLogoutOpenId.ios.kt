package io.github.openid

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
actual fun RememberLogoutOpenId(onLogoutResult: (Boolean?) -> Unit): LogoutOpenIdState {
    return LogoutOpenIdState(
        authorizationRequest = remember {
            onLogoutResult
        }
    )
}

actual class LogoutOpenIdState actual constructor(private val authorizationRequest: Any) {
    @Suppress("UNCHECKED_CAST")
    actual suspend fun launch(auth: AuthOpenId) {
        val res = auth.logout()
        res.onSuccess {
            (authorizationRequest as (Boolean?) -> Unit)(true)
        }.onFailure {
            (authorizationRequest as (Boolean?) -> Unit)(false)
        }
    }
}
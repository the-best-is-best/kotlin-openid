package io.github.openid

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
actual fun RememberLogoutOpenId(onLogoutResult: (Boolean?) -> Unit): LogoutOpenIdState {
    return LogoutOpenIdState(
        logoutLauncher = remember {
            onLogoutResult
        }
    )
}

actual class LogoutOpenIdState actual constructor(private val logoutLauncher: Any) {
    @Suppress("UNCHECKED_CAST")
    actual suspend fun launch(auth: AuthOpenId) {
        val res = auth.logout()
        res.onSuccess {
            (logoutLauncher as (Boolean?) -> Unit)(true)
        }.onFailure {
            (logoutLauncher as (Boolean?) -> Unit)(false)
        }
    }
}
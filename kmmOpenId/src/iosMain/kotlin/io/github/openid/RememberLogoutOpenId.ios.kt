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

actual class LogoutOpenIdState actual constructor(logoutLauncher: Any) {
    private val callback = (logoutLauncher as (Boolean?) -> Unit)

    actual suspend fun launch(auth: AuthOpenId) {
        val res = auth.logout()
        println(res)

        callback(res)
    }
}
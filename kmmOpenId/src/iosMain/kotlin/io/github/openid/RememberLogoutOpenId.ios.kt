package io.github.openid

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.cinterop.ExperimentalForeignApi

@Composable
actual fun RememberLogoutOpenId(onLogoutResult: (Boolean?) -> Unit): LogoutOpenIdState {
    return LogoutOpenIdState(
        logoutLauncher = remember {
            onLogoutResult
        }
    )
}

@OptIn(ExperimentalForeignApi::class)
actual class LogoutOpenIdState actual constructor(logoutLauncher: Any) {
    private val callback = (logoutLauncher as (Boolean?) -> Unit)


    @OptIn(ExperimentalForeignApi::class)
    actual suspend fun launch() {
        AuthOpenId().login(callback)
    }
}
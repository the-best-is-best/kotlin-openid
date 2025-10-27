package io.github.openid

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember


@Composable
actual fun RememberAuthOpenId(onAuthResult: (Boolean?) -> Unit): AuthOpenIdState {
    return AuthOpenIdState(
        authLauncher = remember {
            onAuthResult
        }
    )
}

actual class AuthOpenIdState actual constructor(authLauncher: Any) {
    private val onAuthResult = (authLauncher as (Boolean?) -> Unit)
    actual suspend fun launch() {
        AuthOpenId().login().onSuccess {
            onAuthResult(true)
        }.onFailure {
            onAuthResult(false)
        }

    }



}
package io.github.openid

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
actual fun RememberAuthOpenId(onAuthResult: (Boolean?) -> Unit): AuthOpenIdState {

    return remember {
        AuthOpenIdState(onAuthResult)
    }
}

@Suppress("UNCHECKED_CAST")
actual class AuthOpenIdState actual constructor(private val authLauncher: Any) {
    actual suspend fun launch(auth: AuthOpenId) {
        val res = auth.login()
        res.onSuccess {
            (authLauncher as (Boolean?) -> Unit)(true)
        }.onFailure {
            (authLauncher as (Boolean?) -> Unit)(false)
        }
    }
}
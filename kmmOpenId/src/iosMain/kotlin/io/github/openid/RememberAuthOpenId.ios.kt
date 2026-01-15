package io.github.openid

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
actual fun RememberAuthOpenId(
    authorizationRequest: AuthorizationRequest,
    onAuthResult: (Boolean?) -> Unit
): AuthOpenIdState {

    return remember {
        AuthOpenIdState(authorizationRequest, onAuthResult)
    }
}

@Suppress("UNCHECKED_CAST")
actual class AuthOpenIdState actual constructor(
    val authorizationRequest: AuthorizationRequest,
    val authLauncher: Any
) {
    actual suspend fun launch() {
        val res = AuthOpenId().login(authorizationRequest)
        res.onSuccess {
            (authLauncher as (Boolean?) -> Unit)(true)
        }.onFailure {
            (authLauncher as (Boolean?) -> Unit)(false)
        }
    }
}
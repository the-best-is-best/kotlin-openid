package io.github.kmmopenid.kappauthcmp

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import io.github.openid.AuthOpenId
import io.github.openid.AuthorizationRequest

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
    private val authorizationRequest: AuthorizationRequest,
    private val authLauncher: Any
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
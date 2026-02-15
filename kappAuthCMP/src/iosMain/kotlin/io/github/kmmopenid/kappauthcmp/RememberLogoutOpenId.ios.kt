package io.github.kmmopenid.kappauthcmp

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import io.github.openid.AuthOpenId
import io.github.openid.AuthorizationRequest

@Composable
actual fun RememberLogoutOpenId(
    authorizationRequest: AuthorizationRequest,
    onLogoutResult: (Boolean?) -> Unit
): LogoutOpenIdState {
    return remember {
        LogoutOpenIdState(authorizationRequest, onLogoutResult)
    }

}

actual class LogoutOpenIdState actual constructor
    (private val authorizationRequest: AuthorizationRequest, private val logoutLauncher: Any) {
    @Suppress("UNCHECKED_CAST")
    actual suspend fun launch() {
        val res = AuthOpenId().logout(authorizationRequest)
        res.onSuccess {
            (logoutLauncher as (Boolean?) -> Unit)(true)
        }.onFailure {
            (logoutLauncher as (Boolean?) -> Unit)(false)
        }
    }
}
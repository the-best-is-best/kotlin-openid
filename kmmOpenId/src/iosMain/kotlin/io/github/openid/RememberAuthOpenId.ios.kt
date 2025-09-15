package io.github.openid

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.cinterop.ExperimentalForeignApi


@Composable
actual fun RememberAuthOpenId(onAuthResult: (Boolean?) -> Unit): AuthOpenIdState {
    return AuthOpenIdState(
        authLauncher = remember {
            onAuthResult
        }
    )
}

@OptIn(ExperimentalForeignApi::class)
actual class AuthOpenIdState actual constructor(authLauncher: Any) {
    private val onAuthResult = (authLauncher as (Boolean?) -> Unit)

    @OptIn(ExperimentalForeignApi::class)
    actual suspend fun launch() {

        AuthOpenId().login(onAuthResult)

    }



}
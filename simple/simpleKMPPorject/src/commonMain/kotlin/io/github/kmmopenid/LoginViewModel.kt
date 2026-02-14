package io.github.kmmopenid

import androidx.lifecycle.ViewModel
import io.github.openid.AuthResult
import kotlinx.coroutines.flow.StateFlow

sealed interface AuthEvent {
    object StartOpenIdLogin : AuthEvent
    object StartOpenIdLogout : AuthEvent
}

sealed interface AuthState {
    data class LoginSuccess(val authResult: AuthResult) : AuthState
    data class LoginFailed(val message: String) : AuthState
    object LogoutSuccess : AuthState
    data class LogoutFailed(val message: String) : AuthState
}

expect class LoginViewModel : ViewModel {
    val userInfo: StateFlow<AuthResult?>

    fun login()
    fun logout()

    fun refreshToken()
    fun getUserData()

    fun getApiCall()
}


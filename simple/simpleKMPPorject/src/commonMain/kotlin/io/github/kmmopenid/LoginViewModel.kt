package io.github.kmmopenid

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.kmmopenid.api.KtorServices
import io.github.openid.AuthOpenId
import io.github.openid.AuthResult
import io.github.openid.TokenRequest
import io.github.tbib.koingeneratorannotations.KoinViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

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

@KoinViewModel
class LoginViewModel(private val ktorServices: KtorServices) : ViewModel() {
    private val _events = MutableSharedFlow<AuthEvent>()
    val events = _events.asSharedFlow()

    private val _states = MutableSharedFlow<AuthState>()
    val states = _states.asSharedFlow()

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn = _isLoggedIn.asStateFlow()

    private val authOpenId = AuthOpenId()

    fun login() {
        viewModelScope.launch {
            _events.emit(AuthEvent.StartOpenIdLogin)
        }
    }

    fun logout() {
        viewModelScope.launch {
            _events.emit(AuthEvent.StartOpenIdLogout)
        }
    }

    fun onLoginResult(success: Boolean?) {
        if (success == true) {
            getUserData()
        } else {
            viewModelScope.launch {
                _states.emit(AuthState.LoginFailed("Login failed or was cancelled by user."))
            }
        }
    }

    fun onLogoutResult(success: Boolean?) {
        viewModelScope.launch {
            // WE DO NOT CARE if the server-side call failed.
            // We clear local state so the app isn't stuck.
            _isLoggedIn.value = false
            _states.emit(AuthState.LogoutSuccess)

            if (success != true) {
                println("Server logout returned an error, but local session was destroyed.")
            }
        }
    }

    fun getUserData() {
        viewModelScope.launch {
            authOpenId.getLastAuth().onSuccess { authResult ->
                if (authResult != null) {
                    _isLoggedIn.value = true
                    _states.emit(AuthState.LoginSuccess(authResult))
                } else {
                    _isLoggedIn.value = false
                    _states.emit(AuthState.LoginFailed("Could not retrieve auth session."))
                }
            }.onFailure {
                _isLoggedIn.value = false
                _states.emit(AuthState.LoginFailed(it.message ?: "Unknown error"))
            }
        }
    }

    fun refreshToken(tokenRequest: TokenRequest) {
        viewModelScope.launch {
            val res = authOpenId.refreshToken(tokenRequest = tokenRequest)
            res.onSuccess {
                _states.emit(AuthState.LoginSuccess(it))
            }.onFailure {
                _states.emit(AuthState.LoginFailed(it.message ?: "Unknown error"))
            }
        }
    }

    fun getApiCall() {
        viewModelScope.launch {
            try {
                val result = ktorServices.testApi()
                println("data api is $result")
            } catch (e: Exception) {
                println("error is ${e.message}")
            }
        }
    }

    init {
        getUserData()
    }
}
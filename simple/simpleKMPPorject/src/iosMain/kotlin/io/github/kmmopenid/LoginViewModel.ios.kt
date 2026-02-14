package io.github.kmmopenid

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.kmmopenid.api.KtorServices
import io.github.kmmopenid.services.OpenIdService
import io.github.openid.AuthOpenId
import io.github.openid.AuthResult
import io.github.tbib.koingeneratorannotations.KoinViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch


@KoinViewModel
actual class LoginViewModel(
    private val ktorServices: KtorServices,

    ) : ViewModel() {
    private val auth = AuthOpenId()

    private val _userInfo = MutableStateFlow<AuthResult?>(null)
    actual val userInfo = _userInfo.asStateFlow()

    actual fun login() {
        viewModelScope.launch {
            val res = auth.login(OpenIdService().getAuthorizationRequest())
            res.onSuccess {
                _userInfo.value = it

            }.onFailure {
                _userInfo.value = null
            }
        }
    }

    actual fun logout() {
        viewModelScope.launch {
            val res = auth.logout(OpenIdService().getAuthorizationRequest())
            res.onSuccess {
                _userInfo.value = null
            }
        }
    }

    actual fun refreshToken() {
        viewModelScope.launch {
            val res = auth.refreshToken(OpenIdService().getTokenRequest())
            res.onSuccess {
                _userInfo.value = it
            }
        }
    }

    actual fun getUserData() {
        viewModelScope.launch {
            val res = auth.getLastAuth()
            res.onSuccess {
                _userInfo.value = it
            }.onFailure {
            }
        }
    }

    actual fun getApiCall() {
        viewModelScope.launch {
            viewModelScope.launch {
                try {
                    ktorServices.testApi()
                } catch (e: Exception) {
                    println("$e")
                }
            }
        }

    }
}
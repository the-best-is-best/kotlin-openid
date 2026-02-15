package io.github.kmmopenid

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.kmmopenid.api.KtorServices
import io.github.kmmopenid.services.OpenIdService
import io.github.openid.AndroidOpenId
import io.github.openid.AuthOpenId
import io.github.openid.AuthResult
import io.github.tbib.koingeneratorannotations.Single
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface Event {
    data class LaunchLogin(val intent: Intent) : Event
    data class LaunchLogout(val intent: Intent) : Event
}

@Single
actual class LoginViewModel(
    private val ktorServices: KtorServices,
    private val androidOpenId: AndroidOpenId,
) : ViewModel() {
    private val auth = AuthOpenId()


    private val _userInfo = MutableStateFlow<AuthResult?>(null)
    actual val userInfo = _userInfo.asStateFlow()

    // Shared Flow to tell the Activity to launch the browser
    private val _event = MutableSharedFlow<Event>()
    val event = _event.asSharedFlow()

    actual fun login() {
        viewModelScope.launch {

            val getAuthRequest = OpenIdService().getAuthorizationRequest()
            val intent = auth.getLoginIntent(getAuthRequest)
            _event.emit(Event.LaunchLogin(intent))
        }

    }


    actual fun logout() {
        viewModelScope.launch {
            // 1. Prepare the request
            val getAuthRequest = OpenIdService().getAuthorizationRequest()
            val intent = auth.getLoginIntent(getAuthRequest)
            // 3. Emit a new Event type that carries the Intent
            _event.emit(Event.LaunchLogout(intent))
        }
    }

    actual fun getUserData() {
        viewModelScope.launch {
            auth.getLastAuth().onSuccess {
                _userInfo.value = it

            }
        }
    }

    init {
        getUserData()
    }

    actual fun refreshToken() {
        viewModelScope.launch {
            auth.refreshToken(OpenIdService().getTokenRequest()).onSuccess {
                _userInfo.value = it
            }

        }
    }

    actual fun getApiCall() {
        viewModelScope.launch {
            try {
                val res = ktorServices.testApi()
                println("api call $res")
            } catch (e: Exception) {
                println("$e")
            }
        }
    }

    fun completeLogout() {
        auth.deleteLastAuth()
        _userInfo.value = null

    }
}

package io.github.kmmopenid

import androidx.lifecycle.ViewModel
import io.github.openid.AuthResult
import kotlinx.coroutines.flow.StateFlow


expect class LoginViewModel : ViewModel {
    val userInfo: StateFlow<AuthResult?>

    fun login()
    fun logout()

    fun refreshToken()
    fun getUserData()

    fun getApiCall()
}


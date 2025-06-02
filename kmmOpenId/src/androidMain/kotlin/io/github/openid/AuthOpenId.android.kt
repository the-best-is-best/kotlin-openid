package io.github.openid

import android.util.Log
import com.google.gson.Gson
import io.github.kmmcrypto.KMMCrypto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import net.openid.appauth.AuthorizationService
import net.openid.appauth.GrantTypeValues
import net.openid.appauth.TokenRequest
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException


actual class AuthOpenId {
    private val authService = AuthorizationService(applicationContext)

    companion object {
        lateinit var key: String
        lateinit var group: String
    }

    actual fun init(key: String, group: String) {
        AuthOpenId.key = key
        AuthOpenId.group = group
    }


    actual suspend fun refreshToken(callback: (Result<Boolean>) -> Unit) {
        getLastAuth { data ->
            data.onSuccess { authData ->
                val refreshToken = authData?.refreshToken ?: ""
                if (refreshToken.isEmpty()) {
                    callback(Result.failure(Exception("Refresh token is null or empty")))
                    return@onSuccess // Early return if the refresh token is empty
                }

                val serviceConfig = getAuthServicesConfig()

                val tokenRequest = TokenRequest.Builder(
                    serviceConfig,
                    OpenIdConfig.clientId
                ).setGrantType(GrantTypeValues.REFRESH_TOKEN)
                    .setRefreshToken(refreshToken)
                    .build()

                CoroutineScope(Dispatchers.Main + SupervisorJob()).launch {
                    try {
                        val result = suspendCancellableCoroutine<Boolean?> { cont ->
                            authService.performTokenRequest(tokenRequest) { tokenResponse, exception ->
                                if (tokenResponse != null) {
                                    saveData(
                                        AuthResult(
                                            accessToken = tokenResponse.accessToken!!,
                                            refreshToken = tokenResponse.refreshToken!!,
                                            idToken = tokenResponse.idToken!!
                                        )
                                    )

                                    cont.resume(true)
                                } else {
                                    exception?.let {
                                        Log.e(
                                            "TokenRequest",
                                            "Error refreshing token: ${it.message}"
                                        )
                                        cont.resumeWithException(it)
                                    } ?: run {
                                        Log.e(
                                            "TokenRequest",
                                            "Unknown error occurred during token refresh"
                                        )
                                        cont.resume(false)
                                    }
                                }
                            }
                        }

                        if (result == true) {
                            callback(Result.success(true))
                        } else {
                            callback(Result.failure(Exception("Token refresh failed")))
                        }
                    } catch (e: Exception) {
                        Log.e("TokenRequest", "Exception during token refresh: ${e.message}")
                        callback(Result.failure(e))
                    }
                }
            }.onFailure { exception ->
                callback(Result.failure(Exception("Failed to get last auth: ${exception.message}")))
            }
        }
    }


    actual suspend fun getLastAuth(callback: (Result<AuthResult?>) -> Unit) {
        try {
            CoroutineScope(Dispatchers.Main + SupervisorJob()).launch {

                val dataString = KMMCrypto().loadData(key, group)

                val authResult = Gson().fromJson(dataString, AuthResult::class.java)

                callback(
                    Result.success(authResult)
                )
            }

        } catch (e: Exception) {
            callback(Result.failure(Exception("No data saved")))
        }
    }

    private fun saveData(data: AuthResult?) {
        val kmmCrypto = KMMCrypto()
        if (data != null) {
            val jsonString = Gson().toJson(data)

            kmmCrypto.saveData(key, group, jsonString)
        } else {
            kmmCrypto.saveData(key, group, "")
        }
    }
}

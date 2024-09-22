package io.github.openid

import android.net.Uri
import android.util.Log
import io.github.openid.AndroidOpenId.authLauncher
import io.github.openid.AndroidOpenId.authService
import io.github.openid.AndroidOpenId.continuation
import io.github.openid.AndroidOpenId.kmmCrypto
import io.github.openid.AndroidOpenId.saveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import net.openid.appauth.AuthorizationRequest
import net.openid.appauth.AuthorizationServiceConfiguration
import net.openid.appauth.EndSessionRequest
import net.openid.appauth.GrantTypeValues
import net.openid.appauth.ResponseTypeValues
import net.openid.appauth.TokenRequest
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException


actual class AuthOpenId {
    companion object {
        lateinit var key: String
        lateinit var group: String
    }

    actual fun init(key: String, group: String) {
        AuthOpenId.key = key
        AuthOpenId.group = group
    }

    actual fun auth(callback: (Result<Boolean>) -> Unit) {
        val serviceConfig = getAuthServicesConfig()

        val authRequest = AuthorizationRequest.Builder(
            serviceConfig,
            OpenIdConfig.clientId,
            ResponseTypeValues.CODE,
            Uri.parse(OpenIdConfig.redirectUrl)
        )
            .setScopes(OpenIdConfig.scope)

            .build()

        val authIntent = authService.getAuthorizationRequestIntent(authRequest)

        CoroutineScope(Dispatchers.Main + SupervisorJob()).launch {
            suspendCancellableCoroutine { cont ->
                // Launch the intent
                authLauncher.launch(authIntent)
                // Store the continuation for the result
                continuation = cont
            }.let { result ->
                callback(Result.success(result ?: false))
            }
        }
    }


    actual fun refreshToken(callback: (Result<Boolean>) -> Unit) {
        getLastAuth { result ->
            result.onSuccess { authData ->
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


    actual fun logout(callback: (Result<Boolean?>) -> Unit) {
        getLastAuth { result ->
            result.onSuccess { authData ->
                val idToken = authData?.idToken ?: ""
                if (idToken.isEmpty()) {
                    callback(Result.failure(Exception("ID token is null")))
                    return@onSuccess
                }

                val serviceConfig = getAuthServicesConfig()
                val endSessionRequest = EndSessionRequest.Builder(serviceConfig)
                    .setIdTokenHint(idToken)
                    .setPostLogoutRedirectUri(Uri.parse(OpenIdConfig.postLogoutRedirectURL))
                    .build()

                val endSessionIntent = authService.getEndSessionRequestIntent(endSessionRequest)

                // Launch the intent and handle the result in a callback
                CoroutineScope(Dispatchers.Main + SupervisorJob()).launch {
                    val isSuccess = suspendCancellableCoroutine<Boolean?> { cont ->
                        // Set the continuation to the launch result
                        authLauncher.launch(endSessionIntent)
                        continuation = cont
                    }

                    if (isSuccess == true) {
                        callback(Result.success(true))
                    } else {
                        callback(Result.failure(Exception("Logout failed")))
                    }
                }
            }.onFailure { exception ->
                callback(Result.failure(Exception("Failed to get last auth: ${exception.message}")))
            }
        }
    }


    private fun getAuthServicesConfig(): AuthorizationServiceConfiguration {
        return AuthorizationServiceConfiguration(
            Uri.parse(OpenIdConfig.authEndPoint),
            Uri.parse(OpenIdConfig.tokenEndPoint),
            if (OpenIdConfig.registerEndPoint == null) null else Uri.parse(OpenIdConfig.registerEndPoint),
            Uri.parse(OpenIdConfig.endSessionEndPoint),

            )
    }

    actual fun getLastAuth(callback: (Result<AuthResult?>) -> Unit) {
        try {
            CoroutineScope(Dispatchers.Main + SupervisorJob()).launch {

                val dataString = kmmCrypto.loadData(key, group)

                val authResult = AndroidOpenId.gson.fromJson(dataString, AuthResult::class.java)

                callback(
                    Result.success(authResult)
                )
            }

        } catch (e: Exception) {
            callback(Result.failure(Exception("No data saved")))
        }
    }


}

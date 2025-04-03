package io.github.openid

import android.util.Log
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.net.toUri
import io.github.openid.AndroidOpenId.activity
import io.github.openid.AndroidOpenId.authLauncher
import io.github.openid.AndroidOpenId.continuation
import io.github.openid.AndroidOpenId.kmmCrypto
import io.github.openid.AndroidOpenId.logoutLauncher
import io.github.openid.AndroidOpenId.saveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import net.openid.appauth.AuthorizationRequest
import net.openid.appauth.AuthorizationService
import net.openid.appauth.AuthorizationServiceConfiguration
import net.openid.appauth.EndSessionRequest
import net.openid.appauth.GrantTypeValues
import net.openid.appauth.ResponseTypeValues
import net.openid.appauth.TokenRequest
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException


actual class AuthOpenId {
    private val authService = AuthorizationService(activity.get()!!)

    companion object {
        lateinit var key: String
        lateinit var group: String
    }

    actual fun init(key: String, group: String) {
        AuthOpenId.key = key
        AuthOpenId.group = group
    }

    actual fun auth(callback: (Result<Boolean?>) -> Unit) {

        val serviceConfig = getAuthServicesConfig()

        val authRequest = AuthorizationRequest.Builder(
            serviceConfig,
            OpenIdConfig.clientId,
            ResponseTypeValues.CODE,
            OpenIdConfig.redirectUrl.toUri()
        )
            .setScopes(OpenIdConfig.scope)
            .build()

        val authRequestIntent = authService.getAuthorizationRequestIntent(
            authRequest,
            CustomTabsIntent.Builder()
                .setShowTitle(false)
                .setUrlBarHidingEnabled(true)
                .build()
                .apply {
                    intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NO_HISTORY) // يجعل المتصفح يُغلق عند العودة للتطبيق
                    intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                }
        )
        CoroutineScope(Dispatchers.Main + SupervisorJob()).launch {
            val result = suspendCancellableCoroutine { cont ->
                continuation = cont
                authLauncher.launch(authRequestIntent)

                // Handle cancellation
                cont.invokeOnCancellation {
                    if (!cont.isCompleted) {
                        cont.resume(false)
                    }
                }
            }

            callback(Result.success(result))
        }
    }


    actual fun refreshToken(callback: (Result<Boolean>) -> Unit) {
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
                    .setPostLogoutRedirectUri(OpenIdConfig.postLogoutRedirectURL.toUri())
                    .build()

                val endSessionIntent =  authService .getEndSessionRequestIntent(
                    endSessionRequest,
                    CustomTabsIntent.Builder()
                        .setShowTitle(false)
                        .setUrlBarHidingEnabled(true)
                        .build()
                        .apply {
                            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NO_HISTORY) // يجعل المتصفح يُغلق عند العودة للتطبيق
                            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                )

                // Launch the intent and handle the result in a callback
                CoroutineScope(Dispatchers.Main + SupervisorJob()).launch {
                    val isSuccess = suspendCancellableCoroutine { cont ->
                        // Set the continuation to the launch result
                        logoutLauncher.launch(endSessionIntent)
                        continuation = cont

                        cont.invokeOnCancellation {
                            if (!continuation.isCompleted) {
                                continuation.resume(false)
                            }
                        }

                    }
                    if (isSuccess == true) {
                        kmmCrypto.deleteData(key, group)
                    }
                    callback(Result.success(isSuccess))


                }
            }.onFailure { exception ->
                callback(Result.failure(Exception("Failed to get last auth: ${exception.message}")))
            }
        }
    }


    private fun getAuthServicesConfig(): AuthorizationServiceConfiguration {
        return AuthorizationServiceConfiguration(
            OpenIdConfig.authEndPoint.toUri(),
            OpenIdConfig.tokenEndPoint.toUri(),
            if (OpenIdConfig.registerEndPoint == null) null else OpenIdConfig.registerEndPoint?.toUri(),
            OpenIdConfig.endSessionEndPoint.toUri(),

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

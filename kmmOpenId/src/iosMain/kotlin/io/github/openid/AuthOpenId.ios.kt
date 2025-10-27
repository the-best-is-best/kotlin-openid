package io.github.openid

import io.native.appauth.KAuthManager
import io.native.appauth.KOpenIdConfig
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

@OptIn(ExperimentalForeignApi::class)
actual class AuthOpenId {

    private val auth = KAuthManager.shared()

    actual fun init(key: String, group: String) {
        KOpenIdConfig.shared().configureWithDiscoveryUrl(
            OpenIdConfig.issuerUrl,
            OpenIdConfig.clientId,
            OpenIdConfig.redirectUrl,
            OpenIdConfig.scope,
            OpenIdConfig.postLogoutRedirectURL
        )
        auth.initCryptoWithService(key, group)

    }

    actual suspend fun refreshToken(): Result<AuthResult> = try {
        suspendCancellableCoroutine { cont ->
            auth.refreshAccessToken { authTokens, error ->
                if (authTokens != null) {
                    cont.resume(
                        Result.success(
                            AuthResult(
                                accessToken = authTokens.accessToken()!!,
                                refreshToken = authTokens.refreshToken()!!,
                                idToken = authTokens.idToken()!!
                            )
                        )
                    )
                } else if (error != null) {
                    cont.resume(Result.failure(Exception(error)))
                } else {
                    cont.resume(Result.failure(Exception("Unknown error refreshing token")))
                }
            }
        }
    } catch (e: Exception) {
        Result.failure(e)
    }



    actual suspend fun getLastAuth(): Result<AuthResult?> {
        return try {
            val userAuthInfo = auth.getAuthTokens()

            if (userAuthInfo != null) {
                Result.success(
                    AuthResult(
                        accessToken = userAuthInfo.accessToken() ?: "",
                        refreshToken = userAuthInfo.refreshToken() ?: "",
                        idToken = userAuthInfo.idToken() ?: ""
                    )
                )
            } else {
                Result.failure(Exception("No token response"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun login(): Result<AuthResult> = try {
        suspendCancellableCoroutine { cont ->
            auth.login { authTokens, error ->
                MainScope().launch {

                    if (authTokens != null) {
                        cont.resume(
                            Result.success(
                                AuthResult(
                                    accessToken = authTokens.accessToken()!!,
                                    refreshToken = authTokens.refreshToken()!!,
                                    idToken = authTokens.idToken()!!
                                )
                            )
                        )
                    } else if (error != null) {
                        cont.resume(Result.failure(Exception(error)))
                    } else {
                        cont.resume(Result.failure(Exception("Unknown error during login")))
                    }
                }
            }
        }
    } catch (e: Exception) {
        Result.failure(e)
    }


    suspend fun logout(): Boolean? = suspendCancellableCoroutine { cont ->
        try {
            auth.logout { res, error ->
                MainScope().launch {
                    println("Logout callback called: res=$res, error=$error")
                    if (!cont.isCompleted) {
                        if (error != null) cont.resume(false)
                        else cont.resume(res)
                    }
                }
            }
        } catch (e: Exception) {
            if (!cont.isCompleted) cont.resume(false)
        }
    }

}

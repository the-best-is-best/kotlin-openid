package io.github.openid

import io.github.app_auth_interop.KAuthManager
import io.github.app_auth_interop.KOpenIdConfig
import io.github.appauth.OIDAuthState
import io.github.kmmcrypto.KMMCrypto
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSKeyedUnarchiver
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@OptIn(ExperimentalForeignApi::class)
actual class AuthOpenId {
    private val authInterop = KAuthManager.shared()
    companion object {
        internal lateinit var service: String
        internal lateinit var group: String
    }

    private val crypto = KMMCrypto()

    actual fun init(key: String, group: String) {
        service = key
        AuthOpenId.group = group
        val client = KOpenIdConfig(
            OpenIdConfig.issuer,
            OpenIdConfig.clientId,
            OpenIdConfig.redirectUrl,
            OpenIdConfig.scope,
            OpenIdConfig.postLogoutRedirectURL

        )
        authInterop.initCryptoWithService(service, group, client)
    }

    actual suspend fun refreshToken(): Result<AuthResult> = suspendCancellableCoroutine { cont ->
        try {

            authInterop.refreshAccessToken { authTokens, error ->
                if (error != null) {
                    cont.resumeWithException(Exception(error))
                    return@refreshAccessToken
                }
                val accessToken = authTokens?.accessToken() ?: ""
                val refreshToken = authTokens?.refreshToken() ?: ""
                val idToken = authTokens?.idToken() ?: ""

                val authResult = AuthResult(
                    accessToken = accessToken,
                    refreshToken = refreshToken,
                    idToken = idToken
                )

                cont.resume(Result.success(authResult))


            }
        } catch (e: Exception) {
            cont.resume(Result.failure(e))
        }
    }

    actual suspend fun getLastAuth(): Result<AuthResult?> {
        return try {
            val state = loadState()
            val token = state?.lastTokenResponse

            if (token != null) {
                Result.success(
                    AuthResult(
                        accessToken = token.accessToken ?: "",
                        refreshToken = token.refreshToken ?: "",
                        idToken = token.idToken ?: ""
                    )
                )
            } else {
                Result.failure(Exception("No token response"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun loadState(): OIDAuthState? {
        return try {
            val data = crypto.loadDataType(service, group)
            data?.let {
                NSKeyedUnarchiver.unarchiveObjectWithData(it) as? OIDAuthState
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun login(): Result<Boolean> = suspendCancellableCoroutine { cont ->
        authInterop.login { res, error ->
            if (error != null) {
                cont.resume(Result.failure(Exception(error)))
                return@login
            } else {
                val accessToken = res?.accessToken() ?: ""
                val refreshToken = res?.refreshToken() ?: ""
                val idToken = res?.idToken() ?: ""

                println("Authentication successful: Access Token: $accessToken, Refresh Token: $refreshToken, ID Token: $idToken")

                try {
                    cont.resume(Result.success(true))
                } catch (e: Exception) {
                    cont.resume(Result.failure(Exception("Failed to save auth state: ${e.message}")))
                }
            }
        }
    }

    suspend fun logout(): Result<Boolean> = suspendCancellableCoroutine { cont ->
        authInterop.logout { res, error ->
            if (error != null) {
                cont.resume(Result.failure(Exception(error)))
                return@logout

            }
            try {
                cont.resume(Result.success(true))
            } catch (e: Exception) {
                cont.resume(Result.failure(Exception("Failed to save auth state: ${e.message}")))
            }
        }

    }

}
package io.github.openid

import io.github.app_auth_interop.KAuthManager
import io.github.app_auth_interop.KOpenIdConfig
import io.github.appauth.OIDAuthState
import io.github.appauth.OIDAuthorizationService
import io.github.kmmcrypto.KMMCrypto
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSKeyedArchiver
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

    actual suspend fun refreshToken(): Result<AuthResult> {
        return try {
            val authState = loadState() ?: return Result.failure(Exception("Auth state missing"))
            val refreshRequest = authState.tokenRefreshRequest()
                ?: return Result.failure(Exception("Refresh request is null"))

            val tokenResponse = suspendCancellableCoroutine { cont ->
                OIDAuthorizationService.performTokenRequest(
                    request = refreshRequest,
                    callback = { response, error ->
                        if (response != null) cont.resume(response)
                        else cont.resumeWithException(
                            error?.let { Exception("Token refresh failed: ${it.localizedDescription}") }
                                ?: Exception("Unknown error")
                        )
                    }
                )
            }

            authState.updateWithTokenResponse(tokenResponse, null)
            saveState(authState)

            val newAuthResult = AuthResult(
                accessToken = tokenResponse.accessToken ?: "",
                refreshToken = tokenResponse.refreshToken ?: "",
                idToken = tokenResponse.idToken ?: ""
            )

            Result.success(newAuthResult)

        } catch (e: Exception) {
            Result.failure(e)
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

    private fun saveState(authState: OIDAuthState?) {
        try {
            val data = authState?.let {
                NSKeyedArchiver.archivedDataWithRootObject(it)
            } ?: throw IllegalStateException("Data is null")
            crypto.saveDataType(service, group, data)
        } catch (e: Exception) {
            throw Exception(e.message)
        }
    }

    private suspend fun loadState(): OIDAuthState? {
        return try {
            val data = crypto.loadDataType(service, group)

            data?.let {
                val auth = NSKeyedUnarchiver.unarchiveObjectWithData(it) as? OIDAuthState
                println("refresh token : ${auth?.lastTokenResponse?.refreshToken}")
                auth
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
                println("Logout failed: $error")
                cont.resume(Result.failure(Exception(error)))
                return@logout

            }
            try {
                cont.resume(Result.success(true))
            } catch (e: Exception) {
                println("Failed to save auth state: ${e.message}")
                cont.resume(Result.failure(Exception("Failed to save auth state: ${e.message}")))
            }
        }

    }
//
//    @OptIn(ExperimentalForeignApi::class)
//    private fun createAuthRequest(): OIDAuthorizationRequest {
//        val authConfig = getAuthConfig()
//        val clientId = OpenIdConfig.clientId
//        val scopesList: List<String> = OpenIdConfig.scope.split(" ")
//        val redirectUrl = NSURL(string = OpenIdConfig.redirectUrl)
//
//        return OIDAuthorizationRequest(
//            configuration = authConfig,
//            clientId = clientId,
//            clientSecret = null,
//            scopes = scopesList,
//            redirectURL = redirectUrl,
//            responseType = OIDResponseTypeCode!!,
//            additionalParameters = null
//        )
//    }
}
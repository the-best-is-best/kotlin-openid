package io.github.openid

import io.github.appauth.OIDAuthState
import io.github.appauth.OIDAuthorizationService
import io.github.appauth.OIDTokenResponse
import io.github.kmmcrypto.KMMCrypto
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSKeyedArchiver
import platform.Foundation.NSKeyedUnarchiver
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@OptIn(ExperimentalForeignApi::class)
actual class AuthOpenId {

    companion object {
        internal lateinit var service: String
        internal lateinit var group: String
    }

    private val crypto = KMMCrypto()

    actual fun init(key: String, group: String) {
        service = key
        AuthOpenId.group = group
    }

    actual suspend fun refreshToken(): Result<AuthResult> {
        return try {
            val authState = loadState() ?: return Result.failure(Exception("Auth state missing"))

            val refreshRequest = authState.tokenRefreshRequest()
                ?: return Result.failure(Exception("Refresh request is null"))

            val tokenResponse = suspendCancellableCoroutine<OIDTokenResponse> { cont ->
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


    internal fun saveState(authState: OIDAuthState?) {
        try {
            val data = authState?.let {
                NSKeyedArchiver.archivedDataWithRootObject(it)
            } ?: throw IllegalStateException("Data is null")
            crypto.saveDataType(service, group, data)
        } catch (e: Exception) {
            throw Exception(e.message)
        }
    }
    internal suspend fun loadState(): OIDAuthState? {
        return try {
            val data = crypto.loadDataType(service, group)
            data?.let {
                NSKeyedUnarchiver.unarchiveObjectWithData(it) as? OIDAuthState
            }
        } catch (e: Exception) {
            null
        }
    }
}

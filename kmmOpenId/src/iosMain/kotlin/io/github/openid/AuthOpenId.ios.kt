// iosMain/src/AuthOpenId.kt
package io.github.openid


import io.github.appauth.OIDAuthState
import io.github.appauth.OIDAuthorizationService
import io.github.kmmcrypto.KMMCrypto
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import platform.Foundation.NSKeyedArchiver
import platform.Foundation.NSKeyedUnarchiver

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

    actual fun refreshToken(callback: (Result<Boolean>) -> Unit) {
        CoroutineScope(Dispatchers.Main + SupervisorJob()).launch {
            val authState = loadState()

            if (authState == null) {
                callback(Result.failure(Exception("No auth state available")))
                return@launch
            }

            val refreshRequest = authState.tokenRefreshRequest()
            if (refreshRequest == null) {
                callback(Result.failure(Exception("No refresh token available")))
                return@launch
            }

            // ✅ استدعاء static method مباشرة من OIDAuthorizationService
            OIDAuthorizationService.performTokenRequest(
                request = refreshRequest,
                callback = { response, error ->

                    if (error == null && response != null) {
                        // ✅ استدعاء الطريقة الصحيحة لتحديث الحالة
                        authState.updateWithTokenResponse(response, null)
                        saveState(authState)
                        callback(Result.success(true))
                    } else {
                        callback(Result.failure(Exception("Token refresh failed: ${error?.localizedDescription ?: "Unknown error"}")))
                    }
                }
            )
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

    actual fun getLastAuth(callback: (Result<AuthResult?>) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val state = loadState()
                if (state != null) {
                    val token = state.lastTokenResponse
                    if (token != null) {
                        callback(
                            Result.success(
                                AuthResult(
                                    accessToken = token.accessToken ?: "",
                                    refreshToken = token.refreshToken ?: "",
                                    idToken = token.idToken ?: ""
                                )
                            )
                        )
                        return@launch
                    }
                }
                callback(Result.failure(Exception("No data available")))
            } catch (e: Exception) {
                callback(Result.failure(e))
            }
        }
    }

}

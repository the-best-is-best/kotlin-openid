package io.github.openid

import io.github.app_auth_interop.KAuthManager
import io.github.kmmcrypto.KMMCrypto
import io.native.appauth.OIDAuthState
import io.native.appauth.OIDAuthorizationService
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import platform.Foundation.NSKeyedArchiver
import platform.Foundation.NSKeyedUnarchiver
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException


@OptIn(ExperimentalForeignApi::class)
actual class AuthOpenId {
    private val authInterop by lazy { KAuthManager.shared() }
    companion object {
        internal lateinit var service: String
        internal lateinit var group: String
    }

    private val crypto = KMMCrypto()

    actual fun init(key: String, group: String) {
        service = key
        AuthOpenId.group = group

        authInterop.initCryptoWithService(service, group)
    }

    actual suspend fun refreshToken(tokenRequest: TokenRequest): Result<AuthResult> =
        withContext(Dispatchers.Main) { // Move to Main Thread to avoid ObjHeader traps
            try {
                val authState = loadState()
                    ?: return@withContext Result.failure(Exception("Auth state missing"))

                // The trap often happens here if the native authState
                // is accessed from the wrong thread.
                val refreshRequest = authState.tokenRefreshRequest()
                    ?: return@withContext Result.failure(Exception("Refresh request is null"))

                val tokenResponse = suspendCancellableCoroutine { cont ->
                    OIDAuthorizationService.performTokenRequest(refreshRequest) { response, error ->
                        // Guard the continuation
                        if (cont.isActive) {
                            if (response != null) cont.resume(response)
                            else cont.resumeWithException(Exception(error?.localizedDescription))
                        }
                    }
                }

                authState.updateWithTokenResponse(tokenResponse, null)
                saveState(authState)

                Result.success(
                    AuthResult(
                        accessToken = tokenResponse.accessToken ?: "",
                        refreshToken = tokenResponse.refreshToken ?: "",
                        idToken = tokenResponse.idToken ?: ""
                    )
                )
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    actual suspend fun getLastAuth(): Result<AuthResult?> = withContext(Dispatchers.Main) {
        try {
            val state = loadState() // This now runs on Main
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

    // Ensure loadState is also called within a Main context or called from one


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

    private suspend fun loadState(): OIDAuthState? = withContext(Dispatchers.Main) {
        try {
            val data = crypto.loadDataType(service, group)
            data?.let {
                // Unarchiving native objects MUST happen on a consistent thread
                val auth = NSKeyedUnarchiver.unarchiveObjectWithData(it) as? OIDAuthState
                auth
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun login(authorizationRequest: AuthorizationRequest): Result<AuthResult> =
        suspendCancellableCoroutine { cont ->
            authInterop.loginWithOpenId(authorizationRequest.toIOSOpenIdConfig()) { res, error ->
                if (error != null) {
                    cont.resume(Result.failure(Exception(error)))
                    return@loginWithOpenId
                } else {
                    // 1. Extract tokens
                    val accessToken = res?.accessToken() ?: ""
                    val refreshToken = res?.refreshToken() ?: ""
                    val idToken = res?.idToken() ?: ""

                    // 2. Create the AuthResult object
                    val authResult = AuthResult(
                        accessToken = accessToken,
                        refreshToken = refreshToken,
                        idToken = idToken
                        // Add other fields if your AuthResult class requires them
                    )

                    println("Authentication successful: Access Token: $accessToken")

                    try {
                        // 3. Resume with the AuthResult object, NOT just 'true'
                        cont.resume(Result.success(authResult))
                    } catch (e: Exception) {
                        cont.resume(Result.failure(Exception("Failed to save auth state: ${e.message}")))
                    }
                }
            }
        }

    suspend fun logout(authorizationRequest: AuthorizationRequest): Result<Boolean> =
        suspendCancellableCoroutine { cont ->
            authInterop.logoutWithOpenId(authorizationRequest.toIOSOpenIdConfig()) { res, error ->
                // 1. Handle Native Errors (e.g., Network/Browser issues)
                if (error != null) {
                    println("Native Logout Error: $error")
                    cont.resume(Result.failure(Exception(error)))
                    return@logoutWithOpenId
                }

                // 2. Check if 'res' is actually true (The user confirmed logout in the browser)
                if (res) {
                    try {
                        // Only delete local data if the server/browser logout was successful
                        crypto.deleteData(service, group)
                        println("Local data wiped successfully.")
                        cont.resume(Result.success(true))
                    } catch (e: Exception) {
                        cont.resume(Result.failure(Exception("Wipe failed: ${e.message}")))
                    }
                } else {
                    // If res is false, the user likely cancelled the logout dialog
                    println("Logout cancelled by user or failed.")
                    cont.resume(Result.success(false))
                }
            }
        }

}
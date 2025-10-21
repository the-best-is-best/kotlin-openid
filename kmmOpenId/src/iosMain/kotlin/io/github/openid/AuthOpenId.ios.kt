package io.github.openid

import io.github.appauth.OIDAuthState
import io.github.appauth.OIDAuthorizationRequest
import io.github.appauth.OIDAuthorizationService
import io.github.appauth.OIDEndSessionRequest
import io.github.appauth.OIDExternalUserAgentIOS
import io.github.appauth.OIDResponseTypeCode
import io.github.kmmcrypto.KMMCrypto
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSKeyedArchiver
import platform.Foundation.NSKeyedUnarchiver
import platform.Foundation.NSURL
import platform.UIKit.UIApplication
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
                NSKeyedUnarchiver.unarchiveObjectWithData(it) as? OIDAuthState
            }
        } catch (e: Exception) {
            null
        }
    }

    fun login(onAuthResult: (Boolean?) -> Unit) {
        val authRequest = createAuthRequest()
        val viewController = UIApplication.sharedApplication.keyWindow?.rootViewController

        if (viewController == null) {
            onAuthResult(false)
            return
        }

        val externalUserAgent = OIDExternalUserAgentIOS(
            presentingViewController = viewController
        )

        OIDAuthState.authStateByPresentingAuthorizationRequest(
            authorizationRequest = authRequest,
            externalUserAgent = externalUserAgent,
            callback = { authState, _ ->
                if (authState != null) {

                    val accessToken = authState.lastTokenResponse?.accessToken ?: ""
                    val refreshToken = authState.lastTokenResponse?.refreshToken ?: ""
                    val idToken =
                        authState.lastTokenResponse?.idToken ?: ""  // Extract ID token
                    println("Authentication successful: Access Token: $accessToken, Refresh Token: $refreshToken, ID Token: $idToken")
                    AuthOpenId().saveState(authState)
                    onAuthResult(true)
                } else {
                    onAuthResult(false)
                }
            }

        )
    }

    suspend fun logout(callback: (Boolean?) -> Unit) {
        val authConfig = getAuthConfig()

        val idToken = AuthOpenId().loadState()?.lastTokenResponse?.idToken
        if (idToken == null) {
            callback(false)
            return
        }

        val endSessionRequest = OIDEndSessionRequest(
            configuration = authConfig,
            idTokenHint = idToken,
            postLogoutRedirectURL = NSURL(string = OpenIdConfig.postLogoutRedirectURL),
            additionalParameters = null
        )

        // Present the logout request in a web view (or default browser)
        val viewController = UIApplication.sharedApplication.keyWindow?.rootViewController
        if (viewController == null) {
            callback(false)
            return
        }

        val externalUserAgent = OIDExternalUserAgentIOS(
            presentingViewController = viewController
        )

        OIDAuthorizationService.presentEndSessionRequest(
            endSessionRequest,
            externalUserAgent,
            callback = { endSessionResponse, error ->
                if (endSessionResponse != null) {
                    // Handle successful logout
                    KMMCrypto().deleteData(service, group)
                    callback(true) // Return null or any specific result if needed
                } else {
                    callback(false)
                }
            }
        )
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun createAuthRequest(): OIDAuthorizationRequest {
        val authConfig = getAuthConfig()
        val clientId = OpenIdConfig.clientId
        val scopesList: List<String> = OpenIdConfig.scope.split(" ")
        val redirectUrl = NSURL(string = OpenIdConfig.redirectUrl)

        return OIDAuthorizationRequest(
            configuration = authConfig,
            clientId = clientId,
            clientSecret = null,
            scopes = scopesList,
            redirectURL = redirectUrl,
            responseType = OIDResponseTypeCode!!,
            additionalParameters = null
        )
    }
}

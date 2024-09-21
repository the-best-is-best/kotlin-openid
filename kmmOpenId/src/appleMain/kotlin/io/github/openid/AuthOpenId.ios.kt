// iosMain/src/AuthOpenId.kt
package io.github.openid

import cocoapods.AppAuth.OIDAuthState
import cocoapods.AppAuth.OIDAuthorizationRequest
import cocoapods.AppAuth.OIDAuthorizationService
import cocoapods.AppAuth.OIDEndSessionRequest
import cocoapods.AppAuth.OIDExternalUserAgentIOS
import cocoapods.AppAuth.OIDResponseTypeCode
import cocoapods.AppAuth.OIDServiceConfiguration
import io.github.kmmcrypto.KMMCrypto
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import platform.Foundation.NSData
import platform.Foundation.NSKeyedArchiver
import platform.Foundation.NSKeyedUnarchiver
import platform.Foundation.NSURL
import platform.UIKit.UIApplication


@OptIn(ExperimentalForeignApi::class)
actual class AuthOpenId {
    companion object {
        private lateinit var service: String
        private lateinit var group: String

        private var authState: OIDAuthState? = null

        private val crypto = KMMCrypto()

    }


    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    actual fun init(key: String, group: String) {
        service = key
        AuthOpenId.group = group
        scope.launch {
            authState = loadState()
        }
    }


    actual fun auth(callback: (Result<Boolean>) -> Unit) {
        val authRequest = createAuthRequest()
        val viewController = UIApplication.sharedApplication.keyWindow?.rootViewController

        if (viewController == null) {
            callback(Result.failure(Exception("No root view controller available")))
            return
        }

        val externalUserAgent = OIDExternalUserAgentIOS(
            presentingViewController = viewController
        )

        println("Starting authentication request...")
        OIDAuthState.authStateByPresentingAuthorizationRequest(
            authorizationRequest = authRequest,
            externalUserAgent = externalUserAgent,
            callback = { authState, error ->
                if (authState != null) {
                    AuthOpenId.authState = authState
                    val accessToken = authState.lastTokenResponse?.accessToken ?: ""
                    val refreshToken = authState.lastTokenResponse?.refreshToken ?: ""
                    val idToken = authState.lastTokenResponse?.idToken ?: ""  // Extract ID token
                    println("Authentication successful: Access Token: $accessToken, Refresh Token: $refreshToken, ID Token: $idToken")
                    saveState(authState)
                    callback(Result.success(true))
                } else {
                    println("Authorization error: ${error?.localizedDescription}")
                    callback(Result.failure(Exception("Authorization failed: ${error?.localizedDescription}")))
                }
            }
        )
    }


    actual fun refreshToken(callback: (Result<Boolean>) -> Unit) {
        val authState = AuthOpenId.authState

        if (authState == null) {
            callback(Result.failure(Exception("No auth state available")))
            return
        }

        authState.performActionWithFreshTokens { _, _, error ->

            if (error == null) {
                saveState(authState)
                callback(Result.success(true))
            } else {
                println("Token refresh error: ${error.localizedDescription}")
                callback(Result.failure(Exception("Token refresh failed: ${error.localizedDescription}")))
            }
        }
    }


    actual fun logout(callback: (Result<Boolean?>) -> Unit) {
        val authConfig = getAuthConfig()

        val idToken = authState?.lastTokenResponse?.idToken
        if (idToken == null) {
            callback(Result.failure(IllegalStateException("ID Token is null")))
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
            callback(Result.failure(Exception("No root view controller available")))
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
                    authState = null
                    // Handle successful logout
                    crypto.saveDataType(service, group, NSData())
                    callback(Result.success(null)) // Return null or any specific result if needed
                } else {
                    println("Logout error: ${error?.localizedDescription}")
                    callback(Result.failure(Exception("Logout failed: ${error?.localizedDescription}")))
                }
            }
        )
    }

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

    private fun getAuthConfig(): OIDServiceConfiguration = OIDServiceConfiguration(
        authorizationEndpoint = NSURL(string = OpenIdConfig.authEndPoint),
        tokenEndpoint = NSURL(string = OpenIdConfig.tokenEndPoint),
        null,
        null,
        NSURL(string = OpenIdConfig.endSessionEndPoint),


        )


    private fun saveState(authState: OIDAuthState?) {
        try {
            val data = authState?.let {
                NSKeyedArchiver.archivedDataWithRootObject(it)
            } ?: throw IllegalStateException("Data is null")
            println("data will save is $data")
            crypto.saveDataType(service, group, data)
            println("data saved")
        } catch (e: Exception) {
            println("Error save data ${e.message}")
            throw Exception(e.message)
        }


    }

    private suspend fun loadState(): OIDAuthState? {

        try {
            val data = crypto.loadDataType(service, group)
            println("Data retrieved: $data")

            return try {
                data?.let {
                    NSKeyedUnarchiver.unarchiveObjectWithData(it) as? OIDAuthState
                }
            } catch (e: Exception) {
                println("Error during unarchiving: ${e.message}")
                null
            }
        } catch (e: Exception) {
            println("Error is ${e.message}")
            return null
        }
    }


    actual fun getLastAuth(callback: (Result<AuthResult?>) -> Unit) {
        if (authState != null) {
            val lastTokenResponse = authState!!.lastTokenResponse
            if (lastTokenResponse != null) {
                callback(
                    Result.success(
                        AuthResult(
                            accessToken = lastTokenResponse.accessToken!!,
                            refreshToken = lastTokenResponse.refreshToken!!,
                            idToken = lastTokenResponse.idToken!!,
                        )
                    )
                )
            }
        }
        return callback(Result.failure(Exception("No data available")))
    }


}





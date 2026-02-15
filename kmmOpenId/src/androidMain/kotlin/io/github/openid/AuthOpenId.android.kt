package io.github.openid

import android.app.Activity
import android.content.Intent
import androidx.core.net.toUri
import io.github.kmmcrypto.KMMCrypto
import kotlinx.coroutines.suspendCancellableCoroutine
import net.openid.appauth.AuthorizationService
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

    // فنكشن مساعدة عشان تجيب الـ Activity بأمان
    private fun getActivity(): Activity? = applicationContext as? Activity

    actual suspend fun refreshToken(tokenRequest: io.github.openid.TokenRequest): Result<AuthResult> {
        val authService = AuthorizationService(applicationContext)
        return try {
            val authResult = getLastAuth().getOrThrow()
            val refreshToken = authResult?.refreshToken
                ?: return Result.failure(Exception("❌ Refresh token is missing"))

            val serviceConfig = getAuthServicesConfig(
                tokenRequest.issuer,
                AuthorizationServiceConfig(
                    authorizationEndpoint = tokenRequest.tokenEndpoint,
                    tokenEndpoint = tokenRequest.tokenEndpoint,
                    endSessionEndpoint = "connect/endsession",
                    registerEndPoint = null
                )
            )
            val tokenRequestBuilder = TokenRequest.Builder(
                serviceConfig,
                tokenRequest.clientId
            ).setGrantType(GrantTypeValues.REFRESH_TOKEN)
                .setRefreshToken(refreshToken)
                .build()

            val tokenResponse =
                suspendCancellableCoroutine { cont ->
                    authService.performTokenRequest(tokenRequestBuilder) { response, ex ->
                        if (response != null) cont.resume(response)
                        else cont.resumeWithException(
                            ex ?: Exception("❌ Unknown error during token refresh")
                        )
                    }
                }

            val newAuthResult = AuthResult(
                accessToken = tokenResponse.accessToken.orEmpty(),
                refreshToken = tokenResponse.refreshToken.orEmpty(),
                idToken = tokenResponse.idToken.orEmpty()
            )

            saveData(newAuthResult)
            Result.success(newAuthResult)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getLoginIntent(authorizationRequest: AuthorizationRequest): Intent {
        val authService = AuthorizationService(applicationContext)

        val serviceConfig = getAuthServicesConfig(
            authorizationRequest.issuer,
            authorizationRequest.authorizationServiceConfiguration
        )

        val request = net.openid.appauth.AuthorizationRequest.Builder(
            serviceConfig,
            authorizationRequest.clientId,
            ResponseTypeValues.CODE,
            authorizationRequest.redirectUrl.toUri()
        ).setScope(authorizationRequest.scope.joinToString(" ")).build()

        // Return the intent to be started by the Activity/Fragment
        return authService.getAuthorizationRequestIntent(request)
    }


    suspend fun getLogoutIntent(authorizationRequest: AuthorizationRequest): Intent {
        val authService = AuthorizationService(applicationContext)

        val serviceConfig = getAuthServicesConfig(
            authorizationRequest.issuer,
            authorizationRequest.authorizationServiceConfiguration
        )

        val endSessionRequest = net.openid.appauth.EndSessionRequest.Builder(serviceConfig)
            .setPostLogoutRedirectUri(authorizationRequest.authorizationServiceConfiguration.postLogoutRedirectURL!!.toUri())
            // Important: AppAuth often requires the ID Token hint for logout
            .apply {
                getLastAuth().getOrNull()?.idToken?.let { setIdTokenHint(it) }
            }
            .build()

        // Clear local data
        KMMCrypto().deleteData(key, group)

        return authService.getEndSessionRequestIntent(endSessionRequest)
    }


    actual suspend fun getLastAuth(): Result<AuthResult?> {
        return try {

            val dataString = KMMCrypto().loadData(key, group)
            if (dataString == null) {
                Result.success(null)
            } else {
                val authResult =
                    kotlinx.serialization.json.Json.decodeFromString<AuthResult>(dataString)
                Result.success(authResult)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun saveData(data: AuthResult?) {
        val kmmCrypto = KMMCrypto()
        if (data != null) {
            val jsonString = kotlinx.serialization.json.Json.encodeToString(data)

            kmmCrypto.saveData(key, group, jsonString)
        } else {
            kmmCrypto.saveData(key, group, "")
        }
    }

    fun deleteLastAuth() {
        val kmmCrypto = KMMCrypto()
        kmmCrypto.deleteData(key, group)
    }
}
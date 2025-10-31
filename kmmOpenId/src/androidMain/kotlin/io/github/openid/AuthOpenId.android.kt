package io.github.openid

import com.google.gson.Gson
import io.github.kmmcrypto.KMMCrypto
import kotlinx.coroutines.suspendCancellableCoroutine
import net.openid.appauth.AuthorizationService
import net.openid.appauth.GrantTypeValues
import net.openid.appauth.TokenRequest
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException


actual class AuthOpenId {
    private val authService = AuthorizationService(applicationContext)

    companion object {
        lateinit var key: String
        lateinit var group: String
    }

    actual fun init(key: String, group: String) {
        AuthOpenId.key = key
        AuthOpenId.group = group
    }

    actual suspend fun refreshToken(): Result<AuthResult> {
        return try {
            val authResult = getLastAuth().getOrThrow()
            val refreshToken = authResult?.refreshToken
                ?: return Result.failure(Exception("❌ Refresh token is missing"))

            val serviceConfig = getAuthServicesConfig()
            val tokenRequest = TokenRequest.Builder(
                serviceConfig,
                OpenIdConfig.clientId
            ).setGrantType(GrantTypeValues.REFRESH_TOKEN)
                .setRefreshToken(refreshToken)
                .build()

            val tokenResponse =
                suspendCancellableCoroutine { cont ->
                    authService.performTokenRequest(tokenRequest) { response, ex ->
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


    actual suspend fun getLastAuth(): Result<AuthResult?> {
        return try {
            val dataString = KMMCrypto().loadData(key, group)
            val authResult = Gson().fromJson(dataString, AuthResult::class.java)
            Result.success(authResult)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun saveData(data: AuthResult?) {
        val kmmCrypto = KMMCrypto()
        if (data != null) {
            val jsonString = Gson().toJson(data)

            kmmCrypto.saveData(key, group, jsonString)
        } else {
            kmmCrypto.saveData(key, group, "")
        }
    }
}

package io.github.openid

import android.annotation.SuppressLint
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationResponse
import net.openid.appauth.AuthorizationService
import net.openid.appauth.TokenResponse
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@SuppressLint("StaticFieldLeak")
object AndroidOpenId {

     lateinit var authLauncher: ActivityResultLauncher<Intent>
    lateinit var continuation: CancellableContinuation<AuthResult?>
     lateinit var authService: AuthorizationService

    // Initialize the static members with an activity
    @JvmStatic
    fun init(activity: ComponentActivity) {
        authService = AuthorizationService(activity)
        authLauncher = activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            CoroutineScope(Dispatchers.Main).launch {
                val authResult = handleAuthResult(result)
                continuation.resume(authResult)
            }
        }
    }

    private suspend fun handleAuthResult(result: androidx.activity.result.ActivityResult): AuthResult? {
        val data = result.data
        if (data != null) {
            val response = AuthorizationResponse.fromIntent(data)
            val error = AuthorizationException.fromIntent(data)

            if (response != null) {
                return exchangeAuthorizationCode(response)
            } else if (error != null) {
                handleAuthError(error)
            }
        }
        return null
    }

    private suspend fun exchangeAuthorizationCode(response: AuthorizationResponse): AuthResult? {
        val tokenRequest = response.createTokenExchangeRequest()

        return suspendCancellableCoroutine { cont ->
            authService.performTokenRequest(tokenRequest) { tokenResponse, exception ->
                if (tokenResponse != null) {
                    val authResult = handleTokenResponse(tokenResponse)
                    cont.resume(authResult)
                } else if (exception != null) {
                    cont.resumeWithException(exception)
                } else {
                    cont.resume(null)
                }
            }
        }
    }

    private fun handleAuthError(error: AuthorizationException) {
        println("Authorization error: ${error.message}")
    }

     fun handleTokenResponse(tokenResponse: TokenResponse): AuthResult? {
        val accessToken = tokenResponse.accessToken
        val refreshToken = tokenResponse.refreshToken
        return if (accessToken != null && refreshToken != null)
            AuthResult(accessToken, refreshToken)
        else null
    }

}

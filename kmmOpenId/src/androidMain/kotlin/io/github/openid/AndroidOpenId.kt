package io.github.openid

import android.app.Activity
import com.google.gson.Gson
import io.github.kmmcrypto.KMMCrypto
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationResponse
import net.openid.appauth.AuthorizationService
import net.openid.appauth.TokenResponse
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class AndroidOpenId {

    //    internal lateinit var authLauncher: ActivityResultLauncher<Intent>
    internal lateinit var continuation: CancellableContinuation<Boolean?>

//    internal lateinit var logoutLauncher: ActivityResultLauncher<Intent>


    private val kmmCrypto = KMMCrypto()

    private val gson = Gson()
//
//    @Composable
//    fun Init() {
//        // Get the context and make sure it's an Activity
//
//        // Initialize authLauncher using rememberLauncherForActivityResult
//        authLauncher =
//            rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
//                if (::continuation.isInitialized && !continuation.isCompleted) {
//                        val isSuccess = handleAuthResult(result) // Check if login was successful
//                        continuation.resume(isSuccess != null) // Resume with correct result
//
//                }
//            }
//
//        // Initialize logoutLauncher using rememberLauncherForActivityResult
//        logoutLauncher =
//            rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
//                if (::continuation.isInitialized && !continuation.isCompleted) {
//                        val isSuccess = handleLogoutResult(result) // Handle logout result
//                        continuation.resume(isSuccess)
//                    }
//                }
//            }
//    }


    internal fun handleLogoutResult(result: androidx.activity.result.ActivityResult): Boolean {
        return result.resultCode == Activity.RESULT_OK
    }

    internal suspend fun handleAuthResult(result: androidx.activity.result.ActivityResult): AuthResult? {
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
        saveData(null)
        return null
    }

    private suspend fun exchangeAuthorizationCode(response: AuthorizationResponse): AuthResult? {
        val tokenRequest = response.createTokenExchangeRequest()
        val authService = AuthorizationService(applicationContext)

        return suspendCancellableCoroutine { cont ->
            authService.performTokenRequest(tokenRequest) { tokenResponse, exception ->
                if (tokenResponse != null) {
                    val authResult = handleTokenResponse(tokenResponse)
                    saveData(authResult)
                    if (authResult != null) {

                        cont.resume(authResult)

                    } else {
                        cont.resume(null)

                    }
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

    private fun handleTokenResponse(tokenResponse: TokenResponse?): AuthResult? {
        val accessToken = tokenResponse?.accessToken
        val refreshToken = tokenResponse?.refreshToken
        val idToken = tokenResponse?.idToken

         return if (accessToken != null && refreshToken != null && idToken != null)
             AuthResult(accessToken, refreshToken, idToken)
        else null
    }

    private fun saveData(data: AuthResult?) {

        if (data != null) {
            val jsonString = gson.toJson(data)

            kmmCrypto.saveData(AuthOpenId.key, AuthOpenId.group, jsonString)
            println("data saved")
        } else {
            kmmCrypto.saveData(AuthOpenId.key, AuthOpenId.group, "")
            println("data removed")
        }
    }
}



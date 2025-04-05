// iosMain/src/AuthOpenId.kt
package io.github.openid


import io.github.appauth.OIDAuthState
import io.github.kmmcrypto.KMMCrypto
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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


    actual suspend fun refreshToken(callback: (Result<Boolean>) -> Unit) {
        CoroutineScope(Dispatchers.Main + SupervisorJob()).launch {

            val authState = loadState()

            if (authState == null) {
                callback(Result.failure(Exception("No auth state available")))
                return@launch
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
    }


    internal fun saveState(authState: OIDAuthState?) {
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

    internal suspend fun loadState(): OIDAuthState? {

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


    actual suspend fun getLastAuth(callback: (Result<AuthResult?>) -> Unit) {
        val loadState = loadState()
        println("get last auth")
        if (loadState != null) {
            val lastTokenResponse = loadState.lastTokenResponse
            if (lastTokenResponse != null) {
                println("data loaded token is ${lastTokenResponse.accessToken}")
                callback(
                    Result.success(
                        AuthResult(
                            accessToken = lastTokenResponse.accessToken!!,
                            refreshToken = lastTokenResponse.refreshToken!!,
                            idToken = lastTokenResponse.idToken!!,
                        )
                    )
                )
                return // Early return to prevent further execution
            }
        }
        callback(Result.failure(Exception("No data available"))) // Only call this if no success callback has been executed
    }


}





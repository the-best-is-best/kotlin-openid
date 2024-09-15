package io.github.openid

import android.net.Uri
import io.github.openid.AndroidOpenId.authLauncher
import io.github.openid.AndroidOpenId.authService
import io.github.openid.AndroidOpenId.continuation
import io.github.openid.AndroidOpenId.handleTokenResponse
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.suspendCancellableCoroutine
import net.openid.appauth.AuthorizationRequest
import net.openid.appauth.AuthorizationServiceConfiguration
import net.openid.appauth.GrantTypeValues
import net.openid.appauth.ResponseTypeValues
import net.openid.appauth.TokenRequest
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

actual class AuthOpenId

 {


    actual suspend fun auth(): AuthResult? {
        val serviceConfig = AuthorizationServiceConfiguration(
            Uri.parse(OpenIdConfig.authEndPoint),
            Uri.parse(OpenIdConfig.tokenEndPoint)
        )

        val authRequest = AuthorizationRequest.Builder(
            serviceConfig,
            OpenIdConfig.clientId,
            ResponseTypeValues.CODE,
            Uri.parse(OpenIdConfig.redirectUrl)
        ).setScopes(OpenIdConfig.scope)
            .build()

        val authIntent = authService.getAuthorizationRequestIntent(authRequest)

        return suspendCancellableCoroutine { cont ->
            continuation = cont
            authLauncher.launch(authIntent)
        }
    }

     actual fun config(
         discoveryUrl: String,
         tokenEndPoint: String,
         authEndPoint: String,
         endSessionEndPoint: String,
         clientId: String,
         redirectUrl: String,
         scope: String
     ) {
         OpenIdConfig.discoveryUrl =discoveryUrl
         OpenIdConfig.tokenEndPoint = tokenEndPoint
         OpenIdConfig.authEndPoint = authEndPoint
         OpenIdConfig.endSessionEndPoint = endSessionEndPoint
         OpenIdConfig.clientId = clientId
         OpenIdConfig.redirectUrl  = redirectUrl
         OpenIdConfig.scope = scope
     }

     @OptIn(ExperimentalCoroutinesApi::class)
     actual suspend fun refreshToken(refreshToken:String): AuthResult? {
         val serviceConfig = AuthorizationServiceConfiguration(
             Uri.parse(OpenIdConfig.authEndPoint),
             Uri.parse(OpenIdConfig.tokenEndPoint)
         )
         val tokenRequest = TokenRequest.Builder(
             serviceConfig,
             OpenIdConfig.clientId
         ).setGrantType(GrantTypeValues.REFRESH_TOKEN)
             .setRefreshToken(refreshToken)
             .build()

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
 }

package io.github.openid


fun authOpenIdConfig(
    issuerUrl: String,
    discoveryUrl: String,
    tokenEndPoint: String,
    authEndPoint: String,
    endSessionEndPoint: String,
    clientId: String,
    redirectUrl: String,
    scope: String,
    postLogoutRedirectURL: String

) {
    OpenIdConfig.authEndPoint = "$issuerUrl/$authEndPoint"
    OpenIdConfig.scope = scope
    OpenIdConfig.clientId = clientId
    OpenIdConfig.redirectUrl = redirectUrl
    OpenIdConfig.discoveryUrl = "$issuerUrl/$discoveryUrl"
    OpenIdConfig.tokenEndPoint = "$issuerUrl/$tokenEndPoint"
    OpenIdConfig.endSessionEndPoint = "$issuerUrl/$endSessionEndPoint"
    OpenIdConfig.postLogoutRedirectURL = postLogoutRedirectURL

}
 expect class AuthOpenId() {


     suspend fun auth(): Boolean?

     suspend fun refreshToken(refreshToken: String): Boolean?

     suspend fun logout(idToken: String): Boolean?

     fun getLastAuth(): AuthResult?
 }




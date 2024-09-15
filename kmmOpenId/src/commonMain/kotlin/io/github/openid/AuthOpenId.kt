package io.github.openid


fun authOpenIdConfig(
    issuerUrl: String,
    discoveryUrl: String,
    tokenEndPoint: String,
    authEndPoint: String,
    endSessionEndPoint: String,
    clientId: String,
    redirectUrl: String,
    scope: String
) {
    OpenIdConfig.authEndPoint = issuerUrl + "/" + authEndPoint
    OpenIdConfig.scope = scope
    OpenIdConfig.clientId = clientId
    OpenIdConfig.redirectUrl = redirectUrl
    OpenIdConfig.discoveryUrl = issuerUrl + "/" + discoveryUrl
    OpenIdConfig.tokenEndPoint = issuerUrl + "/" + tokenEndPoint
    OpenIdConfig.endSessionEndPoint = issuerUrl + "/" + endSessionEndPoint

}
 expect class AuthOpenId() {


     suspend fun auth(): AuthResult?

     suspend fun refreshToken(refreshToken:String): AuthResult?
 }




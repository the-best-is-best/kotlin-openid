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


     fun init(key: String, group: String)


     fun auth(callback: (Result<Boolean>) -> Unit)

     fun refreshToken(callback: (Result<Boolean>) -> Unit)

     fun logout(callback: (Result<Boolean?>) -> Unit)

     fun getLastAuth(callback: (Result<AuthResult?>) -> Unit)
 }




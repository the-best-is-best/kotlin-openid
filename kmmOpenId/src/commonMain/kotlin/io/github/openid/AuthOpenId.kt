package io.github.openid


fun authOpenIdConfig(
    issuerUrl: String,
    discoveryUrl: String,
    tokenEndPoint: String,
    authEndPoint: String,
    endSessionEndPoint: String,
    registerEndPoint: String? = null,
    clientId: String,
    redirectUrl: String,
    scope: String,
    postLogoutRedirectURL: String

) {

    OpenIdConfig.issuer = issuerUrl
    OpenIdConfig.authEndPoint = "$issuerUrl/$authEndPoint"
    OpenIdConfig.scope = scope
    OpenIdConfig.clientId = clientId
    OpenIdConfig.redirectUrl = redirectUrl
    OpenIdConfig.discoveryUrl = "$issuerUrl/$discoveryUrl"
    OpenIdConfig.tokenEndPoint = "$issuerUrl/$tokenEndPoint"
    OpenIdConfig.endSessionEndPoint = "$issuerUrl/$endSessionEndPoint"
    OpenIdConfig.postLogoutRedirectURL = postLogoutRedirectURL
    OpenIdConfig.registerEndPoint = registerEndPoint


}

 expect class AuthOpenId() {


     fun init(key: String, group: String)

     suspend fun refreshToken(): Result<AuthResult>
     suspend fun getLastAuth(): Result<AuthResult?>

 }


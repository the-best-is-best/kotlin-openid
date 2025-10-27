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

    OpenIdConfig.issuerUrl = issuerUrl
    OpenIdConfig.authEndPoint = authEndPoint
    OpenIdConfig.scope = scope
    OpenIdConfig.clientId = clientId
    OpenIdConfig.redirectUrl = redirectUrl
    OpenIdConfig.discoveryUrl = discoveryUrl
    OpenIdConfig.tokenEndPoint = tokenEndPoint
    OpenIdConfig.endSessionEndPoint = endSessionEndPoint
    OpenIdConfig.postLogoutRedirectURL = postLogoutRedirectURL
    OpenIdConfig.registerEndPoint = registerEndPoint


}

 expect class AuthOpenId() {


     fun init(key: String, group: String)

     suspend fun refreshToken(): Result<AuthResult>
     suspend fun getLastAuth(): Result<AuthResult?>

 }


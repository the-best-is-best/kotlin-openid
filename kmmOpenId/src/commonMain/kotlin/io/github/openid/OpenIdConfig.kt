package io.github.openid

//internal object OpenIdConfig {
//    lateinit var issuer: String
//    lateinit var discoveryUrl: String
//    lateinit var tokenEndPoint: String
//    lateinit var authEndPoint: String
//    lateinit var endSessionEndPoint: String
//    var registerEndPoint: String? = null
//    lateinit var clientId: String
//    lateinit var redirectUrl: String
//    lateinit var scope: String
//    lateinit var postLogoutRedirectURL: String
//}

data class AuthorizationRequest(
    val clientId: String,
    val redirectUrl: String,
    val issuer: String,
    val discoveryUrl: String,
    val scope: List<String>,
    val authorizationServiceConfiguration: AuthorizationServiceConfig
)

//
data class AuthorizationServiceConfig(
    val authorizationEndpoint: String,
    val tokenEndpoint: String,
    val endSessionEndpoint: String? = null,
    val postLogoutRedirectURL: String? = null,
    val registerEndPoint: String? = null
)


data class TokenRequest(
    val issuer: String,
    val tokenEndpoint: String,
    val clientId: String,
    val clientSecret: String? = null
)
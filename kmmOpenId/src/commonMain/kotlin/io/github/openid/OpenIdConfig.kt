package io.github.openid


data class AuthorizationRequest(
    val clientId: String,
    val redirectUrl: String,
    val issuer: String,
    val discoveryUrl: String,
    val scope: List<String>,
    val authorizationServiceConfiguration: AuthorizationServiceConfig
)


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
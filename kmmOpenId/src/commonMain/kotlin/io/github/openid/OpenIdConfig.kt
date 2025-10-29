package io.github.openid

class OpenIdConfig(
    val issuerUrl: String,
    val discoveryUrl: String,
    val tokenEndPoint: String,
    val authEndPoint: String,
    val endSessionEndPoint: String,
    val registerEndPoint: String? = null,
    val clientId: String,
    val redirectUrl: String,
    val scope: String,
    val postLogoutRedirectURL: String
)

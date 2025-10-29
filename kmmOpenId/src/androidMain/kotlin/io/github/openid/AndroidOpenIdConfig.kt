package io.github.openid

internal object AndroidOpenIdConfig {
    lateinit var issuerUrl: String
    lateinit var discoveryUrl: String
    lateinit var tokenEndPoint: String
    lateinit var authEndPoint: String
    lateinit var endSessionEndPoint: String
    var registerEndPoint: String? = null
    lateinit var clientId: String
    lateinit var redirectUrl: String
    lateinit var scope: String
    lateinit var postLogoutRedirectURL: String

    fun init(openIdConfig: OpenIdConfig) {
        issuerUrl = openIdConfig.issuerUrl
        discoveryUrl = openIdConfig.discoveryUrl
        tokenEndPoint = openIdConfig.tokenEndPoint
        authEndPoint = openIdConfig.authEndPoint
        endSessionEndPoint = openIdConfig.endSessionEndPoint
        registerEndPoint = openIdConfig.registerEndPoint
        clientId = openIdConfig.clientId
        redirectUrl = openIdConfig.redirectUrl
        scope = openIdConfig.scope
        postLogoutRedirectURL = openIdConfig.postLogoutRedirectURL

    }
}
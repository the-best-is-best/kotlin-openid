package io.github.openid

object OpenIdConfig {
    lateinit var discoveryUrl: String
    lateinit var tokenEndPoint: String
    lateinit var authEndPoint: String
    lateinit var endSessionEndPoint: String
    var registerEndPoint: String? = null
    lateinit var clientId: String
    lateinit var redirectUrl: String
    lateinit var scope: String
    lateinit var postLogoutRedirectURL: String
}

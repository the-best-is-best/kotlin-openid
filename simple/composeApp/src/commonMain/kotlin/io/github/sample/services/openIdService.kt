package io.github.sample.services

import io.github.openid.AuthOpenId
import io.github.openid.OpenIdConfig

object OpenIdService {
    private val auth = AuthOpenId()

    val getAuth: AuthOpenId
        get() = auth

    private const val key = "key"
    private const val service = "service"

    init {
        val issuerUrl = "https://demo.duendesoftware.com"
        val client = OpenIdConfig(
            issuerUrl = issuerUrl,
            discoveryUrl = ".well-known/openid-configuration",
            tokenEndPoint = "connect/token",
            authEndPoint = "connect/authorize",
            endSessionEndPoint = "connect/endsession",
            clientId = "interactive.public",
            redirectUrl = "com.duendesoftware.demo:/oauthredirect",
            scope = "openid profile offline_access email api",
            postLogoutRedirectURL = "com.duendesoftware.demo:/",

            )
        auth.init(key, service, client)
    }
}
package io.github.sample.api

import io.github.openid.AuthOpenId
import io.github.openid.authOpenIdConfig

object AuthOpenedIdService {
    val auth = AuthOpenId()

    init {
        val issuerUrl = "https://demo.duendesoftware.com"
        authOpenIdConfig(
            issuerUrl = issuerUrl,
            discoveryUrl = ".well-known/openid-configuration",
            tokenEndPoint = "connect/token",
            authEndPoint = "connect/authorize",
            endSessionEndPoint = "connect/endsession",
            clientId = "interactive.public",
            redirectUrl = "com.duendesoftware.demo:/oauthredirect'",
            scope = "openid profile offline_access email api",
            postLogoutRedirectURL = "com.duendesoftware.demo:/",

            )
        auth.init("auth", "kmmOpenId")
    }
}
package io.github.kmmopenid.services

import io.github.openid.AuthorizationRequest
import io.github.openid.AuthorizationServiceConfig
import io.github.openid.TokenRequest


class OpenIdService {
    fun getAuthorizationRequest(): AuthorizationRequest {
        return AuthorizationRequest(
            issuer = "https://demo.duendesoftware.com",
            discoveryUrl = ".well-known/openid-configuration",
            scope = listOf("openid", "profile", "offline_access", "email"),
            clientId = "interactive.public",
            redirectUrl = "com.duendesoftware.demo:/oauthredirect",
            authorizationServiceConfiguration = AuthorizationServiceConfig(
                authorizationEndpoint = "connect/authorize",
                tokenEndpoint = "connect/token",
                endSessionEndpoint = "connect/endsession",
                postLogoutRedirectURL = "com.duendesoftware.demo:/postlogout",
                registerEndPoint = null
            ),
        )
    }


    fun getTokenRequest(): TokenRequest {
        return TokenRequest(
            issuer = "https://demo.duendesoftware.com",
            tokenEndpoint = "connect/token",
            clientId = "interactive.public",
        )
    }

}
package io.github.openid

import androidx.core.net.toUri
import net.openid.appauth.AuthorizationServiceConfiguration


fun getAuthServicesConfig(): AuthorizationServiceConfiguration {
    return AuthorizationServiceConfiguration(
        "${OpenIdConfig.issuerUrl}/${OpenIdConfig.authEndPoint}".toUri(),
        "${OpenIdConfig.issuerUrl}/${OpenIdConfig.tokenEndPoint}".toUri(),
        if (OpenIdConfig.registerEndPoint == null) null else OpenIdConfig.registerEndPoint?.toUri(),
        "${OpenIdConfig.issuerUrl}/${OpenIdConfig.endSessionEndPoint}".toUri(),

        )
}
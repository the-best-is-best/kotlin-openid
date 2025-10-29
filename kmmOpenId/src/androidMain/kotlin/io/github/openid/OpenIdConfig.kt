package io.github.openid

import androidx.core.net.toUri
import net.openid.appauth.AuthorizationServiceConfiguration


fun getAuthServicesConfig(): AuthorizationServiceConfiguration {
    return AuthorizationServiceConfiguration(
        "${AndroidOpenIdConfig.issuerUrl}/${AndroidOpenIdConfig.authEndPoint}".toUri(),
        "${AndroidOpenIdConfig.issuerUrl}/${AndroidOpenIdConfig.tokenEndPoint}".toUri(),
        if (AndroidOpenIdConfig.registerEndPoint == null) null else AndroidOpenIdConfig.registerEndPoint?.toUri(),
        "${AndroidOpenIdConfig.issuerUrl}/${AndroidOpenIdConfig.endSessionEndPoint}".toUri(),

        )
}
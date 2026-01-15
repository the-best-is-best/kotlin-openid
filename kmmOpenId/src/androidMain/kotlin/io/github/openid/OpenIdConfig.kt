package io.github.openid

import androidx.core.net.toUri
import net.openid.appauth.AuthorizationServiceConfiguration

internal fun getAuthServicesConfig(
    issurUrl: String,
    authorizationServiceConfiguration: AuthorizationServiceConfig
): AuthorizationServiceConfiguration {
    return AuthorizationServiceConfiguration(
        "${issurUrl}/${authorizationServiceConfiguration.authorizationEndpoint}".toUri(),
        "${issurUrl}/${authorizationServiceConfiguration.tokenEndpoint}".toUri(),
        if (authorizationServiceConfiguration.registerEndPoint == null) null else "${issurUrl}/${authorizationServiceConfiguration.registerEndPoint}".toUri(),
        if (authorizationServiceConfiguration.postLogoutRedirectURL == null) null else "${issurUrl}/${authorizationServiceConfiguration.endSessionEndpoint}".toUri()

        )
}

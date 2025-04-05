package io.github.openid

import io.github.appauth.OIDServiceConfiguration
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSURL


@OptIn(ExperimentalForeignApi::class)
internal fun getAuthConfig(): OIDServiceConfiguration = OIDServiceConfiguration(
    authorizationEndpoint = NSURL(string = OpenIdConfig.authEndPoint),
    tokenEndpoint = NSURL(string = OpenIdConfig.tokenEndPoint),
    null,
    if (OpenIdConfig.registerEndPoint == null) null else NSURL(string = OpenIdConfig.registerEndPoint!!),
    NSURL(string = OpenIdConfig.endSessionEndPoint),


    )
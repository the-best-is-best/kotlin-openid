package io.github.openid

import io.github.app_auth_interop.KOpenIdConfig
import kotlinx.cinterop.ExperimentalForeignApi

@OptIn(ExperimentalForeignApi::class)
fun AuthorizationRequest.toIOSOpenIdConfig(): KOpenIdConfig = KOpenIdConfig(
    this.issuer,
    this.clientId,
    this.redirectUrl,
    this.scope,
    this.authorizationServiceConfiguration.postLogoutRedirectURL ?: ""


)
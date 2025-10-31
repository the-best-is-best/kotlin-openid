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

        auth.init(key, service)
    }
}
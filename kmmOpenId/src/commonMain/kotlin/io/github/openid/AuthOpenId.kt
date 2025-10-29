package io.github.openid

expect class AuthOpenId() {


    fun init(key: String, group: String, openIdConfig: OpenIdConfig)

     suspend fun refreshToken(): Result<AuthResult>
     suspend fun getLastAuth(): Result<AuthResult?>

 }


package io.github.openid


expect class AuthOpenId() {


    fun init(key: String, group: String)

    suspend fun refreshToken(tokenRequest: TokenRequest): Result<AuthResult>
    suspend fun getLastAuth(): Result<AuthResult?>

}


package io.github.openid


expect class AuthOpenId() {


    fun init(key: String, group: String)

    //    suspend fun login(authRequest: AuthorizationRequest)
//    suspend fun logout(endSessionRequest: EndSessionRequest)
    suspend fun refreshToken(tokenRequest: TokenRequest): Result<AuthResult>
    suspend fun getLastAuth(): Result<AuthResult?>

}


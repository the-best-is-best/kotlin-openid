package io.github.openid

 expect class AuthOpenId() {
     fun  config(
          discoveryUrl: String,
       tokenEndPoint: String,
       authEndPoint: String,
          endSessionEndPoint: String,
          clientId: String,
         redirectUrl: String,
        scope: String
     )

     suspend fun auth(): AuthResult?

     suspend fun refreshToken(refreshToken:String): AuthResult?
 }




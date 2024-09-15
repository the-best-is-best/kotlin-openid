package io.github.sample

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import io.github.openid.AuthOpenId
import io.github.sample.theme.AppTheme
import kotlinx.coroutines.launch

@Composable
internal fun App() = AppTheme {
    val scope = rememberCoroutineScope()
    val auth = AuthOpenId()

    var refreshToken by remember {
        mutableStateOf("")
    }
    LaunchedEffect(Unit){
        val baseUrl:String = "https://sylidentitystaging.azurewebsites.net";

        auth.config(
            "$baseUrl/.well-known/openid-configuration",
            "$baseUrl/connect/token",
            "$baseUrl/connect/authorize",
            "au.u52ndsolution.syl",
            "syl.interpreter.mobile",
            "au.u52ndsolution.syl:/auth/signin-oidc",
            "openid profile offline_access syl.interpreter.api.fullaccess"
        )
    }
   Column {
       Button(onClick = {

           scope.launch {
         val result =   auth.auth()
               println("login success ${result!!.accessToken}")
               refreshToken = result.refreshToken
           }

       }){
           Text("Login")
       }
       Button(
           enabled = refreshToken.isNotEmpty(),
           onClick =  {

           scope.launch {
               val result =   auth.refreshToken(refreshToken)
               println("login success ${result!!.accessToken}")
               refreshToken = result.refreshToken
           }

       }){
           Text("Refresh Token")
       }


   }
}

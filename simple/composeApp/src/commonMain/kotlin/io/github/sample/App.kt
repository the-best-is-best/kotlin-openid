package io.github.sample

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.openid.AuthOpenId
import io.github.openid.authOpenIdConfig
import io.github.sample.theme.AppTheme
import kotlinx.coroutines.launch


@Composable
internal fun App() = AppTheme {
    val scope = rememberCoroutineScope()
    var auth by remember { mutableStateOf<AuthOpenId?>(null) }

    var refreshToken by remember { mutableStateOf("0450368BB663FA761544B84719623BC827FAA088784E2B65E80BEC64E28777EF-1") }
    var accessToken by remember { mutableStateOf("") }
    var idToken by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        val issuerUrl = "https://demo.duendesoftware.com"
        authOpenIdConfig(
            issuerUrl = issuerUrl,
            discoveryUrl = ".well-known/openid-configuration",
            tokenEndPoint = "connect/token",
            authEndPoint = "connect/authorize",
            endSessionEndPoint = "connect/endsession",
            clientId = "interactive.public",
            redirectUrl = "com.duendesoftware.demo:/oauthredirect'",
            scope = "openid profile offline_access email api",
            postLogoutRedirectURL = "com.duendesoftware.demo:/"
        )
        auth = AuthOpenId()
        val lastAuth = auth!!.getLastAuth()
        println("last auth token ${lastAuth?.accessToken}")
        if (lastAuth != null) {
            accessToken = lastAuth.accessToken
            refreshToken = lastAuth.refreshToken
            idToken = lastAuth.idToken
        }

    }

    Surface(
        modifier = Modifier.fillMaxSize()
            .navigationBarsPadding()
            .imePadding(),
        color = Color.White,
    ) {
        MaterialTheme(colorScheme = lightColorScheme()) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Button(onClick = {
                    scope.launch {
                        try {

                            println("Attempting to login")
                            auth!!.auth()
                            val result = auth!!.getLastAuth()

                            if (result != null) {
                                println("Login success ${result.accessToken}")
                                println("refresh token is ${result.refreshToken}")
                                refreshToken = result.refreshToken
                                idToken = result.idToken
                                accessToken = result.accessToken
                            } else {
                                println("Login failed: result is null")
                            }
                        } catch (e: Exception) {
                            println("Login failed: ${e.message}")
                        }
                    }
                }) {
                    Text("Login")
                }
                Button(
                    enabled = refreshToken.isNotEmpty(),
                    onClick = {
                        scope.launch {
                            try {
                                println("Attempting to refresh token")
                                auth!!.refreshToken(refreshToken)
                                val result = auth!!.getLastAuth()

                                if (result != null) {
                                    if (result.refreshToken == refreshToken) {
                                        println("refresh token is the same")
                                    }
                                    if (accessToken == result.accessToken) {
                                        println("acccess token is the same")
                                    }
                                    println("Refresh token success ${result.refreshToken}")
                                    println("access token is ${result.accessToken}")
                                    refreshToken = result.refreshToken
                                    accessToken = result.accessToken
                                    idToken = result.idToken
                                } else {
                                    println("Refresh token failed: result is null")
                                }
                            } catch (e: Exception) {
                                println("Refresh token failed: ${e.message}")
                            }
                        }
                    }
                ) {
                    Text("Refresh Token")
                }

                Spacer(Modifier.height(10.dp))
                Text("access token $accessToken", style = TextStyle(fontSize = 10.sp))
                Spacer(Modifier.height(10.dp))
                Text("refresh token $refreshToken", style = TextStyle(fontSize = 10.sp))
                Spacer(Modifier.height(10.dp))
                Text("id token $idToken", style = TextStyle(fontSize = 10.sp))


                Spacer(Modifier.height(50.dp))
                Button(
                    enabled = idToken.isNotEmpty(),
                    onClick = {
                        scope.launch {
                            auth!!.logout(idToken)
                            accessToken = ""
                            idToken = ""
                            refreshToken = ""
                        }
                    }) {
                    Text("Logout")
                }
            }
        }
    }
}

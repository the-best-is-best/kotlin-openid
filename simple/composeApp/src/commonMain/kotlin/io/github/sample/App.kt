package io.github.sample

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
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
import io.github.openid.RememberAuthOpenId
import io.github.openid.RememberLogoutOpenId
import io.github.sample.api.KtorServices
import io.github.sample.services.OpenIdService
import io.github.sample.theme.AppTheme
import kotlinx.coroutines.launch
import org.koin.compose.KoinApplication
import org.koin.compose.getKoin
import org.koin.dsl.module


@Composable
internal fun App() = AppTheme {
    var refreshToken by remember { mutableStateOf("") }
    var accessToken by remember { mutableStateOf("") }
    var idToken by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    val appModule = module {
        single { KtorServices() }
    }

//    LaunchedEffect(Unit){
//        OpenIdService.init()
//    }

    KoinApplication(
        application = {
            modules(appModule)
        }
    ) {
        val apiService = getKoin().get<KtorServices>()

        val authRes = RememberAuthOpenId {
            println("Auth result $it")
            it?.let { isAuthenticated ->
                if (isAuthenticated) {
                    scope.launch {

                        val res = OpenIdService.getAuth.getLastAuth()
                        res.onSuccess { result ->
                            accessToken = result!!.accessToken
                            refreshToken = result.refreshToken
                            idToken = result.idToken
                        }
                    }
                    println("Authentication successful!")
                } else {
                    println("Authentication failed!")

                }
            }
        }
        val authLogout = RememberLogoutOpenId {
            println("Logout result $it")
            it?.let { isLogout ->
                if (isLogout) {
                    accessToken = ""
                    refreshToken = ""
                    idToken = ""

                    println("Logout successful!")
                } else {
                    println("Logout failed!")

                }


            }
        }

        Surface(
            modifier = Modifier.fillMaxSize()
                .navigationBarsPadding()
                .imePadding(),
            color = Color.White,
        ) {
            MaterialTheme(colorScheme = lightColorScheme()) {
                LazyColumn(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    item {
                        Spacer(Modifier.height(20.dp))
                        Button(onClick = {
                            scope.launch {
                                val res = OpenIdService.getAuth.getLastAuth()
                                res.onSuccess {
                                    println("last auth token ${it?.accessToken}")
                                    if (it != null) {
                                        accessToken = it.accessToken
                                        refreshToken = it.refreshToken
                                        idToken = it.idToken
                                    }
                                }
                            }
                        }) {
                            Text("Get data")
                        }
                        Button(onClick = {
                            scope.launch {
                                authRes.launch(OpenIdService.getAuth)
                            }

                        }) {
                            Text("Login")
                        }
                        Button(
                            enabled = refreshToken.isNotEmpty(),
                            onClick = {
                                try {
                                    println("Attempting to refresh token")
                                    scope.launch {

                                        val result = OpenIdService.getAuth.refreshToken()
                                        result.onSuccess { isAuthenticated ->
                                            println("Authentication successful!")
                                            scope.launch {

                                                val newAuth = OpenIdService.getAuth.getLastAuth()
                                                newAuth.onSuccess {
                                                    if (it != null) {
                                                        println("Login success ${it.accessToken}")
                                                        println("refresh token is ${it.refreshToken}")
                                                        refreshToken = it.refreshToken
                                                        idToken = it.idToken
                                                        accessToken = it.accessToken
                                                    } else {
                                                        println("Login failed: result is null")
                                                    }
                                                }
                                            }

                                        }.onFailure { error ->
                                            println("Authentication error: ${error.message}")
                                        }
                                    }
                                } catch (e: Exception) {
                                    println("Refresh token failed: ${e.message}")
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
                                    authLogout.launch(OpenIdService.getAuth)
                                }
                            }) {
                            Text("Logout")
                        }
                        Spacer(modifier = Modifier.height(30.dp))
                        Button(onClick = {
                            scope.launch {
                                try {
                                    val result = apiService.testApi()
                                    println("data api is $result")
                                } catch (e: Exception) {
                                    println("error is ${e.message}")
                                }
                            }
                        }) {
                            Text("Test Api")
                        }

                    }
                }
            }
        }
    }

}

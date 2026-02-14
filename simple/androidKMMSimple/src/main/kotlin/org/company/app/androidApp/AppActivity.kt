package org.company.app.androidApp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.kmmcrypto.AndroidKMMCrypto
import io.github.kmmopenid.AuthEvent
import io.github.kmmopenid.AuthState
import io.github.kmmopenid.LoginViewModel
import io.github.kmmopenid.kappauthcmp.RememberAuthOpenId
import io.github.kmmopenid.kappauthcmp.RememberLogoutOpenId
import io.github.kmmopenid.services.OpenIdService
import io.github.openid.AuthResult
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.compose.koinViewModel

class AppActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        AndroidKMMCrypto.init("key0")
        setContent {
            val openIdServices = OpenIdService()
            val loginViewModel: LoginViewModel = koinViewModel()
            var userInfo by remember { mutableStateOf<AuthResult?>(null) }

            var startAuth by rememberSaveable { mutableStateOf(false) }
            var startLogout by rememberSaveable { mutableStateOf(false) }

            val authRes = RememberAuthOpenId(
                authorizationRequest = openIdServices.getAuthorizationRequest(),
                onAuthResult = loginViewModel::onLoginResult
            )

            val authLogout = RememberLogoutOpenId(
                openIdServices.getAuthorizationRequest(),
                onLogoutResult = loginViewModel::onLogoutResult
            )

            LaunchedEffect(Unit) {
                loginViewModel.states.collectLatest { state ->
                    when (state) {
                        is AuthState.LoginSuccess -> {
                            userInfo = state.authResult
                        }

                        is AuthState.LogoutSuccess -> {
                            userInfo = null
                        }

                        is AuthState.LoginFailed -> {
                            println("Login failed: ${state.message}")
                        }

                        is AuthState.LogoutFailed -> {
                            println("Logout failed: ${state.message}")
                        }
                    }
                }
            }

            LaunchedEffect(Unit) {
                loginViewModel.events.collectLatest { event ->
                    when (event) {
                        AuthEvent.StartOpenIdLogin -> startAuth = true
                        AuthEvent.StartOpenIdLogout -> startLogout = true
                    }
                }
            }

            LaunchedEffect(startAuth, startLogout) {
                if (startAuth) {
                    authRes.launch()
                    startAuth = false
                }
                if (startLogout) {
                    authLogout.launch()
                    startLogout = false
                }
            }

            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding()
                    .imePadding(),
                color = Color.White,
            ) {
                MaterialTheme(colorScheme = lightColorScheme()) {
                    val isLoggedIn by loginViewModel.isLoggedIn.collectAsState()

                    LazyColumn(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        item {
                            Spacer(Modifier.height(20.dp))
                            Button(onClick = { loginViewModel.getUserData() }) {
                                Text("Get data")
                            }
                            Button(onClick = { loginViewModel.login() }) {
                                Text("Login")
                            }
                            Button(
                                enabled = userInfo?.refreshToken?.isNotEmpty() == true,
                                onClick = { loginViewModel.refreshToken(openIdServices.getTokenRequest()) }
                            ) {
                                Text("Refresh Token")
                            }

                            Spacer(Modifier.height(10.dp))
                            Text("access token ${userInfo?.accessToken}", style = TextStyle(fontSize = 10.sp))
                            Spacer(Modifier.height(10.dp))
                            Text("refresh token ${userInfo?.refreshToken}", style = TextStyle(fontSize = 10.sp))
                            Spacer(Modifier.height(10.dp))
                            Text("id token ${userInfo?.idToken}", style = TextStyle(fontSize = 10.sp))


                            Spacer(Modifier.height(50.dp))
                            Button(
                                enabled = isLoggedIn,
                                onClick = { loginViewModel.logout() }
                            ) {
                                Text("Logout")
                            }
                            Spacer(modifier = Modifier.height(30.dp))
                            Button(onClick = { loginViewModel.getApiCall() }) {
                                Text("Test Api")
                            }
                        }
                    }
                }
            }
        }
    }
}
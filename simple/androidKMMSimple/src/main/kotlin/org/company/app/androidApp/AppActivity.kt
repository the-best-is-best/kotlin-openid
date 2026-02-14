package org.company.app.androidApp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.lifecycleScope
import io.github.kmmcrypto.AndroidKMMCrypto
import io.github.kmmopenid.Event
import io.github.kmmopenid.LoginViewModel
import io.github.openid.AndroidOpenId
import io.github.openid.AuthOpenId
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.compose.koinViewModel

@Composable
fun AuthResultObserver(viewModel: LoginViewModel) {
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.getUserData()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
}

class AppActivity : ComponentActivity() {
    private val loginViewModel: LoginViewModel by inject()

    // ✅ FIXED: Launchers MUST be defined here at the class level
    private val loginLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        lifecycleScope.launch {
            AndroidOpenId().handleAuthResult(result)
            // Trigger UI refresh after login completes
            loginViewModel.getUserData()
        }
    }

    private val logoutLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        lifecycleScope.launch {
            // Refresh state after logout
            val isSuccessful = AndroidOpenId().handleLogoutResult(result)
            if (isSuccessful) {
                loginViewModel.completeLogout()
            }

        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize Context Bridge & Crypto
        AuthOpenId().init("key", "group")
        AndroidKMMCrypto.init("key0")

        setContent {
            CompositionLocalProvider(
                LocalAuthLaunchers provides AuthLaunchers(
                    onLogin = { loginLauncher.launch(it) },
                    onLogout = { logoutLauncher.launch(it) }
                )
            ) {
                AuthScreen()
            }
        }
    }
}

@Composable
fun AuthScreen() {
    val loginViewModel: LoginViewModel = koinViewModel()

    AuthResultObserver(viewModel = loginViewModel)

    // Collect State from ViewModel
    val userInfo by loginViewModel.userInfo.collectAsState()
    val scope = rememberCoroutineScope()
    val launchers = LocalAuthLaunchers.current

    // ✅ Collect Events to trigger the class-level launchers
    LaunchedEffect(Unit) {
        loginViewModel.event.collectLatest { event ->
            when (event) {
                is Event.LaunchLogin -> launchers.onLogin(event.intent)
                is Event.LaunchLogout -> launchers.onLogout(event.intent)
            }
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
            LazyColumn(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                item {
                    Spacer(Modifier.height(20.dp))
                    Button(onClick = { loginViewModel.getUserData() }) {
                        Text("Get data")
                    }
                    Button(onClick = {
                        scope.launch { loginViewModel.login() }
                    }) {
                        Text("Login")
                    }
                    Button(
                        enabled = userInfo?.refreshToken?.isNotEmpty() == true,
                        onClick = { loginViewModel.refreshToken() }
                    ) {
                        Text("Refresh Token")
                    }

                    Spacer(Modifier.height(10.dp))
                    Text(
                        "access token ${userInfo?.accessToken}",
                        style = TextStyle(fontSize = 10.sp)
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "refresh token ${userInfo?.refreshToken}",
                        style = TextStyle(fontSize = 10.sp)
                    )
                    Spacer(Modifier.height(10.dp))
                    Text("id token ${userInfo?.idToken}", style = TextStyle(fontSize = 10.sp))

                    Spacer(Modifier.height(50.dp))
                    Button(
                        enabled = userInfo?.idToken?.isNotEmpty() == true,
                        onClick = {
                            scope.launch { loginViewModel.logout() }
                        }
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

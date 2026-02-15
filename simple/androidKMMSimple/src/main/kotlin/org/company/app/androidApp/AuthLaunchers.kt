package org.company.app.androidApp

import android.content.Intent
import androidx.compose.runtime.staticCompositionLocalOf

// 1. Data class to hold your launch logic
data class AuthLaunchers(
    val onLogin: (Intent) -> Unit,
    val onLogout: (Intent) -> Unit
)

// 2. The "Key" used to look up these functions in Compose
val LocalAuthLaunchers = staticCompositionLocalOf<AuthLaunchers> {
    error("No AuthLaunchers provided! Check your Activity setContent.")
}

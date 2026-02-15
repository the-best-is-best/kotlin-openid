# kappAuthCMP (KApp Auth)

Optional **Compose Multiplatform** helper for [kmmOpenId](../kmmOpenId/README.md). It provides
composables that handle platform-specific auth flows (Activity result on Android, native auth on
iOS) so you can share login and logout UI between Android and iOS.

**Maven coordinates:** `io.github.the-best-is-best:kapp-auth-cmp:7.0.0`

**Requires:** [kmmOpenId](../kmmOpenId/README.md) (core KApp Auth). Add this module only if you use*
*Compose Multiplatform** for shared UI. For native UI (Jetpack Compose + SwiftUI), use **kmmOpenId**
alone.

---

## Supported platforms

- **Android** (Compose, minSdk 24)
- **iOS** (iosArm64, iosSimulatorArm64)

---

## Installation

Add both the core library and the CMP helper in the module where you use Compose:

```kotlin
// In your shared UI / Compose module (e.g. build.gradle.kts)
kotlin {
    sourceSets {
        commonMain.dependencies {
            api("io.github.the-best-is-best:kapp-auth:7.0.0")
            api("io.github.the-best-is-best:kapp-auth-cmp:7.0.0")
        }
    }
}
```

---

## Initialization

Call once at app startup, before any auth calls. Same as [kmmOpenId](../kmmOpenId/README.md).

**Android** (e.g. in `Application.onCreate()` or main `Activity.onCreate()`):

```kotlin
AuthOpenId().init("your_storage_key", "your_keychain_group")
```

**iOS** (e.g. in `App` init or first view’s `onAppear`):

```kotlin
AuthOpenId().doInit(key: "your_storage_key", group: "your_keychain_group")
```

---

## Composables

### RememberAuthOpenId

Returns a state object with a `launch()` function that starts the OAuth/OpenID login flow. The
result is reported via callback.

| Parameter              | Description                                                                                                                     |
|------------------------|---------------------------------------------------------------------------------------------------------------------------------|
| `authorizationRequest` | From [kmmOpenId](../kmmOpenId/README.md): clientId, redirectUrl, issuer, discoveryUrl, scope, authorizationServiceConfiguration |
| `onAuthResult`         | Callback: `Boolean?` — `true` = success, `false` = failed, `null` = cancelled                                                   |

```kotlin
@Composable
fun LoginScreen(authorizationRequest: io.github.openid.AuthorizationRequest) {
    val scope = rememberCoroutineScope()
    val authState = RememberAuthOpenId(
        authorizationRequest = authorizationRequest,
        onAuthResult = { success -> /* handle result */ }
    )

    Button(onClick = { scope.launch { authState.launch() } }) {
        Text("Login")
    }
}
```

### RememberLogoutOpenId

Returns a state object with a `launch()` function that starts the end-session (logout) flow.

| Parameter              | Description                                                                                      |
|------------------------|--------------------------------------------------------------------------------------------------|
| `authorizationRequest` | Same as above; must include `authorizationServiceConfiguration.postLogoutRedirectURL` for logout |
| `onLogoutResult`       | Callback: `Boolean?` — outcome of the logout flow                                                |

```kotlin
@Composable
fun LogoutButton(authorizationRequest: io.github.openid.AuthorizationRequest) {
    val scope = rememberCoroutineScope()
    val logoutState = RememberLogoutOpenId(
        authorizationRequest = authorizationRequest,
        onLogoutResult = { success -> /* handle result */ }
    )

    Button(onClick = { scope.launch { logoutState.launch() } }) {
        Text("Logout")
    }
}
```

---

## Usage notes

- **AuthorizationRequest** and **AuthorizationServiceConfig** use the same data types
  as [kmmOpenId](../kmmOpenId/README.md). Build them in shared code (e.g. in your ViewModel or
  OpenID service) and pass into the composables.
- On Android, the composables use `rememberLauncherForActivityResult` and Custom Tabs for the auth
  browser; on iOS they use the native AppAuth flow.
- You need a `CoroutineScope` (e.g. `rememberCoroutineScope()`) to call `launch()` on the state
  objects.

---

## Repository

- **GitHub:** [the-best-is-best/kotlin-openid](https://github.com/the-best-is-best/kotlin-openid)
- **License:** Apache-2.0

For the core API (login/logout intents, refresh, `getLastAuth()`), see the *
*[kmmOpenId](../kmmOpenId/README.md)** module.

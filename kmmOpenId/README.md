# kmmOpenId (KApp Auth)

Core Kotlin Multiplatform library for OAuth 2.0 and OpenID Connect. It abstracts
the [Android AppAuth](https://github.com/openid/AppAuth-Android) and iOS AppAuth SDKs behind a
shared API so you can use one codebase for authentication on both platforms.

**Maven coordinates:** `io.github.the-best-is-best:kapp-auth:7.0.0`

This module has **no UI dependencies**. Use it from shared KMP logic (ViewModels, use cases, etc.)
and plug into either native UI (Jetpack Compose / SwiftUI) or shared Compose Multiplatform UI.

---

## Supported platforms

- **Android** (minSdk 24)
- **iOS** (iosArm64, iosSimulatorArm64)

---

## Installation

```kotlin
// In your shared KMP module (e.g. build.gradle.kts)
kotlin {
    sourceSets {
        commonMain.dependencies {
            api("io.github.the-best-is-best:kapp-auth:7.0.0")
        }
    }
}
```

---

## Initialization

Call once at app startup, before any auth calls.

**Android** (e.g. in `Application.onCreate()` or main `Activity.onCreate()`):

```kotlin
AuthOpenId().init("your_storage_key", "your_keychain_group")
```

**iOS** (e.g. in `App` init or first view’s `onAppear`):

```kotlin
AuthOpenId().doInit(key = "your_storage_key", group = "your_keychain_group")
```

---

## Common API (Android & iOS)

| Method                       | Description                                                               |
|------------------------------|---------------------------------------------------------------------------|
| `init(key, group)`           | Initialize storage key and keychain/Keystore group. Call once at startup. |
| `refreshToken(tokenRequest)` | Refresh tokens. Returns `Result<AuthResult>`.                             |
| `getLastAuth()`              | Get last stored auth. Returns `Result<AuthResult?>`.                      |

---

## Platform-specific flows

Login and logout are **not** in the common API: Android returns intents for the UI to launch; iOS
exposes suspend functions that run the flow and return a result.

### Android

- **Login:** `AuthOpenId().getLoginIntent(authorizationRequest)` returns an `Intent`. Start it with
  `startActivityForResult` / `ActivityResultLauncher`, then pass the activity result to
  `AndroidOpenId().handleAuthResult(result)` to complete the flow and persist tokens.
- **Logout:** `AuthOpenId().getLogoutIntent(authorizationRequest)` is a **suspend** function that
  returns an `Intent`. Launch it the same way and use `AndroidOpenId().handleLogoutResult(result)`
  for the outcome.

### iOS

- **Login:** `AuthOpenId().login(authorizationRequest)` is a **suspend** function that returns
  `Result<AuthResult>`. Tokens are persisted inside the library. Call it from a coroutine (e.g.
  `scope.launch { authOpenId.login(request).getOrThrow() }`).
- **Logout:** `AuthOpenId().logout(authorizationRequest)` is a **suspend** function that returns
  `Result<Boolean>`. Local data is cleared on success. Call it from a coroutine.

---

## Data types (common)

- **`AuthorizationRequest`** — clientId, redirectUrl, issuer, discoveryUrl, scope,
  authorizationServiceConfiguration
- **`AuthorizationServiceConfig`** — authorizationEndpoint, tokenEndpoint, endSessionEndpoint,
  postLogoutRedirectURL, registerEndPoint
- **`TokenRequest`** — issuer, tokenEndpoint, clientId, clientSecret (optional)
- **`AuthResult`** — accessToken, refreshToken, idToken

---

## Repository

- **GitHub:** [the-best-is-best/kotlin-openid](https://github.com/the-best-is-best/kotlin-openid)
- **License:** Apache-2.0

For Compose Multiplatform helpers (`RememberAuthOpenId` / `RememberLogoutOpenId`), use the *
*kappAuthCMP** module.

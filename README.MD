# KAppAuth

This package provides a Kotlin Multiplatform abstraction around the Android and iOS AppAuth SDKs, making it easy to communicate with OAuth 2.0 and OpenID Connect providers.

## Project Structure & Philosophy

This project demonstrates two different UI strategies for a Kotlin Multiplatform project, each with a specific library dependency chain.

### Library Modules

1.  **:kmmOpenId** (`io.github.the-best-is-best:kapp-auth`)
    *   The **core KMP library** containing the platform-specific authentication logic. It has no UI dependencies. **It is the required library for native iOS (SwiftUI) development.**
2.  **:kappAuthCMP** (`io.github.the-best-is-best:kapp-auth-cmp`)
    *   An **optional helper library for Compose**. It provides the `RememberAuthOpenId` composable. **It is the required library for Android (Jetpack Compose) or shared Compose Multiplatform UI development.**

### Business Logic Module

*   **:simpleKMPPorject**: This is the shared KMP module that contains all the business logic, like the `LoginViewModel`. It depends on the core `:kmmOpenId` library.

### Demo Apps

*   **Native UI Approach**: An Android app using Jetpack Compose (`:simple:androidKMMSimple`) and an iOS app using SwiftUI (`:simple:iosAppKMP`).
*   **Shared UI Approach**: A shared Compose Multiplatform UI module (`:simple:sharedUI`) hosted inside a native iOS app (`:simple:iosApp`).

---

## Core Setup (Required for All Approaches)

This setup is done in your shared KMP business logic module (e.g., `:simpleKMPPorject`).

#### 1. Configure Gradle Dependencies

Your shared logic module must depend on the core `:kmmOpenId` library.

```kotlin
// in simple/simpleKMPPorject/build.gradle.kts
kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(project(":kmmOpenId"))
                api(libs.androidx.lifecycle.viewmodel)
                api(libs.koin.core)
            }
        }
    }
}
```

#### 2. Create a Shared `LoginViewModel`

This ViewModel in `commonMain` orchestrates the authentication logic.

```kotlin
// in simpleKMPPorject/src/commonMain/kotlin/io/github/kmmopenid/LoginViewModel.kt
@KoinViewModel
class LoginViewModel(...) : ViewModel() { /* ... */ }
```

#### 3. Initialize the Library on App Startup

You must initialize the library to configure encrypted data storage.

**Android (`App.kt`)**
```kotlin
// in simple/androidKMMSimple/src/main/kotlin/org/company/app/androidApp/App.kt
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin { androidContext(this@App) }
        AuthOpenId().init("key", "group")
    }
}
```

**iOS (`iosApp.swift`)**
```swift
// in simple/iosAppKMP/iosApp/iosApp.swift (Native UI example)
@main
struct iOSApp: App {
    init() { KoinModuleIOS().doInitKoinIOS() }
    var body: some Scene {
        WindowGroup {
            ContentView().onAppear { AuthOpenId().doInit(key: "key", group: "group") }
        }
    }
}
```

---

## UI Integration Guides

### Approach 1: Native UIs (Jetpack Compose & SwiftUI)

This is the classic KMP approach where UI is native to each platform.

#### Android (Jetpack Compose)

The Android app (`:simple:androidKMMSimple`) **must** use the `:kappAuthCMP` module for its Composable helpers.

```kotlin
// in simple/androidKMMSimple/build.gradle.kts
implementation(project(":simple:simpleKMPPorject"))
implementation(project(":kappAuthCMP")) // <<< Required for RememberAuthOpenId

// in simple/androidKMMSimple/src/main/kotlin/org/company/app/androidApp/AppActivity.kt
val authLauncher = RememberAuthOpenId(
    authorizationRequest = openIdServices.getAuthorizationRequest(),
    onAuthResult = loginViewModel::onLoginResult
)
```

#### iOS (SwiftUI)

The native iOS app (`:simple:iosAppKMP`) uses **only** the core `:kmmOpenId` library and requires a `UIViewControllerRepresentable` bridge.

```swift
// in simple/iosAppKMP/iosApp/iosApp.swift

// 1. Create a UIViewControllerRepresentable to bridge the auth call.
struct AuthView: UIViewControllerRepresentable {
    func updateUIViewController(_ vc: UIViewController, context: Context) {
        if startAuth {
            // 2. Call the login function from the core KMP library directly
            AuthOpenId().login(...) { result, error in
                // ...
            }
        }
    }
}

// 3. In your main ContentView, use a ZStack to host the bridge.
struct ContentView: View {
    var body: some View {
        ZStack {
            VStack { Button("Login") { ... } }
            // The hidden bridge view is triggered by state changes
            AuthView(...).frame(width: 0, height: 0)
        }
    }
}
```

### Approach 2: Shared UI (Compose Multiplatform)

This approach shares UI code between Android and iOS. This requires the **:kappAuthCMP** module in the `commonMain` of your shared UI module.

**Shared UI Module (e.g., `:simple:sharedUI`)**

Your shared UI module must depend on `:kappAuthCMP` to use `RememberAuthOpenId` in `commonMain`.

```kotlin
// In a shared Composable screen in commonMain
@Composable
fun LoginScreen() {
    // Use RememberAuthOpenId here, same as the native Android example
    val authLauncher = RememberAuthOpenId(...)
}
```

**iOS Host App (`:simple:iosApp`)**

The iOS host app's only job is to display the shared Composable screen. It does not need to know about the auth libraries directly.

```swift
// in simple/iosApp/iosApp/iosApp.swift

// 1. Bridge to the Compose UIViewController created in your shared module's iosMain
struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        return MainKt.MainViewController() // This function returns your Compose UI
    }
}

// 2. Display the view
struct ContentView: View {
    var body: some View {
        ComposeView().ignoresSafeArea(.all)
    }
}
```

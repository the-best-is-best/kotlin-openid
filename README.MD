# KApp Auth (Kotlin OpenID)

Kotlin Multiplatform authentication for **OAuth 2.0** and **OpenID Connect**. This repo provides a
shared API over the [Android AppAuth](https://github.com/openid/AppAuth-Android) and iOS AppAuth
SDKs so you can use one codebase on both platforms.

---

## Libraries

| Module                                   | Maven artifact                                   | Description                                                                                                                |
|------------------------------------------|--------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------|
| **[kmmOpenId](kmmOpenId/README.md)**     | `io.github.the-best-is-best:kapp-auth:7.0.0`     | Core KMP auth library. No UI — use from shared logic (ViewModels, etc.) and wire to native or Compose UI.                  |
| **[kappAuthCMP](kappAuthCMP/README.md)** | `io.github.the-best-is-best:kapp-auth-cmp:7.0.0` | Optional Compose Multiplatform helpers (`RememberAuthOpenId`, `RememberLogoutOpenId`) for shared UI. Depends on kmmOpenId. |

- **Native UI** (Jetpack Compose on Android + SwiftUI on iOS): use **kmmOpenId** only; handle
  intents/callbacks in platform code.
- **Shared UI** (Compose Multiplatform): add **kappAuthCMP** and use the composables in
  `commonMain`.

---

## Sample structure (this repo)

- **`:simple:simpleKMPPorject`** — Shared KMP logic (e.g. ViewModels, OpenID config). Depends on
  `:kmmOpenId`.
- **`:simple:sharedUI`** — Shared Compose UI; can depend on `:kappAuthCMP` and
  `:simple:simpleKMPPorject`.
- **`:simple:androidKMMSimple`** — Android app (native Compose).
- **`:simple:androidSimple`** — Android app variant.

iOS apps (e.g. SwiftUI or Compose host) can depend on the same shared modules; see the library
READMEs for platform setup.

---

## Quick start

1. **Add the core library** (and optionally the CMP helper) in your shared module:

   ```kotlin
   kotlin {
       sourceSets {
           commonMain.dependencies {
               api("io.github.the-best-is-best:kapp-auth:7.0.0")
               // api("io.github.the-best-is-best:kapp-auth-cmp:7.0.0")  // only for Compose Multiplatform
           }
       }
   }
   ```

2. **Initialize at app startup** (before any auth calls):
   - **Android:** `AuthOpenId().init("your_storage_key", "your_keychain_group")`
   - **iOS:** `AuthOpenId().doInit(key: "your_storage_key", group: "your_keychain_group")`

3. **Use the API** for login, logout, refresh, and `getLastAuth()` — see *
   *[kmmOpenId/README.md](kmmOpenId/README.md)** for the full API and platform-specific flows, and *
   *[kappAuthCMP/README.md](kappAuthCMP/README.md)** for Compose usage.

---

## License

Apache-2.0. See [LICENSE](LICENSE) if present.

**Repository:** [the-best-is-best/kotlin-openid](https://github.com/the-best-is-best/kotlin-openid)

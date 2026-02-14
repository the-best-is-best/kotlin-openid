import SwiftUI
import Combine
import SimpleKMPProject

// MARK: - 1. Flow Interop Helpers
class FlowCollector<T>: Kotlinx_coroutines_coreFlowCollector {
    let callback: (T) -> Void
    init(callback: @escaping (T) -> Void) { self.callback = callback }

    func emit(value: Any?, completionHandler: @escaping (Error?) -> Void) {
        if let value = value as? T { callback(value) }
        completionHandler(nil)
    }
}

extension Kotlinx_coroutines_coreFlow {
    func asPublisher<T>(type: T.Type) -> AnyPublisher<T, Never> {
        let subject = PassthroughSubject<T, Never>()
        self.collect(collector: FlowCollector<T> { value in
            subject.send(value)
        }) { _ in }
        return subject.eraseToAnyPublisher()
    }
}

// MARK: - 2. UIViewController Finder
func getActiveViewController() -> UIViewController? {
    let scenes = UIApplication.shared.connectedScenes
    let windowScene = scenes.first as? UIWindowScene
    var topController = windowScene?.windows.first(where: { $0.isKeyWindow })?.rootViewController
    
    while let presented = topController?.presentedViewController {
        topController = presented
    }
    return topController
}

// MARK: - 3. The Main App Entry
@main
struct iOSApp: App {
    init() {
        KoinModuleIOS().doInitKoinIOS()
        // We doInit later in .onAppear to ensure the Window exists
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}

// MARK: - 4. The UI Logic
struct ContentView: View {
    @StateObject private var observable = LoginObservable()

    var body: some View {
        ZStack {
            // Main Content Layer
            ScrollView {
                VStack(spacing: 20) {
                    Text("KMP Auth Demo").font(.headline)
                    
                    Button("Login") { observable.login() }
                        .buttonStyle(.borderedProminent)
                    
                    Button("Get User Data") { observable.getUserData() }
                        .buttonStyle(.bordered)

                    VStack(alignment: .leading, spacing: 8) {
                        Text("Access Token:").bold()
                        Text(observable.userInfo?.accessToken ?? "No Token").font(.caption2).foregroundColor(.gray)
                    }
                    .padding().background(Color.gray.opacity(0.1)).cornerRadius(8)

                    Button("Logout") { observable.logout() }
                        .foregroundColor(.red)
                        .disabled(!observable.isLoggedIn)
                    
                    Button("Test API") { observable.getApiCall() }
                }
                .padding()
            }

            // The Hidden Auth Bridge Layer
            AuthView(
                startAuth: $observable.startAuth,
                startLogout: $observable.startLogout,
                loginViewModel: observable.vm
            )
            .frame(width: 0, height: 0)
        }
        .onAppear {
            // Initialize here so the View Controller is ready
            AuthOpenId().doInit(key: "key", group: "group")
            observable.getUserData()
        }
    }
}

// MARK: - 5. The Auth Bridge (Representable)
struct AuthView: UIViewControllerRepresentable {
    @Binding var startAuth: Bool
    @Binding var startLogout: Bool
    let loginViewModel: LoginViewModel

    func makeUIViewController(context: Context) -> UIViewController {
        return UIViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {
        let authOpenId = AuthOpenId()
        let openIdService = OpenIdService()

        if startAuth {
            // Use the helper to find where the browser should pop up
            if getActiveViewController() != nil {
                DispatchQueue.main.async { startAuth = false }
                
                // If your Kotlin login method supports passing a VC, do it.
                // If not, this helper ensures the window is focused.
                authOpenId.login(authorizationRequest: openIdService.getAuthorizationRequest()) { result, error in
                    let success = (result as? KotlinBoolean)?.boolValue ?? false
                    loginViewModel.onLoginResult(success: KotlinBoolean(bool: success))
                }
            }
        }

        if startLogout {
            if getActiveViewController() != nil {
                DispatchQueue.main.async { startLogout = false }
                
                authOpenId.logout(authorizationRequest: openIdService.getAuthorizationRequest()) { result, error in
                    // PRINT THIS. It will tell you the REAL problem (Redirect URI, id_token, etc.)
                    if let nsError = error {
                        print("DEBUG: NATIVE IOS ERROR: \(nsError.localizedDescription)")
                        print("DEBUG: ERROR CODE: \((nsError as NSError).code)")
                    }

                    let success = (result as? KotlinBoolean)?.boolValue ?? false
                    loginViewModel.onLogoutResult(success: KotlinBoolean(bool: success))
                }
            }
        }
    }
}

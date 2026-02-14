import SwiftUI
import Combine
import SimpleKMPProject

class LoginObservable: ObservableObject {
    let vm: LoginViewModel = KoinHelper.shared.getLoginViewModel()
    
    @Published var userInfo: AuthResult? = nil
    @Published var startAuth = false
    @Published var startLogout = false
    
    // We observe this to enable/disable the logout button
    @Published var isLoggedIn: Bool = false
    
    private var cancellables = Set<AnyCancellable>()
    
    init() {
        observeStates()
        observeEvents()
        
        // Observe the isLoggedIn StateFlow from Kotlin
        vm.isLoggedIn.asPublisher(type: KotlinBoolean.self)
            .receive(on: DispatchQueue.main)
            .sink { [weak self] value in
                self?.isLoggedIn = value.boolValue
            }
            .store(in: &cancellables)
    }
    
    private func observeStates() {
        vm.states.asPublisher(type: AuthState.self)
            .receive(on: DispatchQueue.main)
            .sink { [weak self] state in
                print("Received State: \(state)") // Debugging line
                
                if let success = state as? AuthStateLoginSuccess {
                    self?.userInfo = success.authResult
                } else if state is AuthStateLogoutSuccess {
                    // FORCE CLEAR HERE
                    self?.userInfo = nil
                    self?.isLoggedIn = false
                    print("Logout detected: Clearing userInfo")
                } else if let failed = state as? AuthStateLogoutFailed {
                    print("Logout failed: \(failed.message)")
                }
            }
            .store(in: &cancellables)
    }
    private func observeEvents() {
        vm.events.asPublisher(type: AuthEvent.self)
            .receive(on: DispatchQueue.main)
            .sink { [weak self] event in
                if event is AuthEventStartOpenIdLogin {
                    self?.startAuth = true
                } else if event is AuthEventStartOpenIdLogout {
                    self?.startLogout = true
                }
            }
            .store(in: &cancellables)
    }

    // Native wrappers for ViewModel methods
    func login() { vm.login() }
    func logout() { vm.logout() }
    func getUserData() { vm.getUserData() }
    func refreshToken() {
        let tokenRequest = OpenIdService().getTokenRequest()
        vm.refreshToken(tokenRequest: tokenRequest)
    }
    func getApiCall() { vm.getApiCall() }
}

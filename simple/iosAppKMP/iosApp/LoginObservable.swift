import SwiftUI
import Combine
import SimpleKMPProject

class LoginObservable: ObservableObject {
    let vm: LoginViewModel = KoinHelper.shared.getLoginViewModel()
    
    @Published var userInfo: AuthResult? = nil
//    @Published var isLoggedIn: Bool = false

    // Use these as simple local triggers for the AuthView
//    @Published var startAuth = false
//    @Published var startLogout = false
    
    private var cancellables = Set<AnyCancellable>()
    
    init() {
        // Observe the ONLY flow available in your Kotlin: userInfo
        vm.userInfo.asPublisher(type: AnyObject.self)
        .receive(on: DispatchQueue.main)
        .sink { [weak self] info in
            // Cast it here to check for nil safely
            let authData = info as? AuthResult

            self?.userInfo = authData

            print("UserInfo Updated. Logged In: \(authData != nil)")
        }
        .store(in: &cancellables)
    }

    // Instead of waiting for a Kotlin event, we trigger the bridge directly
    func login() {
        vm.login()
    }

    func logout() {
        vm.logout()
    }

    func getUserData() { vm.getUserData() }

    func refreshToken() {
        vm.refreshToken()
    }

    func getApiCall() { vm.getApiCall() }
}

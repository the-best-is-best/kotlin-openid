import SwiftUI
import Combine
import SimpleKMPProject

@MainActor
class LoginObservable: ObservableObject {
    private let vm = Koin.shared.get(objCClass: LoginViewModel.self) as! LoginViewModel
    
    @Published var userInfo: AuthResult? = nil
//    @Published var isLoggedIn: Bool = false

    // Use these as simple local triggers for the AuthView
//    @Published var startAuth = false
//    @Published var startLogout = false
    
    private var cancellables = Set<AnyCancellable>()
    
    init() {
        Task { @MainActor in
            for await data in vm.userInfo {
                self.userInfo = data
            }
        }

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

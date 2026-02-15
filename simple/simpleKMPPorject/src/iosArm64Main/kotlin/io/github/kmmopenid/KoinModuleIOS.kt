package io.github.kmmopenid

import io.github.kmmopenid.api.KtorServices
import io.github.kmmopenid.di.AppModule
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

fun initKoin(appDeclaration: KoinAppDeclaration = {}) = startKoin {
    appDeclaration()
    modules(manualModule + AppModule().module)
}

class KoinModuleIOS {
    fun initKoinIOS(){
        initKoin()
    }
}

val manualModule = module {
    // 1. Tell Koin how to make the custom class
    factory { KtorServices() }

}
object KoinHelper : KoinComponent {
    fun getLoginViewModel(): LoginViewModel = get()
}
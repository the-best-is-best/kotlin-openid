package io.github.kmmopenid

import io.github.kmmopenid.api.KtorServices
import io.github.kmmopenid.di.AppModule
import io.github.openid.AndroidOpenId
import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

fun initKoin(appDeclaration: KoinAppDeclaration = {}) = startKoin {
    appDeclaration()
    modules(manualModule + AppModule().module)
}

val manualModule = module {
    // 1. Tell Koin how to make the custom class
    factory { AndroidOpenId() }
    factory { KtorServices() }

}
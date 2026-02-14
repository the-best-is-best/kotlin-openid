package io.github.kmmopenid.di

import io.github.tbib.koingeneratorannotations.Module
import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

@Module
interface AppModule {
    val module: org.koin.core.module.Module
}

fun initKoin(appDeclaration: KoinAppDeclaration = {}) = startKoin {
    appDeclaration()
    modules(AppModule().module)
}


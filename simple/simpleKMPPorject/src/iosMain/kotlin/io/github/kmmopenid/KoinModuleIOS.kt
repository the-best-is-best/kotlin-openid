package io.github.kmmopenid

import io.github.kmmopenid.di.initKoin
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ObjCClass
import kotlinx.cinterop.getOriginalKotlinClass
import org.koin.core.Koin
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.qualifier.StringQualifier
import kotlin.reflect.KClass

class KoinModuleIOS {
    fun initKoinIOS(){
        initKoin()
    }
}

object KoinHelper : KoinComponent {
    fun getLoginViewModel(): LoginViewModel = get()
}
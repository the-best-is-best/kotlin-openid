package io.github.kmmopenid.di

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ObjCClass
import kotlinx.cinterop.getOriginalKotlinClass
import org.koin.core.component.KoinComponent
import kotlin.reflect.KClass

@OptIn(BetaInteropApi::class)
object Koin {

    // 1. الدالة الأساسية اللي بتكلم Koin (بتاخد KClass)
    // خليناها private عشان السويفت ميتلخبطش بينهم، والكوتلن يستخدمها داخلياً
    private fun <T : Any> getFromKoin(klass: KClass<T>): T {
        return object : KoinComponent {}.getKoin().get(klass, null, null)
    }

    // 2. الدالة اللي السويفت هيناديها (بتاخد ObjCClass)
    fun <T : Any> get(objCClass: ObjCClass): T {
        val klass = getOriginalKotlinClass(objCClass) as? KClass<T>
            ?: throw IllegalArgumentException("Cannot find Kotlin class for $objCClass")
        return getFromKoin(klass) // بننادي الدالة الأولى بوضوح
    }
}
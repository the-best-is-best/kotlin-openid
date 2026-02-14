package org.company.app.androidApp

import android.app.Application
import io.github.kmmcrypto.AndroidKMMCrypto
import io.github.kmmopenid.di.initKoin
import io.github.openid.AuthOpenId
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger

class App : Application(){
    override fun onCreate() {
        super.onCreate()
        initKoin {
            androidContext(this@App)
            androidLogger()
        }
//        AndroidKMMCrypto.init("key0")
        AuthOpenId().init("key", "group")
    }

}
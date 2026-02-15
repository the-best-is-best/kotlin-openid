package org.company.app.androidApp

import android.app.Application
import io.github.kmmopenid.initKoin
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger

class App : Application(){
    override fun onCreate() {
        super.onCreate()
        initKoin {
            androidContext(this@App)
            androidLogger()
        }

    }

}
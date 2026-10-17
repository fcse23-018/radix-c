package com.terrochroma.app

import android.app.Application
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class TerroChromaApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidContext(this@TerroChromaApplication)
            modules(appModule)
        }
    }
}

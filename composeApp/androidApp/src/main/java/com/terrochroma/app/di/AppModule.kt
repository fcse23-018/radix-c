package com.terrochroma.app.di

import android.content.Context
import com.terrochroma.shared.config.PreferencesStorage
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val appModule = module {
    single { PreferencesStorage(androidContext()) }
}

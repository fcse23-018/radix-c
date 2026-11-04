package com.soilcarbon.engine.core

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers as KotlinDispatchers
import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DefaultDispatcher

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class MainDispatcher

/**
 * Small indirection over [kotlinx.coroutines.Dispatchers] so call sites inside this project can be
 * swapped for test dispatchers without static imports. Prefer injecting [AppDispatchers] in
 * ViewModels and use cases; data-layer helpers may use these defaults directly.
 */
object Dispatchers {
    val io: CoroutineDispatcher = KotlinDispatchers.IO
    val default: CoroutineDispatcher = KotlinDispatchers.Default
    val main: CoroutineDispatcher = KotlinDispatchers.Main.immediate
}

data class AppDispatchers(
    val io: CoroutineDispatcher = KotlinDispatchers.IO,
    val default: CoroutineDispatcher = KotlinDispatchers.Default,
    val main: CoroutineDispatcher = KotlinDispatchers.Main.immediate
)

package com.terrochroma.shared.test

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain

class TestDispatcher {
    val testDispatcher: TestDispatcher = UnconfinedTestDispatcher()

    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    fun teardown() {
        Dispatchers.resetMain()
    }
}

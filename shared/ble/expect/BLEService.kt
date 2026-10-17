package com.terrochroma.shared.ble

import com.terrochroma.shared.ble.model.BLEDevice
import com.terrochroma.shared.ble.model.BLEConnectionState
import com.terrochroma.shared.ble.model.SpectrumData
import kotlinx.coroutines.flow.Flow

interface BLEService {
    suspend fun initialize(): Result<Unit>
    suspend fun scanDevices(): Result<List<BLEDevice>>
    suspend fun connect(deviceId: String): Result<Unit>
    suspend fun disconnect(): Result<Unit>
    suspend fun readSpectrumData(): Result<SpectrumData>
    suspend fun startContinuousScan(interval: Long): Result<Unit>
    suspend fun stopContinuousScan(): Result<Unit>
    fun getConnectionState(): Flow<BLEConnectionState>
}

package com.terrochroma.shared.ui.screens.sync

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.terrochroma.shared.sync.CloudSyncService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SyncSettingsViewModel(
    private val cloudSyncService: CloudSyncService
) : ViewModel() {

    private val _syncEnabled = MutableStateFlow(false)
    val syncEnabled: StateFlow<Boolean> = _syncEnabled.asStateFlow()

    private val _syncStatus = MutableStateFlow(SyncStatus.Idle)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    private val _lastSyncTime = MutableStateFlow(0L)
    val lastSyncTime: StateFlow<Long> = _lastSyncTime.asStateFlow()

    private val _syncFrequency = MutableStateFlow(SyncFrequency.Manual)
    val syncFrequency: StateFlow<SyncFrequency> = _syncFrequency.asStateFlow()

    init {
        // Load settings from persistent storage
        loadSettings()
    }

    private fun loadSettings() {
        // In production, load from encrypted preferences
        _syncEnabled.value = false
        _syncFrequency.value = SyncFrequency.Manual
    }

    fun toggleSyncEnabled() {
        _syncEnabled.value = !_syncEnabled.value
        // Save to persistent storage
    }

    fun setSyncFrequency(frequency: SyncFrequency) {
        _syncFrequency.value = frequency
        // Save to persistent storage
    }

    fun manualSync() {
        viewModelScope.launch {
            _syncStatus.value = SyncStatus.Syncing
            try {
                val result = cloudSyncService.syncSamples()
                if (result.isSuccess) {
                    _lastSyncTime.value = System.currentTimeMillis()
                    _syncStatus.value = SyncStatus.Idle
                } else {
                    _syncStatus.value = SyncStatus.Error
                }
            } catch (e: Exception) {
                _syncStatus.value = SyncStatus.Error
            }
        }
    }
}

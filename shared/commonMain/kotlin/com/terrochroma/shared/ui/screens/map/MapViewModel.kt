package com.terrochroma.shared.ui.screens.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.terrochroma.shared.map.MapService
import com.terrochroma.shared.map.model.getMapPinColorForSOC
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MapViewModel(
    private val mapService: MapService,
    private val databaseManager: com.terrochroma.shared.database.DatabaseManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<MapUiState>(MapUiState.Loading)
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    private val _selectedSample = MutableStateFlow<com.terrochroma.shared.domain.model.SoilSample?>(null)
    val selectedSample: StateFlow<com.terrochroma.shared.domain.model.SoilSample?> = _selectedSample.asStateFlow()

    init {
        initializeMap()
        loadSamples()
    }

    private fun initializeMap() {
        viewModelScope.launch {
            _uiState.value = MapUiState.Loading
            try {
                mapService.initialize()
                _uiState.value = MapUiState.Ready
            } catch (e: Exception) {
                _uiState.value = MapUiState.Error(e.message ?: "Failed to initialize map")
            }
        }
    }

    private fun loadSamples() {
        viewModelScope.launch {
            try {
                val samples = databaseManager.getAllSamples()
                // Add pins to map
                samples.forEach { sample ->
                    mapService.addSamplePin(sample)
                }
                _uiState.value = MapUiState.Ready
            } catch (e: Exception) {
                _uiState.value = MapUiState.Error(e.message ?: "Failed to load samples")
            }
        }
    }

    fun selectSample(sample: com.terrochroma.shared.domain.model.SoilSample) {
        _selectedSample.value = sample
    }

    fun clearSelection() {
        _selectedSample.value = null
    }

    fun centerOnSample(sample: com.terrochroma.shared.domain.model.SoilSample) {
        viewModelScope.launch {
            if (sample.latitude != null && sample.longitude != null) {
                mapService.centerOnLocation(sample.latitude, sample.longitude, 15.0)
            }
        }
    }

    fun dismissError() {
        _uiState.value = MapUiState.Ready
    }
}

sealed class MapUiState {
    object Loading : MapUiState()
    object Ready : MapUiState()
    data class Error(val message: String) : MapUiState()
}

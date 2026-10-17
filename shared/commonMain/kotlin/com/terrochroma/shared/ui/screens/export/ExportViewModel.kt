package com.terrochroma.shared.ui.screens.export

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.terrochroma.shared.export.ExportFormat
import com.terrochroma.shared.export.ExportService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ExportViewModel(
    private val exportService: ExportService
) : ViewModel() {

    private val _uiState = MutableStateFlow<ExportUiState>(ExportUiState.Idle)
    val uiState: StateFlow<ExportUiState> = _uiState.asStateFlow()

    private val _sampleCount = MutableStateFlow(0)
    val sampleCount: StateFlow<Int> = _sampleCount.asStateFlow()

    init {
        _sampleCount.value = 0 // Placeholder
    }

    fun exportGeoJSON() {
        viewModelScope.launch {
            _uiState.value = ExportUiState.Exporting
            try {
                val result = exportService.exportAllToGeoJSON()
                if (result.isSuccess) {
                    _uiState.value = ExportUiState.Success(result.getOrNull() ?: "", ExportFormat.GEOJSON)
                } else {
                    _uiState.value = ExportUiState.Error(result.exceptionOrNull()?.message ?: "Export failed")
                }
            } catch (e: Exception) {
                _uiState.value = ExportUiState.Error(e.message ?: "Export failed")
            }
        }
    }

    fun exportKML() {
        viewModelScope.launch {
            _uiState.value = ExportUiState.Exporting
            try {
                val result = exportService.exportAllToKML()
                if (result.isSuccess) {
                    _uiState.value = ExportUiState.Success(result.getOrNull() ?: "", ExportFormat.KML)
                } else {
                    _uiState.value = ExportUiState.Error(result.exceptionOrNull()?.message ?: "Export failed")
                }
            } catch (e: Exception) {
                _uiState.value = ExportUiState.Error(e.message ?: "Export failed")
            }
        }
    }

    fun exportCSV() {
        viewModelScope.launch {
            _uiState.value = ExportUiState.Exporting
            try {
                val result = exportService.exportAllToCSV()
                if (result.isSuccess) {
                    _uiState.value = ExportUiState.Success(result.getOrNull() ?: "", ExportFormat.CSV)
                } else {
                    _uiState.value = ExportUiState.Error(result.exceptionOrNull()?.message ?: "Export failed")
                }
            } catch (e: Exception) {
                _uiState.value = ExportUiState.Error(e.message ?: "Export failed")
            }
        }
    }

    fun dismissSuccess() {
        _uiState.value = ExportUiState.Idle
    }

    fun dismissError() {
        _uiState.value = ExportUiState.Idle
    }
}

sealed class ExportUiState {
    object Idle : ExportUiState()
    object Exporting : ExportUiState()
    data class Success(val data: String, val format: ExportFormat) : ExportUiState()
    data class Error(val message: String) : ExportUiState()
}

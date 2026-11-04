package com.soilcarbon.engine.presentation.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soilcarbon.engine.domain.model.CalibrationProfile
import com.soilcarbon.engine.domain.model.SampleStats
import com.soilcarbon.engine.domain.model.SoilSample
import com.soilcarbon.engine.domain.repository.CalibrationRepository
import com.soilcarbon.engine.domain.repository.ExportRepository
import com.soilcarbon.engine.domain.repository.SampleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class ExportKind(val label: String, val extension: String) {
    CSV("CSV table", "csv"),
    GEOJSON("GeoJSON", "geojson"),
    BUNDLE("Full JSON bundle", "json"),
    PROVENANCE("Provenance manifest", "json")
}

data class ReviewUiState(
    val samples: List<SoilSample> = emptyList(),
    val stats: SampleStats = SampleStats(),
    val selectedId: String? = null,
    val selected: SoilSample? = null,
    val latestCalibration: CalibrationProfile? = null,
    val exporting: Boolean = false,
    val exportMessage: String? = null,
    val error: String? = null
)

@HiltViewModel
class ReviewViewModel @Inject constructor(
    private val sampleRepository: SampleRepository,
    calibrationRepository: CalibrationRepository,
    private val exportRepository: ExportRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ReviewUiState())
    val state: StateFlow<ReviewUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            sampleRepository.observeSamples().collect { samples ->
                _state.update { current ->
                    val selectedId = current.selectedId?.takeIf { id -> samples.any { it.id == id } }
                    current.copy(
                        samples = samples,
                        selectedId = selectedId,
                        selected = samples.firstOrNull { it.id == selectedId }
                    )
                }
            }
        }
        viewModelScope.launch {
            sampleRepository.observeStats().collect { stats ->
                _state.update { it.copy(stats = stats) }
            }
        }
        viewModelScope.launch {
            calibrationRepository.observeLatest().collect { profile ->
                _state.update { it.copy(latestCalibration = profile) }
            }
        }
    }

    fun select(id: String?) {
        _state.update { current ->
            current.copy(
                selectedId = id,
                selected = current.samples.firstOrNull { it.id == id }
            )
        }
    }

    fun deleteSelected() {
        val id = _state.value.selectedId ?: return
        viewModelScope.launch {
            sampleRepository.delete(id)
            _state.update { it.copy(selectedId = null, selected = null, exportMessage = "Sample deleted.") }
        }
    }

    fun export(kind: ExportKind, onlySelected: Boolean = false) {
        if (_state.value.exporting) return
        val samples = if (onlySelected) {
            listOfNotNull(_state.value.selected)
        } else {
            _state.value.samples
        }
        if (samples.isEmpty()) {
            _state.update { it.copy(error = "No samples to export.") }
            return
        }
        _state.update { it.copy(exporting = true, error = null, exportMessage = null) }
        viewModelScope.launch {
            runCatching {
                val path = when (kind) {
                    ExportKind.CSV -> exportRepository.exportCsv(samples)
                    ExportKind.GEOJSON -> exportRepository.exportGeoJson(samples)
                    ExportKind.BUNDLE -> exportRepository.exportBundle(samples)
                    ExportKind.PROVENANCE -> exportRepository.exportProvenance(samples)
                }
                val ids = samples.map { it.id }
                sampleRepository.markExported(ids, System.currentTimeMillis())
                path
            }.onSuccess { path ->
                _state.update {
                    it.copy(
                        exporting = false,
                        exportMessage = "${kind.label} exported: $path",
                        error = null
                    )
                }
            }.onFailure { throwable ->
                _state.update {
                    it.copy(
                        exporting = false,
                        error = throwable.message ?: "Export failed."
                    )
                }
            }
        }
    }

    fun clearBanners() {
        _state.update { it.copy(exportMessage = null, error = null) }
    }
}

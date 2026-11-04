package com.soilcarbon.engine.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soilcarbon.engine.domain.model.AppSettings
import com.soilcarbon.engine.domain.model.CalibrationProfile
import com.soilcarbon.engine.domain.model.ModelInfo
import com.soilcarbon.engine.domain.model.SampleStats
import com.soilcarbon.engine.domain.model.SoilSample
import com.soilcarbon.engine.domain.model.SyncReport
import com.soilcarbon.engine.domain.repository.CalibrationRepository
import com.soilcarbon.engine.domain.repository.CovariateRepository
import com.soilcarbon.engine.domain.repository.ModelRepository
import com.soilcarbon.engine.domain.repository.SampleRepository
import com.soilcarbon.engine.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardUiState(
    val stats: SampleStats = SampleStats(),
    val samples: List<SoilSample> = emptyList(),
    val calibration: CalibrationProfile? = null,
    val modelInfo: ModelInfo? = null,
    val settings: AppSettings = AppSettings(),
    val loadingModel: Boolean = false,
    val syncing: Boolean = false,
    val message: String? = null,
    val error: String? = null
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    sampleRepository: SampleRepository,
    calibrationRepository: CalibrationRepository,
    private val modelRepository: ModelRepository,
    private val covariateRepository: CovariateRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _state = MutableStateFlow(DashboardUiState())
    val state: StateFlow<DashboardUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            sampleRepository.observeSamples().collect { samples ->
                _state.update { it.copy(samples = samples) }
            }
        }
        viewModelScope.launch {
            sampleRepository.observeStats().collect { stats ->
                _state.update { it.copy(stats = stats) }
            }
        }
        viewModelScope.launch {
            calibrationRepository.observeLatest().collect { profile ->
                _state.update { it.copy(calibration = profile) }
            }
        }
        viewModelScope.launch {
            settingsRepository.settings.collect { settings ->
                _state.update { it.copy(settings = settings) }
            }
        }
        refreshModel()
    }

    fun refreshModel() {
        if (_state.value.loadingModel) return
        _state.update { it.copy(loadingModel = true) }
        viewModelScope.launch {
            runCatching { modelRepository.loadModel() }
                .onSuccess { info -> _state.update { it.copy(modelInfo = info, loadingModel = false) } }
                .onFailure { throwable ->
                    _state.update {
                        it.copy(
                            loadingModel = false,
                            error = throwable.message ?: "Could not read the model metadata."
                        )
                    }
                }
        }
    }

    fun syncNow() {
        if (_state.value.syncing) return
        if (!_state.value.settings.allowNetworkSync) {
            _state.update { it.copy(error = "Network sync is disabled in Settings.") }
            return
        }
        _state.update { it.copy(syncing = true, error = null, message = null) }
        viewModelScope.launch {
            runCatching { covariateRepository.syncPending() }
                .onSuccess { report: SyncReport ->
                    settingsRepository.setSyncResult(report.message, System.currentTimeMillis())
                    _state.update {
                        it.copy(
                            syncing = false,
                            message = report.message
                        )
                    }
                }
                .onFailure { throwable ->
                    settingsRepository.setSyncResult(
                        "Sync failed: ${throwable.message}",
                        System.currentTimeMillis()
                    )
                    _state.update {
                        it.copy(
                            syncing = false,
                            error = throwable.message ?: "Sync failed."
                        )
                    }
                }
        }
    }

    fun clearBanners() = _state.update { it.copy(message = null, error = null) }
}

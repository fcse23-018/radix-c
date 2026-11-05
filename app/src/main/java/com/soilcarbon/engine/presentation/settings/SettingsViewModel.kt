package com.soilcarbon.engine.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soilcarbon.engine.data.sync.AutoSyncCoordinator
import com.soilcarbon.engine.domain.calibration.CalibrationTargetSpec
import com.soilcarbon.engine.domain.model.AppSettings
import com.soilcarbon.engine.domain.model.CalibrationProfile
import com.soilcarbon.engine.domain.model.ModelInfo
import com.soilcarbon.engine.domain.model.SyncReport
import com.soilcarbon.engine.domain.model.UserRole
import com.soilcarbon.engine.domain.repository.CalibrationRepository
import com.soilcarbon.engine.domain.repository.CovariateRepository
import com.soilcarbon.engine.domain.repository.ModelRepository
import com.soilcarbon.engine.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val settings: AppSettings = AppSettings(),
    val cardSpec: CalibrationTargetSpec? = null,
    val calibration: CalibrationProfile? = null,
    val modelInfo: ModelInfo? = null,
    val loadingModel: Boolean = false,
    val syncing: Boolean = false,
    val clearingCalibration: Boolean = false,
    val message: String? = null,
    val error: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val calibrationRepository: CalibrationRepository,
    private val covariateRepository: CovariateRepository,
    private val modelRepository: ModelRepository,
    private val autoSyncCoordinator: AutoSyncCoordinator,
    cardSpec: CalibrationTargetSpec
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsUiState(cardSpec = cardSpec))
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.settings.collect { settings ->
                _state.update { it.copy(settings = settings) }
            }
        }
        viewModelScope.launch {
            calibrationRepository.observeLatest().collect { profile ->
                _state.update { it.copy(calibration = profile) }
            }
        }
        refreshModel()
    }

    fun setRole(role: UserRole) {
        viewModelScope.launch {
            settingsRepository.setRole(role)
            _state.update { it.copy(message = "Workspace set to ${role.displayName}.") }
        }
    }

    fun saveSoilDefaults(bulkDensityGcm3: Double, depthCm: Double) {
        if (bulkDensityGcm3 !in 0.5..2.5) {
            _state.update { it.copy(error = "Bulk density must be between 0.5 and 2.5 g/cm³.") }
            return
        }
        if (depthCm !in 1.0..200.0) {
            _state.update { it.copy(error = "Sampling depth must be between 1 and 200 cm.") }
            return
        }
        viewModelScope.launch {
            settingsRepository.updateSoilDefaults(bulkDensityGcm3, depthCm)
            _state.update { it.copy(message = "Soil defaults saved.") }
        }
    }

    fun setCapturePolicy(requireRaw: Boolean, requireGps: Boolean, minLux: Float) {
        viewModelScope.launch {
            settingsRepository.setCapturePolicy(requireRaw, requireGps, minLux)
            _state.update { it.copy(message = "Capture policy updated.") }
        }
    }

    fun setNetworkPolicy(allowNetwork: Boolean, autoSync: Boolean) {
        viewModelScope.launch {
            settingsRepository.setNetworkPolicy(allowNetwork, autoSync)
            // Enqueue or cancel the periodic worker immediately; the toggle now has an effect.
            val scheduled = autoSyncCoordinator.apply(allowNetwork, autoSync)
            _state.update {
                it.copy(
                    message = buildString {
                        append(
                            if (allowNetwork) "Network covariate lookups enabled."
                            else "Offline mode: network lookups disabled."
                        )
                        append(
                            if (scheduled) " Automatic covariate refresh scheduled."
                            else " Automatic covariate refresh stopped."
                        )
                    }
                )
            }
        }
    }

    fun setGpuEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setGpuEnabled(enabled)
            _state.update {
                it.copy(message = if (enabled) "GPU delegate preferred." else "CPU inference preferred.")
            }
        }
    }

    fun setSentinelToken(token: String) {
        viewModelScope.launch {
            settingsRepository.setSentinelToken(token)
            _state.update {
                it.copy(
                    message = if (token.isBlank()) {
                        "Sentinel token cleared. Satellite NDVI will be a DEFAULT value and results flagged degraded."
                    } else {
                        "Sentinel token saved. NDVI will be fetched for new captures."
                    }
                )
            }
        }
    }

    fun refreshModel() {
        if (_state.value.loadingModel) return
        _state.update { it.copy(loadingModel = true) }
        viewModelScope.launch {
            runCatching { modelRepository.loadModel() }
                .onSuccess { info ->
                    _state.update { it.copy(modelInfo = info, loadingModel = false) }
                }
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
            _state.update { it.copy(error = "Enable network covariate lookup before refreshing.") }
            return
        }
        _state.update { it.copy(syncing = true, error = null, message = null) }
        viewModelScope.launch {
            runCatching { covariateRepository.syncPending() }
                .onSuccess { report: SyncReport ->
                    settingsRepository.setSyncResult(report.message, System.currentTimeMillis())
                    _state.update { it.copy(syncing = false, message = report.message) }
                }
                .onFailure { throwable ->
                    settingsRepository.setSyncResult(
                        "Covariate refresh failed: ${throwable.message}",
                        System.currentTimeMillis()
                    )
                    _state.update {
                        it.copy(syncing = false, error = throwable.message ?: "Covariate refresh failed.")
                    }
                }
        }
    }

    fun clearCalibration() {
        if (_state.value.clearingCalibration) return
        _state.update { it.copy(clearingCalibration = true) }
        viewModelScope.launch {
            runCatching { calibrationRepository.clear() }
                .onSuccess {
                    _state.update {
                        it.copy(
                            clearingCalibration = false,
                            message = "Stored calibration cleared. The next capture will re-fit the card."
                        )
                    }
                }
                .onFailure { throwable ->
                    _state.update {
                        it.copy(
                            clearingCalibration = false,
                            error = throwable.message ?: "Could not clear the calibration profile."
                        )
                    }
                }
        }
    }

    fun clearBanners() = _state.update { it.copy(message = null, error = null) }
}

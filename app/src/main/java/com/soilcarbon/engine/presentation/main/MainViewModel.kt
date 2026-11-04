package com.soilcarbon.engine.presentation.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soilcarbon.engine.domain.model.AppSettings
import com.soilcarbon.engine.domain.model.UserRole
import com.soilcarbon.engine.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    /** Null while DataStore is still loading; the UI shows the branded splash until then. */
    val settings: StateFlow<AppSettings?> = settingsRepository.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = null
    )

    fun completeOnboarding(
        role: UserRole,
        bulkDensityGcm3: Double,
        depthCm: Double,
        consent: Boolean
    ) {
        viewModelScope.launch {
            settingsRepository.setRole(role)
            settingsRepository.updateSoilDefaults(bulkDensityGcm3, depthCm)
            settingsRepository.setGdprConsent(if (consent) System.currentTimeMillis() else null)
            settingsRepository.setOnboardingComplete(true)
        }
    }

    fun selectRole(role: UserRole) {
        viewModelScope.launch { settingsRepository.setRole(role) }
    }

    fun setGdprConsent(consent: Boolean) {
        viewModelScope.launch {
            settingsRepository.setGdprConsent(if (consent) System.currentTimeMillis() else null)
        }
    }
}

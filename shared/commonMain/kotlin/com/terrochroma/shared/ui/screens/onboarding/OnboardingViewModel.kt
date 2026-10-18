package com.terrochroma.shared.ui.screens.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.terrochroma.shared.config.ConsentPreferences
import com.terrochroma.shared.config.PreferencesStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class OnboardingViewModel(
    private val preferencesStorage: PreferencesStorage
) : ViewModel() {

    private val _currentStep = MutableStateFlow(0)
    val currentStep: StateFlow<Int> = _currentStep.asStateFlow()

    private val _isCompleted = MutableStateFlow(false)
    val isCompleted: StateFlow<Boolean> = _isCompleted.asStateFlow()

    private val _consentPreferences = MutableStateFlow(ConsentPreferences())
    val consentPreferences: StateFlow<ConsentPreferences> = _consentPreferences.asStateFlow()

    private val steps = getOnboardingSteps()

    init {
        viewModelScope.launch {
            // Check if onboarding was previously completed
            val completed = preferencesStorage.isOnboardingCompleted().getOrNull() ?: false
            if (completed) {
                _isCompleted.value = true
            }
            
            // Load existing consent preferences
            val existingConsent = preferencesStorage.getConsentPreferences().getOrNull()
            if (existingConsent != null) {
                _consentPreferences.value = existingConsent
            }
        }
    }

    fun nextStep() {
        if (_currentStep.value < steps.size - 1) {
            _currentStep.value++
        } else {
            completeOnboarding()
        }
    }

    fun skipOnboarding() {
        completeOnboarding()
    }

    fun goToStep(step: Int) {
        if (step in steps.indices) {
            _currentStep.value = step
        }
    }

    fun updateConsentPreferences(preferences: ConsentPreferences) {
        _consentPreferences.value = preferences
        viewModelScope.launch {
            saveConsentPreferences(preferences)
        }
    }

    private fun completeOnboarding() {
        _isCompleted.value = true
        viewModelScope.launch {
            // Save completion flag
            preferencesStorage.setOnboardingCompleted()
            // Save consent preferences
            saveConsentPreferences(_consentPreferences.value)
        }
    }

    fun getCurrentStepData(): OnboardingStep {
        return steps[_currentStep.value]
    }

    fun getTotalSteps(): Int {
        return steps.size
    }

    private suspend fun saveConsentPreferences(preferences: ConsentPreferences) {
        val result = preferencesStorage.saveConsentPreferences(preferences)
        if (result.isFailure) {
            // Log error but don't block onboarding
            // In production, implement proper error handling
        }
    }
}

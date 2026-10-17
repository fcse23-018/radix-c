package com.terrochroma.shared.ui.screens.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class OnboardingViewModel : ViewModel() {

    private val _currentStep = MutableStateFlow(0)
    val currentStep: StateFlow<Int> = _currentStep.asStateFlow()

    private val _isCompleted = MutableStateFlow(false)
    val isCompleted: StateFlow<Boolean> = _isCompleted.asStateFlow()

    private val steps = getOnboardingSteps()

    init {
        // Check if onboarding was previously completed
        // In production, this would check encrypted preferences
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

    private fun completeOnboarding() {
        _isCompleted.value = true
        // In production, save to encrypted preferences
        viewModelScope.launch {
            // Save completion flag
        }
    }

    fun getCurrentStepData(): OnboardingStep {
        return steps[_currentStep.value]
    }

    fun getTotalSteps(): Int {
        return steps.size
    }
}

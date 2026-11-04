package com.soilcarbon.engine.presentation.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soilcarbon.engine.domain.model.SoilSample
import com.soilcarbon.engine.domain.repository.SampleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MapUiState(
    val samples: List<SoilSample> = emptyList(),
    val selectedId: String? = null
) {
    val mappedSamples: List<SoilSample>
        get() = samples.filter { it.location?.isValid() == true }

    val selected: SoilSample?
        get() = mappedSamples.firstOrNull { it.id == selectedId } ?: mappedSamples.firstOrNull()
}

@HiltViewModel
class MapViewModel @Inject constructor(
    sampleRepository: SampleRepository
) : ViewModel() {

    private val _state = MutableStateFlow(MapUiState())
    val state: StateFlow<MapUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            sampleRepository.observeSamples().collect { samples ->
                _state.update { current ->
                    val selected = current.selectedId?.takeIf { id -> samples.any { it.id == id } }
                    current.copy(samples = samples, selectedId = selected)
                }
            }
        }
    }

    fun select(id: String) = _state.update { it.copy(selectedId = id) }
}

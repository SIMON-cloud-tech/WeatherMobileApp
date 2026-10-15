package com.example.weatherstore.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.weatherstore.local.WeatherEntity
import com.example.weatherstore.repository.WeatherRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class WeatherUiState(
    val readings: List<WeatherEntity> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class WeatherViewModel(private val repository: WeatherRepository) : ViewModel() {

    private val isLoading = MutableStateFlow(false)
    private val error = MutableStateFlow<String?>(null)

    val uiState: StateFlow<WeatherUiState> = combine(
        repository.getStoredReadings(),
        isLoading,
        error
    ) { readings, loading, errorMessage ->
        WeatherUiState(readings, loading, errorMessage)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = WeatherUiState()
    )

    fun fetchWeather(city: String) {
        if (city.isBlank()) {
            error.value = "Enter a city name"
            return
        }
        viewModelScope.launch {
            isLoading.value = true
            error.value = null
            try {
                repository.fetchAndStore(city.trim())
            } catch (e: Exception) {
                error.value = e.message ?: "Something went wrong"
            } finally {
                isLoading.value = false
            }
        }
    }

    /**
     * Validates and commits a user edit to an existing Room row.
     *
     * Because `original` came from Room, it carries the row's `id`.
     * `original.copy(...)` preserves that id, so `dao.update(...)`
     * performs an UPDATE on that specific row rather than inserting
     * a new one.
     *
     * Validation is done here (not in the UI) so any caller — dialog,
     * tests, future features — gets consistent rules.
     */
    fun editReading(
        original: WeatherEntity,
        temperature: Double,
        humidity: Int,
        description: String
    ) {
        if (humidity !in 0..100 || description.isBlank()) {
            error.value = "Humidity must be 0–100 and the description can't be empty"
            return
        }

        viewModelScope.launch {
            repository.updateReading(
                original.copy(
                    temperature = temperature,
                    humidity = humidity,
                    description = description.trim()
                )
            )
        }
    }
}

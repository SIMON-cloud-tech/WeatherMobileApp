package com.example.weatherstore.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.weatherstore.local.WeatherEntity
import com.example.weatherstore.repository.WeatherRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class WeatherUiState(
    val readings: List<WeatherEntity> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class WeatherViewModel(private val repository: WeatherRepository) : ViewModel() {

    private val isLoading = MutableStateFlow(false)
    private val error = MutableStateFlow<String?>(null)
    private val filterQuery = MutableStateFlow("")

    // Blank query → show everything. Non-blank → filter by city.
    // flatMapLatest drops the old subscription whenever filterQuery changes.
    private val readingsFlow = filterQuery.flatMapLatest { query ->
        if (query.isBlank()) {
            repository.getStoredReadings()
        } else {
            repository.getReadingsForCity(query)
        }
    }

    val uiState: StateFlow<WeatherUiState> = combine(
        readingsFlow,
        isLoading,
        error
    ) { readings, loading, errorMessage ->
        WeatherUiState(readings, loading, errorMessage)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = WeatherUiState()
    )

    /**
     * Called by the search field on every keystroke.
     * Filters saved Room readings locally — no network call.
     */
    fun onQueryChange(query: String) {
        filterQuery.value = query.trim()
    }

    /**
     * Called when the user taps the search icon.
     * Hits the API and saves the row. The Room flow re-emits automatically.
     */
    fun fetchWeather(city: String) {
        val trimmed = city.trim()
        if (trimmed.isBlank()) {
            error.value = "Enter a city name"
            return
        }
        filterQuery.value = trimmed
        viewModelScope.launch {
            isLoading.value = true
            error.value = null
            try {
                repository.fetchAndStore(trimmed)
            } catch (e: Exception) {
                error.value = e.message ?: "Something went wrong"
            } finally {
                isLoading.value = false
            }
        }
    }

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

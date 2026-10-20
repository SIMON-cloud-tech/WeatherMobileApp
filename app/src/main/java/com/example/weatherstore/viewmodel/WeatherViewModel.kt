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
import java.io.IOException

// One complete snapshot of everything the screen needs to draw.
data class WeatherUiState(
    val readings: List<WeatherEntity> = emptyList(), // saved rows (already filtered by the search text)
    val isLoading: Boolean = false,                  // true while an API fetch is running
    val error: String? = null                        // message for the banner, or null for none
)

// flatMapLatest is still marked experimental, so we opt in explicitly.
@OptIn(ExperimentalCoroutinesApi::class)
class WeatherViewModel(private val repository: WeatherRepository) : ViewModel() {

    // Private + mutable: only this class can change them.
    private val isLoading = MutableStateFlow(false)
    private val error = MutableStateFlow<String?>(null)
    private val filterQuery = MutableStateFlow("")

    // READ: blank query -> every saved row; otherwise only rows matching the text.
    // flatMapLatest drops the old Room subscription each time filterQuery changes
    // and starts a new one, so the list always matches the latest text.
    private val readingsFlow = filterQuery.flatMapLatest { query ->
        if (query.isBlank()) {
            repository.getStoredReadings()
        } else {
            repository.getReadingsForCity(query)
        }
    }

    // Public + read-only: the screen watches this and never edits it.
    // combine() merges three sources into one UiState snapshot.
    val uiState: StateFlow<WeatherUiState> = combine(
        readingsFlow,
        isLoading,
        error
    ) { readings, loading, errorMessage ->
        WeatherUiState(readings, loading, errorMessage)
    }.stateIn(
        scope = viewModelScope,
        // Keep running 5 s after the screen stops watching, so a rotation doesn't restart the query.
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = WeatherUiState()
    )

    // READ (filter): called on every keystroke. Local only, no network call.
    fun onQueryChange(query: String) {
        filterQuery.value = query.trim()
        error.value = null // an old banner shouldn't linger while the user types
    }

    // CREATE / UPDATE (refresh): called when the user taps search.
    // Asks the API and saves the result; Room's Flow then re-emits on its own.
    fun fetchWeather(city: String) {
        val trimmed = city.trim()
        if (trimmed.isBlank()) {            // validation lives here, not in the screen
            error.value = "Enter a city name"
            return
        }
        filterQuery.value = trimmed
        viewModelScope.launch {             // repository.fetchAndStore is suspend, so it needs a coroutine
            isLoading.value = true
            error.value = null
            try {
                repository.fetchAndStore(trimmed)
            } catch (e: IOException) {
                // Couldn't reach the server (offline, timeout, bad signal).
                // Saved cards stay on screen; we only explain what happened.
                val hasSaved = uiState.value.readings.isNotEmpty()
                error.value = if (hasSaved) "You're offline. Showing saved readings."
                else "You're offline and no saved reading matches \"$trimmed\"."
            } catch (e: Exception) {
                error.value = e.message ?: "Something went wrong"
            } finally {
                isLoading.value = false     // runs on success AND failure, so the spinner can't get stuck
            }
        }
    }

    // UPDATE (manual edit): called by the edit dialog's Save button.
    fun editReading(
        original: WeatherEntity,
        temperature: Double,
        humidity: Int,
        description: String
    ) {
        // The dialog also blocks bad input, but that is only a convenience.
        // The ViewModel is the real gatekeeper.
        if (humidity !in 0..100 || description.isBlank()) {
            error.value = "Humidity must be 0–100 and the description can't be empty"
            return
        }
        viewModelScope.launch {
            repository.updateReading(
                // copy() keeps the same id (which tells @Update which row to overwrite),
                // and changes only the three edited fields.
                original.copy(
                    temperature = temperature,
                    humidity = humidity,
                    description = description.trim()
                )
            )
        }
    }

    // DELETE: called by the delete dialog's confirm button.
    fun deleteReading(reading: WeatherEntity) {
        viewModelScope.launch {
            repository.deleteReading(reading)
        }
    }
}

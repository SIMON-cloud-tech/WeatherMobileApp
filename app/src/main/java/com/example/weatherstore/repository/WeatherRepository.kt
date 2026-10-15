package com.example.weatherstore.repository

import com.example.weatherstore.local.WeatherDao
import com.example.weatherstore.local.WeatherEntity
import com.example.weatherstore.network.RetrofitInstance
import com.example.weatherstore.network.WeatherApiService
import kotlinx.coroutines.flow.Flow

class WeatherRepository(
    private val api: WeatherApiService,
    private val dao: WeatherDao
) {
    suspend fun fetchAndStore(city: String) {
        // 1. Ask the API first. If this throws (offline), nothing below runs,
        // so the saved row stays exactly as it was.
        val response = api.getWeather(city = city, apiKey = RetrofitInstance.apiKey)

        // 2. Is this city already saved? Look up the API's name for it, not the typed text.
        val existing = dao.findByCity(response.name)

        // 3. Translate the API response to match the entity's table row.
        val entity = WeatherEntity(
            id = existing?.id ?: 0,
            city = response.name,
            temperature = response.main.temp,
            humidity = response.main.humidity,
            description = response.weather[0].description,
            timestamp = System.currentTimeMillis()
        )

        // 4. Same id means update, no row means insert.
        if (existing == null) dao.insert(entity) else dao.update(entity)
    }

    // Lets the ViewModel read what is stored, without knowing Room exists
    fun getStoredReadings(): Flow<List<WeatherEntity>> = dao.getAll()
}

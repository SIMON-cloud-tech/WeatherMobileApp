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
        val response = api.getWeather(city = city, apiKey = RetrofitInstance.apiKey)
        val entity = WeatherEntity(
            city = response.name,
            temperature = response.main.temp,
            humidity = response.main.humidity,
            description = response.weather[0].description,
            timestamp = System.currentTimeMillis()
        )
        dao.insert(entity)
    }

    // NEW: lets the ViewModel read what is stored, without knowing Room exists
    fun getStoredReadings(): Flow<List<WeatherEntity>> = dao.getAll()
}
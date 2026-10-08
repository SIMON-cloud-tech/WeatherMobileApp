package com.example.weatherstore.repository

import com.example.weatherstore.local.WeatherDao
import com.example.weatherstore.local.WeatherEntity
import com.example.weatherstore.network.WeatherApiService
import com.example.weatherstore.network.RetrofitInstance

class WeatherRepository(
    private val api: WeatherApiService,
    private val dao: WeatherDao
) {
    suspend fun fetchAndStore(city: String) {
        // step 1: fetch weather data from the api
        val response = api.getWeather(city = city, apiKey = RetrofitInstance.apiKey)

        // step 2: translate the api response
        // to match the Entity as Room expects it
        val entity = WeatherEntity(
            city = response.name,
            temperature = response.main.temp,
            humidity = response.main.humidity,
            description = response.weather[0].description,
            timestamp = System.currentTimeMillis()
        )
        // step 3: hand over the entity to the DAO
        dao.insert(entity)
    }
}
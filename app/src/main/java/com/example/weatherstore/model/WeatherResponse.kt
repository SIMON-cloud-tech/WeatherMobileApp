
package com.example.weatherstore.model
data class WeatherResponse(
    val name: String,
    val weather: List<WeatherDetail>,
    val main: MainInfo
)
data class WeatherDetail(val description: String)
data class MainInfo(val temp: Double, val humidity: Int)

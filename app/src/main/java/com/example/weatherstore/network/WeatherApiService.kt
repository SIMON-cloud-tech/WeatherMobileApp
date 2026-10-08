package com.example.weatherstore.network
import com.example.weatherstore.model.WeatherResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface WeatherApiService{
    @GET("weather") //@GET   so we
    // make a post call to the
    // api to get weather information
        suspend fun getWeather( //use suspend call
        // just so we can avoid crash incase
        //we face a slow network response
            @Query("q") city: String,
            //define what to use to map the api response
           //so we map the outcome by it
            @Query("appid") apiKey: String,
            //dictate which apiKey to use to
            //access the data
            @Query("units") units: String = "metric"
            //define what units we use
        ) : WeatherResponse
}
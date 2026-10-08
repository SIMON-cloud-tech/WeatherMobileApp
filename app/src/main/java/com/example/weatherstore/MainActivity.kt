package com.example.weatherstore

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import com.example.weatherstore.local.AppDatabase
import com.example.weatherstore.network.RetrofitInstance
import com.example.weatherstore.repository.WeatherRepository
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val db = Room.databaseBuilder(applicationContext, AppDatabase::class.java, "weather-db").build()
        val repository = WeatherRepository(RetrofitInstance.api, db.weatherDao())

        lifecycleScope.launch {
            repository.fetchAndStore("Nairobi")
            Log.d("WeatherTest", "Fetch and store finished")
        }
    }
}
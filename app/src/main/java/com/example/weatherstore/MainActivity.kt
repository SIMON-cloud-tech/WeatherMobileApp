package com.example.weatherstore

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.room.Room
import com.example.weatherstore.local.AppDatabase
import com.example.weatherstore.network.RetrofitInstance
import com.example.weatherstore.repository.WeatherRepository
import com.example.weatherstore.ui.WeatherScreen
import com.example.weatherstore.viewmodel.WeatherViewModel

class MainActivity : ComponentActivity() {

    // "by viewModels" asks Android for the ViewModel instead of creating it ourselves.
    // The factory is only needed because WeatherViewModel has a constructor parameter,
    // which Android doesn't know how to fill in on its own.
    private val viewModel: WeatherViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                // Same chain as before: database -> repository -> ViewModel.
                val db = Room.databaseBuilder(applicationContext, AppDatabase::class.java, "weather-db").build()
                val repository = WeatherRepository(RetrofitInstance.api, db.weatherDao())
                return WeatherViewModel(repository) as T
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                WeatherScreen(viewModel) // hand the ViewModel to the screen
            }
        }
    }
}
package com.example.weatherstore

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import com.example.weatherstore.local.AppDatabase
import com.example.weatherstore.network.RetrofitInstance
import com.example.weatherstore.repository.WeatherRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val db = Room.databaseBuilder(applicationContext, AppDatabase::class.java, "weather-db").build()
        val repository = WeatherRepository(RetrofitInstance.api, db.weatherDao())

        lifecycleScope.launch {
            try {
                repository.fetchAndStore("Nairobi")
                val rows = db.weatherDao().getAll().first()   // read back from Room
                val latest = rows.first()
                val message = "Stored ${rows.size} row(s). Latest: ${latest.city}, ${latest.temperature}°C, ${latest.description}"
                Log.d("WeatherTest", message)
                Toast.makeText(this@MainActivity, message, Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Log.e("WeatherTest", "Failed", e)
                Toast.makeText(this@MainActivity, "Failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}

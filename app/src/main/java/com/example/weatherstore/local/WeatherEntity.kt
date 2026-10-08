
package com.example.weatherstore.local
import android.room.Entity
import android.room.PrimaryKey

@Entity(tableName="weather_readings") //@Entity
// used to define the table onto which our translated data goes into
data class WeatherEntity (
    @PrimaryKey(autoGenerate=true) val id: Int=0, //PrimaryKey
    // autoGenerate true to uniquely autoincrement the tables
    val city : String,
    val temperature: Double,
    val humidity: Int,
    val description: String,
    val timestamp: Long
)

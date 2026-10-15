package com.example.weatherstore.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WeatherDao {
    // The Dao is an interface since Room auto-generates the real
    // implementing class for every instance — this avoids writing
    // unnecessary inheritance ourselves, which would otherwise make
    // the code ambiguous about who actually implements what.

    @Insert
    // Inserts the translated WeatherEntity (built from the API response
    // by the Repository) into the Room database.
    suspend fun insert(entity: WeatherEntity)
    // suspend because this coordinates with Room's disk I/O, which can
    // be slow enough to freeze the UI thread if run synchronously.

    @Query("SELECT * FROM weather_readings ORDER BY timestamp DESC")
    // @Query describes where to pull the data from and how to order it —
    // this is the function it actually belongs to:
    fun getAll(): Flow<List<WeatherEntity>>

    //findByCity answers “is this city already saved?”. It returns WeatherEntity?, 
    //so the result is null when the city is new. 
    //@Update overwrites the row whose primary key matches the entity you pass in.
    
    @Query("SELECT * FROM weather_readings WHERE city = :city COLLATE NOCASE ORDER BY timestamp DESC LIMIT 1")
    suspend fun findByCity(city: String): WeatherEntity?
    //@Update describes what dentry to edit in the room
     @Update
     suspend fun update(entity: WeatherEntity)
}

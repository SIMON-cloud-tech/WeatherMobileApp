package com.example.weatherstore.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface WeatherDao {
    // The Dao is an interface because Room generates the real implementing
    // class at compile time, so we only describe what we need.

    @Insert
    // Adds a brand-new row (used when a city is saved for the first time).
    suspend fun insert(entity: WeatherEntity)
    // suspend: disk work runs off the main thread so the UI doesn't freeze.

    @Update
    // Overwrites the row whose primary key (id) matches the entity passed in.
    suspend fun update(entity: WeatherEntity)

    @Delete
    // Removes the row whose primary key (id) matches the entity passed in.
    suspend fun delete(entity: WeatherEntity)

    @Query("SELECT * FROM weather_readings WHERE city = :city COLLATE NOCASE ORDER BY timestamp DESC LIMIT 1")
    // "Is this city already saved?" Returns the row, or null if it's a new city.
    // COLLATE NOCASE ignores upper/lower case, so "nairobi" matches "Nairobi".
    suspend fun findByCity(city: String): WeatherEntity?

    @Query("SELECT * FROM weather_readings ORDER BY timestamp DESC")
    // Every saved row, newest first, as a live Flow that re-emits whenever the table changes.
    fun getAll(): Flow<List<WeatherEntity>>

    // Filtered, partial-match query that stays live.
    // The '%' || :city || '%' builds a LIKE pattern like '%nai%',
    // matching any row whose city contains the typed text (SQLite's LIKE ignores case for English letters).
    @Query("SELECT * FROM weather_readings WHERE city LIKE '%' || :city || '%' ORDER BY timestamp DESC")
    fun getReadingsForCity(city: String): Flow<List<WeatherEntity>>
}

package com.example.weatherstore.repository

import com.example.weatherstore.local.WeatherDao
import com.example.weatherstore.local.WeatherEntity
import com.example.weatherstore.network.RetrofitInstance
import com.example.weatherstore.network.WeatherApiService
import kotlinx.coroutines.flow.Flow

// The Repository is the ONLY class that knows both the API and the database exist.
// It offers one function per operation, so the ViewModel never touches Room or Retrofit.
// (api and dao are passed in, not created here, so tests can swap in fakes.)
class WeatherRepository(
    private val api: WeatherApiService,
    private val dao: WeatherDao
) {

    // ---------------------------------------------------------------
    // CREATE (new city) + UPDATE (refresh an existing city)
    // ---------------------------------------------------------------
    suspend fun fetchAndStore(city: String) {
        // 1. Ask the API first. If this throws (offline), nothing below runs,
        // so the saved row stays exactly as it was.
        val response = api.getWeather(city = city, apiKey = RetrofitInstance.apiKey)

        // 2. Is this city already saved? Look up the API's name for it, not the typed text.
        val existing = dao.findByCity(response.name)

        // 3. Translate the nested API response into the flat row Room stores.
        //    existing?.id ?: 0 -> reuse the old id if there is one, otherwise 0
        //    (0 tells Room's autoGenerate to assign a new id on insert).
        val entity = WeatherEntity(
            id = existing?.id ?: 0,
            city = response.name,
            temperature = response.main.temp,
            humidity = response.main.humidity,
            description = response.weather[0].description,
            timestamp = System.currentTimeMillis()
        )

        // 4. No existing row -> INSERT a new one. Existing row -> UPDATE it (same id).
        if (existing == null) dao.insert(entity) else dao.update(entity)
    }

    // ---------------------------------------------------------------
    // UPDATE (manual edit from the dialog)
    // ---------------------------------------------------------------
    // Overwrites the row whose id matches the entity passed in.
    suspend fun updateReading(reading: WeatherEntity) = dao.update(reading)

    // ---------------------------------------------------------------
    // DELETE
    // ---------------------------------------------------------------
    // Removes the row whose id matches the entity passed in.
    suspend fun deleteReading(reading: WeatherEntity) = dao.delete(reading)

    // ---------------------------------------------------------------
    // READ
    // ---------------------------------------------------------------
    // Every saved row as a live stream: it re-emits whenever the table changes.
    // Not suspend: a Flow is already asynchronous.
    fun getStoredReadings(): Flow<List<WeatherEntity>> = dao.getAll()

    // Only the rows whose city contains the typed text (SQL LIKE), also live.
    fun getReadingsForCity(city: String): Flow<List<WeatherEntity>> =
        dao.getReadingsForCity(city)
}

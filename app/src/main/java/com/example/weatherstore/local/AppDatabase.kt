package com.example.weatherstore.local

import androidx.room.RoomDatabase
import androidx.room.Database

@Database(entities = [WeatherEntity::class], version = 1)
// @Database lists the entities our Room database maps onto, since Room
// can't automatically discover which classes are meant to be tables.
abstract class AppDatabase : RoomDatabase() {
    // The class is abstract by default since Room ought to write the
    // implementing code at compile time — it's incomplete by design.
    abstract fun weatherDao(): WeatherDao
    // weatherDao() is abstract since Room generates the real, working
    // implementation that returns a usable Dao instance.
}
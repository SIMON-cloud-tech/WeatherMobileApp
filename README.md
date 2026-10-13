# WeatherStore

An Android app that fetches the current weather for any city from OpenWeatherMap, saves every reading in a local Room database, and shows the saved history as cards.

**Flow:** type a city → Retrofit calls the API → the reading is stored in Room → the list updates automatically.

## Tech stack

Kotlin · Jetpack Compose (Material 3) · Retrofit + Gson · Room (via KSP) · Coroutines & Flow · MVVM

## Project structure

```
com.example.weatherstore
├── MainActivity.kt        wires everything together
├── model/                 WeatherResponse.kt      API response shape
├── network/               WeatherApiService.kt, RetrofitInstance.kt
├── local/                 WeatherEntity.kt, WeatherDao.kt, AppDatabase.kt
├── repository/            WeatherRepository.kt    fetch + store, single data gateway
├── viewmodel/             WeatherViewModel.kt     screen state
└── ui/                    WeatherScreen.kt        Compose UI
```

## Setup

1. Get a free API key at [openweathermap.org](https://openweathermap.org/api). New keys can take a while to activate.
2. Add it to `local.properties` in the project root (this file is gitignored):
   ```
   OPEN_WEATHER_API_KEY=your_key_here
   ```
3. Connect a device or emulator and run:
   ```
   ./gradlew installDebug
   ```

Requirements: JDK 17, Android SDK 37 (compileSdk), minSdk 24.

## Building with GitHub Actions

Pushing to `main` builds a debug APK. Add your key under **Settings → Secrets and variables → Actions** as `OPEN_WEATHER_API_KEY`, then download the APK from the run's **Artifacts** section.

## Note

The API key is compiled into the APK through `BuildConfig`, so anyone with the APK can extract it. That is fine for a free key, but don't share builds you wouldn't want it pulled from.

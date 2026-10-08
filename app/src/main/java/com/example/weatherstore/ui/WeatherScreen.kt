package com.example.weatherstore.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.weatherstore.local.WeatherEntity
import com.example.weatherstore.viewmodel.WeatherViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun WeatherScreen(viewModel: WeatherViewModel) {
    val state by viewModel.uiState.collectAsState()
    var city by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = city,
                onValueChange = { city = it },
                label = { Text("City") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = { viewModel.fetchWeather(city) },
                enabled = !state.isLoading
            ) {
                Text("Fetch")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (state.isLoading) {
            CircularProgressIndicator()
        }

        state.error?.let { message ->
            Text(text = "Error: $message", color = MaterialTheme.colorScheme.error)
        }

        if (state.readings.isEmpty() && !state.isLoading) {
            Text("No readings saved yet. Enter a city and tap Fetch.")
        }

        LazyColumn {
            items(items = state.readings, key = { it.id }) { reading ->
                WeatherCard(reading)
            }
        }
    }
}

@Composable
private fun WeatherCard(reading: WeatherEntity) {
    val time = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()).format(Date(reading.timestamp))
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(reading.city, style = MaterialTheme.typography.titleLarge)
            Text("Temperature: ${reading.temperature}°C")
            Text("Humidity: ${reading.humidity}%")
            Text(reading.description)
            Text("Fetched: $time", style = MaterialTheme.typography.bodySmall)
        }
    }
}
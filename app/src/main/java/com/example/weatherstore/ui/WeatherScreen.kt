package com.example.weatherstore.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.weatherstore.local.WeatherEntity
import com.example.weatherstore.viewmodel.WeatherViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// The screen only DRAWS state and REPORTS taps. It never touches Room or Retrofit:
// every real decision is delegated to the ViewModel.
@Composable
fun WeatherScreen(viewModel: WeatherViewModel) {
    // Subscribes to the ViewModel's state; Compose redraws whenever a new value arrives.
    val state by viewModel.uiState.collectAsState()
    // What is typed in the search box. Kept here so typing never lags.
    var city by remember { mutableStateOf("") }
    // The reading being edited (null = no edit dialog open).
    var editing by remember { mutableStateOf<WeatherEntity?>(null) }
    // The reading waiting for delete confirmation (null = no dialog open).
    var deleting by remember { mutableStateOf<WeatherEntity?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding() // keeps content clear of the status and navigation bars
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        SearchBar(
            city = city,
            onCityChange = {
                city = it
                viewModel.onQueryChange(it)       // READ: filters the saved readings as you type
            },
            onSearch = {
                if (city.isNotBlank() && !state.isLoading) {
                    viewModel.fetchWeather(city)  // CREATE/UPDATE: asks the API; falls back to saved data offline
                }
            },
            isLoading = state.isLoading
        )

        // Errors and offline notices appear as a banner; saved cards stay visible below.
        state.error?.let { message ->
            Spacer(modifier = Modifier.height(12.dp))
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // The first matching branch wins, so the order matters.
        when {
            // Saved readings always win: they show while loading, offline, or after an error.
            state.readings.isNotEmpty() -> {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    // key = id gives every row a stable identity, so after an update or delete
                    // Compose reuses the matching cards instead of redrawing the whole list.
                    items(items = state.readings, key = { it.id }) { reading ->
                        WeatherCard(
                            reading = reading,
                            onEdit = { editing = reading },     // opens the edit dialog
                            onDelete = { deleting = reading }   // opens the delete confirmation
                        )
                    }
                }
            }

            // Nothing to show yet and a fetch is running.
            state.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(top = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            // Nothing saved and nothing typed.
            city.isBlank() -> {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(top = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No readings yet.\nType a city and tap search.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Something typed, but no saved city matches it.
            else -> {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(top = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No saved readings for \"$city\".\nTap search to fetch it.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }

    // UPDATE: the edit dialog exists only while a reading is selected.
    editing?.let { reading ->
        EditReadingDialog(
            reading = reading,
            onDismiss = { editing = null },
            onSave = { temperature, humidity, description ->
                viewModel.editReading(reading, temperature, humidity, description)
                editing = null
            }
        )
    }

    // DELETE: ask first, because a delete can't be undone.
    deleting?.let { reading ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Delete ${reading.city}?") },
            text = { Text("This removes the saved reading from your device.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteReading(reading)
                    deleting = null
                }) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { deleting = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SearchBar(
    city: String,
    onCityChange: (String) -> Unit,
    onSearch: () -> Unit,
    isLoading: Boolean
) {
    OutlinedTextField(
        value = city,
        onValueChange = onCityChange,
        placeholder = { Text("Search a city…") },
        singleLine = true,
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        trailingIcon = {
            FilledIconButton(
                onClick = onSearch,
                enabled = city.isNotBlank() && !isLoading,
                shape = CircleShape,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.padding(end = 4.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSearch() }),
        shape = RoundedCornerShape(28.dp),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun WeatherCard(
    reading: WeatherEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    // Room stores the time as milliseconds (a Long); this turns it into readable text.
    val time = SimpleDateFormat("dd MMM • HH:mm", Locale.getDefault())
        .format(Date(reading.timestamp))

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {

            // --- Header: city + temperature ---
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = reading.city,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = reading.description.replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "${reading.temperature.toInt()}°",
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Light,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(12.dp))

            // --- Detail row: humidity + timestamp + edit and delete buttons ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                DetailChip(label = "Humidity", value = "${reading.humidity}%")
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = time,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                IconButton(onClick = onEdit) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit reading",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete reading",
                        tint = MaterialTheme.colorScheme.error // red hints that this is destructive
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailChip(label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun EditReadingDialog(
    reading: WeatherEntity,
    onDismiss: () -> Unit,
    onSave: (Double, Int, String) -> Unit
) {
    // Each box keeps its own typing state, starting from the card's current values.
    var tempText by remember { mutableStateOf(reading.temperature.toString()) }
    var humidityText by remember { mutableStateOf(reading.humidity.toString()) }
    var description by remember { mutableStateOf(reading.description) }

    // toDoubleOrNull / toIntOrNull give null when the text isn't a number.
    val temperature = tempText.toDoubleOrNull()
    val humidity = humidityText.toIntOrNull()
    val isValid = temperature != null && humidity != null &&
        humidity in 0..100 && description.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit ${reading.city}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = tempText,
                    onValueChange = { tempText = it },
                    label = { Text("Temperature (°C)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
                OutlinedTextField(
                    value = humidityText,
                    onValueChange = { humidityText = it },
                    label = { Text("Humidity (%)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = isValid, // Save stays disabled until all three fields are valid
                onClick = {
                    if (temperature != null && humidity != null) {
                        onSave(temperature, humidity, description.trim())
                    }
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

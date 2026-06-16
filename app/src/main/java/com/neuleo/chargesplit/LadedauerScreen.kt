package com.neuleo.chargesplit

import android.app.TimePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LadedauerScreen(
    prefsManager: PreferencesManager,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var startSoc by remember { mutableStateOf(20f) }
    var targetSoc by remember { mutableStateOf(80f) }
    
    var startTime by remember { mutableStateOf(Calendar.getInstance()) }
    val formattedTime = String.format(Locale.US, "%02d:%02d", startTime.get(Calendar.HOUR_OF_DAY), startTime.get(Calendar.MINUTE))

    val timePickerDialog = remember {
        TimePickerDialog(
            context,
            { _, hourOfDay, minute ->
                val newTime = Calendar.getInstance().apply {
                    timeInMillis = startTime.timeInMillis
                    set(Calendar.HOUR_OF_DAY, hourOfDay)
                    set(Calendar.MINUTE, minute)
                }
                startTime = newTime
            },
            startTime.get(Calendar.HOUR_OF_DAY),
            startTime.get(Calendar.MINUTE),
            true
        )
    }

    val chargerTypes = listOf(
        "Schuko (2.3 kW)",
        "Wallbox AC 11 kW",
        "Wallbox AC 22 kW",
        "DC Schnelllader 50 kW",
        "Benutzerdefiniert"
    )
    var chargerType by remember { mutableStateOf("Wallbox AC 11 kW") }
    var customChargerKwText by remember { mutableStateOf("22.0") }
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(text = "Ladedauer-Rechner", style = MaterialTheme.typography.headlineMedium)

        // Start-SOC
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(text = "Start-SOC: ${startSoc.toInt()}%", style = MaterialTheme.typography.bodyLarge)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Slider(
                    value = startSoc,
                    onValueChange = { startSoc = it },
                    valueRange = 0f..100f,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = startSoc.toInt().toString(),
                    onValueChange = {
                        val value = it.toFloatOrNull()
                        if (value != null) {
                            startSoc = value.coerceIn(0f, 100f)
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.width(80.dp)
                )
            }
        }

        // Ziel-SOC
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(text = "Ziel-SOC: ${targetSoc.toInt()}%", style = MaterialTheme.typography.bodyLarge)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Slider(
                    value = targetSoc,
                    onValueChange = { targetSoc = it },
                    valueRange = 0f..100f,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = targetSoc.toInt().toString(),
                    onValueChange = {
                        val value = it.toFloatOrNull()
                        if (value != null) {
                            targetSoc = value.coerceIn(0f, 100f)
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.width(80.dp)
                )
            }
        }

        // Startzeit
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(text = "Startzeit: $formattedTime", style = MaterialTheme.typography.bodyLarge)
            Button(onClick = { timePickerDialog.show() }) {
                Text("Zeit wählen")
            }
        }

        // Ladeart
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = chargerType,
                onValueChange = {},
                readOnly = true,
                label = { Text("Ladeart") },
                trailingIcon = {
                    IconButton(onClick = { expanded = true }) {
                        Icon(Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                chargerTypes.forEach { type ->
                    DropdownMenuItem(
                        text = { Text(type) },
                        onClick = {
                            chargerType = type
                            expanded = false
                        }
                    )
                }
            }
        }

        if (LadedauerValidator.shouldShowCustomCharger(chargerType)) {
            OutlinedTextField(
                value = customChargerKwText,
                onValueChange = { customChargerKwText = it },
                label = { Text("Ladeleistung (kW)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Placeholder for Results section (Phase 4)
        Spacer(modifier = Modifier.height(16.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(text = "Berechnungsergebnisse (wird in Phase 4 integriert)", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

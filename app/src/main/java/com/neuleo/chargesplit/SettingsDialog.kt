package com.neuleo.chargesplit

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.neuleo.chargesplit.model.VehiclePreset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDialog(
    prefsManager: PreferencesManager,
    onDismiss: () -> Unit
) {
    var selectedPreset by remember { mutableStateOf(VehiclePreset.fromId(prefsManager.vehiclePresetId)) }
    var nominalText by remember { mutableStateOf(prefsManager.batteryNominalKwh.toString()) }
    var degradationText by remember { mutableStateOf(prefsManager.batteryDegradation.toString()) }
    var priceText by remember { mutableStateOf(prefsManager.costPerKWh.toString()) }
    var acEffText by remember { mutableStateOf((prefsManager.acEfficiency * 100f).toInt().toString()) }
    var dcEffText by remember { mutableStateOf((prefsManager.dcEfficiency * 100f).toInt().toString()) }
    var expanded by remember { mutableStateOf(false) }

    // Validation checks
    val isBatteryValid = SettingsValidator.validateBatteryCapacity(nominalText) != null
    val isDegradationValid = SettingsValidator.validateDegradation(degradationText) != null
    val isPriceValid = SettingsValidator.validateCostPerKWh(priceText) != null
    val isAcValid = SettingsValidator.validateEfficiency50to100(acEffText) != null
    val isDcValid = SettingsValidator.validateEfficiency50to100(dcEffText) != null
    val canSave = isBatteryValid && isDegradationValid && isPriceValid && isAcValid && isDcValid

    val effectiveCap = (nominalText.toFloatOrNull() ?: 0f) * (1f - (degradationText.toFloatOrNull() ?: 0f) / 100f)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Fahrzeugeinstellungen") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Preset Dropdown Selector
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = selectedPreset.name,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Fahrzeugprofil") },
                        trailingIcon = {
                            Icon(Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    // Click overlay to capture clicks anywhere on the field
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { expanded = true }
                    )
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        VehiclePreset.ALL_PRESETS.forEach { preset ->
                            DropdownMenuItem(
                                text = { Text(preset.name) },
                                onClick = {
                                    selectedPreset = preset
                                    expanded = false
                                    if (preset != VehiclePreset.CUSTOM) {
                                        nominalText = preset.nominalKwh.toString()
                                        acEffText = (preset.acEfficiency * 100f).toInt().toString()
                                        dcEffText = (preset.dcEfficiency * 100f).toInt().toString()
                                    }
                                }
                            )
                        }
                    }
                }

                // Battery Capacity
                OutlinedTextField(
                    value = nominalText,
                    onValueChange = {
                        nominalText = it
                        selectedPreset = VehiclePreset.CUSTOM
                    },
                    label = { Text("Nominale Batteriekapazität (kWh)") },
                    isError = !isBatteryValid,
                    supportingText = {
                        if (!isBatteryValid) {
                            Text("Positive Zahl erforderlich (z.B. 70.0)")
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                // Battery Degradation
                OutlinedTextField(
                    value = degradationText,
                    onValueChange = { degradationText = it },
                    label = { Text("Batteriedegradation (%)") },
                    isError = !isDegradationValid,
                    supportingText = {
                        if (!isDegradationValid) {
                            Text("Wert zwischen 0 und 100 erforderlich")
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                // Effective Capacity (Read-Only)
                OutlinedTextField(
                    value = String.format(java.util.Locale.US, "%.2f kWh", effectiveCap),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Effektive nutzbare Kapazität") },
                    modifier = Modifier.fillMaxWidth()
                )

                // Electricity Price
                OutlinedTextField(
                    value = priceText,
                    onValueChange = { priceText = it },
                    label = { Text("Strompreis (€/kWh)") },
                    isError = !isPriceValid,
                    supportingText = {
                        if (!isPriceValid) {
                            Text("Gültiger Strompreis erforderlich")
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                // AC Efficiency
                OutlinedTextField(
                    value = acEffText,
                    onValueChange = {
                        acEffText = it
                        selectedPreset = VehiclePreset.CUSTOM
                    },
                    label = { Text("AC Ladeeffizienz (%)") },
                    isError = !isAcValid,
                    supportingText = {
                        if (!isAcValid) {
                            Text("Wert zwischen 50 und 100 erforderlich")
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                // DC Efficiency
                OutlinedTextField(
                    value = dcEffText,
                    onValueChange = {
                        dcEffText = it
                        selectedPreset = VehiclePreset.CUSTOM
                    },
                    label = { Text("DC Ladeeffizienz (%)") },
                    isError = !isDcValid,
                    supportingText = {
                        if (!isDcValid) {
                            Text("Wert zwischen 50 und 100 erforderlich")
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (canSave) {
                        prefsManager.vehiclePresetId = selectedPreset.id
                        prefsManager.batteryNominalKwh = nominalText.toFloat()
                        prefsManager.batteryDegradation = degradationText.toFloat()
                        prefsManager.costPerKWh = priceText.toFloat()
                        prefsManager.acEfficiency = acEffText.toFloat() / 100f
                        prefsManager.dcEfficiency = dcEffText.toFloat() / 100f
                        onDismiss()
                    }
                },
                enabled = canSave
            ) {
                Text("Speichern")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen")
            }
        }
    )
}

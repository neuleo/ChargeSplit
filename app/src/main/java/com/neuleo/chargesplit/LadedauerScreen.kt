package com.neuleo.chargesplit

import android.app.TimePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.neuleo.chargesplit.model.VehiclePreset
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
    var showCalibrationDialog by remember { mutableStateOf(false) }
    var calibrationResultEfficiency by remember { mutableStateOf<Float?>(null) }
    var calibratedChargerType by remember { mutableStateOf("") }
    var calibrationTrigger by remember { mutableStateOf(0) }
    
    var startTime by remember { mutableStateOf(Calendar.getInstance()) }
    val formattedTime = String.format(Locale.US, "%02d:%02d", startTime.get(Calendar.HOUR_OF_DAY), startTime.get(Calendar.MINUTE))

    val timePickerDialog = remember(startTime) {
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

    var priceText by remember { mutableStateOf(prefsManager.costPerKWh.toString()) }
    var priceError by remember { mutableStateOf(false) }

    val electricityPrice = remember(priceText) {
        val parsed = priceText.toFloatOrNull()
        if (parsed != null && parsed >= 0f) {
            prefsManager.costPerKWh = parsed
            priceError = false
            parsed
        } else {
            priceError = true
            prefsManager.costPerKWh
        }
    }

    // Resolve vehicle preset and custom settings
    val activePreset = remember(prefsManager.vehiclePresetId, prefsManager.batteryNominalKwh, prefsManager.acEfficiency, prefsManager.dcEfficiency) {
        if (prefsManager.vehiclePresetId == "custom") {
            VehiclePreset(
                id = "custom",
                name = "Custom",
                nominalKwh = prefsManager.batteryNominalKwh,
                usableKwh = prefsManager.batteryNominalKwh,
                maxAcKw = 22.0f,
                maxDcKw = 150.0f,
                acEfficiency = prefsManager.acEfficiency,
                dcEfficiency = prefsManager.dcEfficiency
            )
        } else {
            VehiclePreset.fromId(prefsManager.vehiclePresetId)
        }
    }

    // Determine charger Kw and type
    val chargerKw = when (chargerType) {
        "Schuko (2.3 kW)" -> 2.3f
        "Wallbox AC 11 kW" -> 11.0f
        "Wallbox AC 22 kW" -> 22.0f
        "DC Schnelllader 50 kW" -> 50.0f
        else -> customChargerKwText.toFloatOrNull() ?: 0f
    }
    val isAc = chargerType != "DC Schnelllader 50 kW" && (chargerType != "Benutzerdefiniert" || chargerKw <= 22.0f)

    val currentEfficiency = remember(activePreset.id, chargerType, isAc, activePreset.acEfficiency, activePreset.dcEfficiency, calibrationTrigger) {
        prefsManager.getChargerEfficiency(activePreset.id, chargerType, isAc)
    }

    // Run charging calculator
    val result = remember(startSoc, targetSoc, chargerKw, isAc, activePreset, prefsManager.batteryDegradation, electricityPrice, currentEfficiency) {
        ChargingCalculator.calculateChargingDuration(
            startSoc = startSoc,
            targetSoc = targetSoc,
            chargerKw = chargerKw,
            isAc = isAc,
            preset = activePreset,
            degradation = prefsManager.batteryDegradation,
            electricityPrice = electricityPrice,
            efficiencyOverride = currentEfficiency
        )
    }

    // End time calculation
    val endTime = remember(startTime, result.durationMinutes) {
        (startTime.clone() as Calendar).apply {
            add(Calendar.MINUTE, result.durationMinutes.toInt())
        }
    }
    val formattedEndTime = String.format(Locale.US, "%02d:%02d", endTime.get(Calendar.HOUR_OF_DAY), endTime.get(Calendar.MINUTE))

    val hours = result.durationMinutes.toInt() / 60
    val minutes = result.durationMinutes.toInt() % 60
    val durationStr = "${hours}h ${minutes}min"

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(text = "Ladedauer-Rechner", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

        // Vehicle info preview card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Aktives Fahrzeug: ${activePreset.name}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    text = "Kapazität: ${activePreset.nominalKwh} kWh | Degradation: ${prefsManager.batteryDegradation}% | Strompreis: ${String.format(Locale.US, "%.2f", electricityPrice)} €/kWh",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                )
            }
        }

        // Start-SOC
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(text = "Start-SOC: ${startSoc.toInt()}%", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
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
            Text(text = "Ziel-SOC: ${targetSoc.toInt()}%", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
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
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Startzeit: $formattedTime",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            Button(onClick = { timePickerDialog.show() }) {
                Text("Zeit wählen")
            }
            OutlinedButton(onClick = { startTime = Calendar.getInstance() }) {
                Text("Jetzt")
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

        // Active efficiency display with optional reset
        val isCalibrated = remember(activePreset.id, chargerType, calibrationTrigger) {
            prefsManager.isChargerCalibrated(activePreset.id, chargerType)
        }
        val displayEfficiency = (currentEfficiency * 100f).toInt()

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Ladeeffizienz: $displayEfficiency% " + if (isCalibrated) "(Kalibriert)" else "(Standard)",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (isCalibrated) {
                TextButton(
                    onClick = {
                        prefsManager.clearChargerEfficiency(activePreset.id, chargerType)
                        calibrationTrigger++
                    },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text("Zurücksetzen", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        // Ladeleistung (falls Custom) & Strompreis Eingabe
        if (LadedauerValidator.shouldShowCustomCharger(chargerType)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = customChargerKwText,
                    onValueChange = { customChargerKwText = it },
                    label = { Text("Ladeleistung (kW)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = priceText,
                    onValueChange = { priceText = it },
                    label = { Text("Strompreis (€/kWh)") },
                    isError = priceError,
                    supportingText = {
                        if (priceError) {
                            Text("Ungültig")
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
            }
        } else {
            OutlinedTextField(
                value = priceText,
                onValueChange = { priceText = it },
                label = { Text("Strompreis (€/kWh)") },
                isError = priceError,
                supportingText = {
                    if (priceError) {
                        Text("Geben Sie einen gültigen Preis ein")
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Capping Info Note
        if (result.isCapped) {
            val maxRate = result.effectivePowerKw
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Begrenzung",
                        tint = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Text(
                        text = "Begrenzung: Die Ladeleistung wird durch das Fahrzeug auf $maxRate kW begrenzt (Ladegerät liefert $chargerKw kW).",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }

        // Calculation Results Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Berechnungsergebnisse",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                
                HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Ladedauer:", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text(text = durationStr, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Energie (Netz):", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text(
                        text = String.format(Locale.US, "%.1f kWh", result.gridEnergyKwh),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Ladekosten:", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text(
                        text = String.format(Locale.GERMANY, "%.2f €", result.totalCostEur),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Geschätzte Fertigzeit:", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text(text = formattedEndTime, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
        }

        // Button for Calibration Wizard
        Button(
            onClick = { showCalibrationDialog = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Ladevorgang kalibrieren")
        }
    }

    if (showCalibrationDialog) {
        CalibrationDialog(
            prefsManager = prefsManager,
            initialChargerType = chargerType,
            initialStartSoc = startSoc,
            initialTargetSoc = targetSoc,
            onDismiss = { showCalibrationDialog = false },
            onCalibrated = { eff, type ->
                calibrationResultEfficiency = eff
                calibratedChargerType = type
                calibrationTrigger++
                showCalibrationDialog = false
            }
        )
    }

    if (calibrationResultEfficiency != null) {
        AlertDialog(
            onDismissRequest = { calibrationResultEfficiency = null },
            title = { Text("Kalibrierung erfolgreich") },
            text = {
                Text("Kalibrierte Effizienz für $calibratedChargerType: ${(calibrationResultEfficiency!! * 100f).toInt()}%")
            },
            confirmButton = {
                TextButton(onClick = { calibrationResultEfficiency = null }) {
                    Text("OK")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalibrationDialog(
    prefsManager: PreferencesManager,
    initialChargerType: String,
    initialStartSoc: Float,
    initialTargetSoc: Float,
    onDismiss: () -> Unit,
    onCalibrated: (Float, String) -> Unit
) {
    val chargerTypes = listOf(
        "Schuko (2.3 kW)",
        "Wallbox AC 11 kW",
        "Wallbox AC 22 kW",
        "DC Schnelllader 50 kW",
        "Benutzerdefiniert"
    )
    
    var selectedChargerType by remember { mutableStateOf(initialChargerType) }
    var chargerDropdownExpanded by remember { mutableStateOf(false) }
    
    var startSocText by remember { mutableStateOf(initialStartSoc.toInt().toString()) }
    var targetSocText by remember { mutableStateOf(initialTargetSoc.toInt().toString()) }
    var durationHoursText by remember { mutableStateOf("") }
    var durationMinutesText by remember { mutableStateOf("") }
    
    var customPowerText by remember { mutableStateOf("22.0") }

    val startSocVal = startSocText.toFloatOrNull()
    val targetSocVal = targetSocText.toFloatOrNull()
    val hoursVal = durationHoursText.toIntOrNull()
    val minutesVal = durationMinutesText.toIntOrNull()
    
    val isStartSocValid = startSocVal != null && startSocVal in 0f..100f
    val isTargetSocValid = targetSocVal != null && targetSocVal in 0f..100f && (startSocVal == null || targetSocVal > startSocVal)
    val isDurationValid = (hoursVal != null && hoursVal >= 0) && (minutesVal != null && minutesVal in 0..59) && (hoursVal > 0 || minutesVal > 0)
    
    val powerVal = when (selectedChargerType) {
        "Schuko (2.3 kW)" -> 2.3f
        "Wallbox AC 11 kW" -> 11.0f
        "Wallbox AC 22 kW" -> 22.0f
        "DC Schnelllader 50 kW" -> 50.0f
        else -> customPowerText.toFloatOrNull() ?: 0f
    }
    val isPowerValid = selectedChargerType != "Benutzerdefiniert" || (customPowerText.toFloatOrNull() != null && customPowerText.toFloatOrNull()!! > 0f)

    val canCalibrate = isStartSocValid && isTargetSocValid && isDurationValid && isPowerValid

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ladevorgang kalibrieren") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = selectedChargerType,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Ladeart") },
                        trailingIcon = {
                            Icon(Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { chargerDropdownExpanded = true }
                    )
                    DropdownMenu(
                        expanded = chargerDropdownExpanded,
                        onDismissRequest = { chargerDropdownExpanded = false }
                    ) {
                        chargerTypes.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type) },
                                onClick = {
                                    selectedChargerType = type
                                    chargerDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                if (selectedChargerType == "Benutzerdefiniert") {
                    OutlinedTextField(
                        value = customPowerText,
                        onValueChange = { customPowerText = it },
                        label = { Text("Ladeleistung (kW)") },
                        isError = !isPowerValid,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = startSocText,
                        onValueChange = { startSocText = it },
                        label = { Text("Start-SOC (%)") },
                        isError = !isStartSocValid,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = targetSocText,
                        onValueChange = { targetSocText = it },
                        label = { Text("Ziel-SOC (%)") },
                        isError = !isTargetSocValid,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }

                Text("Tatsächliche Ladedauer:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = durationHoursText,
                        onValueChange = { durationHoursText = it },
                        label = { Text("Stunden") },
                        isError = hoursVal == null || hoursVal < 0,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = durationMinutesText,
                        onValueChange = { durationMinutesText = it },
                        label = { Text("Minuten") },
                        isError = minutesVal == null || minutesVal !in 0..59,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (canCalibrate) {
                        val start = startSocVal!!
                        val target = targetSocVal!!
                        val durationHours = hoursVal!! + (minutesVal!!.toFloat() / 60f)
                        
                        val activeVehicleId = prefsManager.vehiclePresetId
                        val effCap = prefsManager.effectiveCapacity
                        
                        val efficiency = ChargingCalculator.calculateCalibratedEfficiency(
                            startSoc = start,
                            targetSoc = target,
                            actualDurationHours = durationHours,
                            chargerPowerKw = powerVal,
                            effectiveBatteryCapacityKwh = effCap
                        )
                        
                        prefsManager.setChargerEfficiency(activeVehicleId, selectedChargerType, efficiency)
                        onCalibrated(efficiency, selectedChargerType)
                    }
                },
                enabled = canCalibrate
            ) {
                Text("Kalibrieren")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen")
            }
        }
    )
}


package com.neuleo.chargesplit

import android.app.TimePickerDialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
    activePreset: VehiclePreset = prefsManager.selectedPreset,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Initialize state from PreferencesManager for the current preset
    val chargingState = remember(activePreset) {
        prefsManager.getVehicleChargingState(activePreset)
    }

    var startSoc by remember(activePreset) { mutableStateOf(chargingState.currentSoc) }
    var targetSoc by remember(activePreset) { mutableStateOf(chargingState.targetSoc) }
    var chargingMode by remember(activePreset) { mutableStateOf(chargingState.chargingMode) }
    var customAmps by remember(activePreset) { mutableStateOf(chargingState.customAmps) }
    var customPhases by remember(activePreset) { mutableStateOf(chargingState.customPhases) }

    var showCalibrationDialog by remember { mutableStateOf(false) }
    var calibrationResultEfficiency by remember { mutableStateOf<Float?>(null) }
    var calibratedChargerType by remember { mutableStateOf("") }
    var calibrationTrigger by remember { mutableStateOf(0) }

    var startTime by remember { mutableStateOf(Calendar.getInstance()) }
    val formattedTime = String.format(Locale.GERMANY, "%02d:%02d", startTime.get(Calendar.HOUR_OF_DAY), startTime.get(Calendar.MINUTE))

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

    // Immediate state persistence helper
    fun persistState(
        newStartSoc: Float = startSoc,
        newTargetSoc: Float = targetSoc,
        newMode: String = chargingMode,
        newAmps: Int = customAmps,
        newPhases: Int = customPhases
    ) {
        val state = VehicleChargingState(
            currentSoc = newStartSoc,
            targetSoc = newTargetSoc,
            chargingMode = newMode,
            customAmps = newAmps,
            customPhases = newPhases,
            capacityKwh = prefsManager.getBatteryCapacityForPreset(activePreset),
            batteryDegradation = prefsManager.getBatteryDegradationForPreset(activePreset)
        )
        prefsManager.saveVehicleChargingState(activePreset, state)
    }

    // Charger configurations
    val (chargerPhases, chargerAmps, chargerName) = when (chargingMode) {
        "SCHUKO" -> Triple(1, 10, "Schuko (10A)")
        "16A" -> Triple(3, 16, "16A / 11 kW")
        "32A" -> Triple(3, 32, "32A / 22 kW")
        else -> Triple(
            if (activePreset.maxPhases == 1) 1 else customPhases,
            customAmps,
            "Manuell"
        )
    }

    val effectivePhases = minOf(chargerPhases, activePreset.maxPhases)
    val effectiveAmps = if (chargingMode == "32A" && activePreset == VehiclePreset.TESLA_MODEL_S_90D) {
        16 // Standard 11kW OBC on Model S 90D
    } else {
        minOf(chargerAmps, activePreset.maxAmps)
    }

    val grossPowerKw = (effectivePhases * effectiveAmps * 230f) / 1000f
    val effectivePowerKw = minOf(grossPowerKw, activePreset.maxAcKw)
    val isCapped = grossPowerKw > activePreset.maxAcKw || chargerAmps > effectiveAmps || chargerPhases > effectivePhases

    // Calculation
    val capacity = prefsManager.getBatteryCapacityForPreset(activePreset)
    val degradation = prefsManager.getBatteryDegradationForPreset(activePreset)
    val effectiveCapacity = capacity * (1f - degradation / 100f)
    val deltaSoc = (targetSoc - startSoc).coerceAtLeast(0f)
    val energyNeeded = effectiveCapacity * (deltaSoc / 100f)

    val currentEfficiency = remember(activePreset.id, chargerName, calibrationTrigger) {
        prefsManager.getChargerEfficiency(activePreset.id, chargerName, true)
    }
    val gridEnergyDrawn = if (currentEfficiency > 0f) energyNeeded / currentEfficiency else energyNeeded

    val durationMinutes = if (effectivePowerKw > 0f) {
        (gridEnergyDrawn / effectivePowerKw) * 60f
    } else {
        0f
    }

    val electricityPrice = prefsManager.costPerKWh
    val totalCost = gridEnergyDrawn * electricityPrice

    val endTime = remember(startTime, durationMinutes) {
        (startTime.clone() as Calendar).apply {
            add(Calendar.MINUTE, durationMinutes.toInt())
        }
    }
    val formattedEndTime = String.format(Locale.GERMANY, "%02d:%02d Uhr", endTime.get(Calendar.HOUR_OF_DAY), endTime.get(Calendar.MINUTE))

    val hours = durationMinutes.toInt() / 60
    val minutes = durationMinutes.toInt() % 60
    val durationStr = if (hours > 0) "${hours} Std. ${minutes} Min." else "${minutes} Min."

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Hero Result Card
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Berechnete Ladedauer",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                        Text(
                            text = durationStr,
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "Fertig ca. $formattedEndTime",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))

                // 3-Column summary: Energie, Leistung, Kosten
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(horizontalAlignment = Alignment.Start) {
                        Text(
                            text = "Energie",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                        Text(
                            text = String.format(Locale.GERMANY, "+%.1f kWh", gridEnergyDrawn),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Ladeleistung",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                        Text(
                            text = String.format(Locale.GERMANY, "%.2f kW (%dP · %dA)", effectivePowerKw, effectivePhases, effectiveAmps),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Kosten",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                        Text(
                            text = String.format(Locale.GERMANY, "%.2f €", totalCost),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }

        // Capping info note
        if (isCapped) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Begrenzung",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = if (activePreset == VehiclePreset.LEAPMOTOR_T03 && chargerPhases > 1) {
                            "Leapmotor T03 lädt 1-phasig. Ladeleistung wird auf max. ${effectivePowerKw} kW (${effectiveAmps}A) begrenzt."
                        } else if (activePreset == VehiclePreset.TESLA_MODEL_S_90D && chargingMode == "32A") {
                            "Model S 90D lädt mit Standard 11 kW On-Board-Lader (3x16A). Ladeleistung: 11,0 kW."
                        } else {
                            "Ladeleistung wird fahrzeugseitig auf max. ${effectivePowerKw} kW begrenzt."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // 2. Battery SoC Inputs with Visual Progress Indicator & Quick-Action Chips
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Akkustand",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                // Visual progress bar
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    LinearProgressIndicator(
                        progress = { (targetSoc / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Start: ${startSoc.toInt()}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "+${(targetSoc - startSoc).coerceAtLeast(0f).toInt()}% Hub",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Ziel: ${targetSoc.toInt()}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Start-SOC Slider
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Start-Akkustand",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "${startSoc.toInt()}%",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Slider(
                        value = startSoc,
                        onValueChange = {
                            startSoc = it
                            if (targetSoc < it) {
                                targetSoc = it
                            }
                            persistState(newStartSoc = it, newTargetSoc = targetSoc)
                        },
                        valueRange = 0f..100f
                    )
                }

                // Target-SOC Slider & Quick Action Chips
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Ziel-Akkustand",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "${targetSoc.toInt()}%",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Slider(
                        value = targetSoc,
                        onValueChange = {
                            targetSoc = it
                            if (startSoc > it) {
                                startSoc = it
                            }
                            persistState(newStartSoc = startSoc, newTargetSoc = it)
                        },
                        valueRange = 0f..100f
                    )

                    // Quick Action Chips: 80%, 90%, 100%
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(80f, 90f, 100f).forEach { socVal ->
                            FilterChip(
                                selected = targetSoc.toInt() == socVal.toInt(),
                                onClick = {
                                    targetSoc = socVal
                                    if (startSoc > socVal) startSoc = socVal
                                    persistState(newStartSoc = startSoc, newTargetSoc = socVal)
                                },
                                label = { Text("${socVal.toInt()} %") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        // 3. Charging Mode Selection Chips
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Lademethode",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                // 4 FilterChips in a Grid/Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val modes = listOf(
                        "SCHUKO" to "Schuko\n(10A)",
                        "16A" to (if (activePreset == VehiclePreset.LEAPMOTOR_T03) "16A Wallbox\n(3,7 kW)" else "16A Wallbox\n(11 kW)"),
                        "32A" to (if (activePreset == VehiclePreset.LEAPMOTOR_T03) "32A Säule\n(6,6 kW)" else "32A Säule\n(22 kW)"),
                        "MANUAL" to "Manuell\n(Regelbar)"
                    )

                    modes.forEach { (modeKey, label) ->
                        FilterChip(
                            selected = chargingMode == modeKey,
                            onClick = {
                                chargingMode = modeKey
                                persistState(newMode = modeKey)
                            },
                            label = {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (chargingMode == modeKey) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Manual Configuration Section
                AnimatedVisibility(
                    visible = chargingMode == "MANUAL",
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Stromstärke: ${customAmps} A",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (activePreset == VehiclePreset.LEAPMOTOR_T03) {
                                Surface(
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    shape = MaterialTheme.shapes.small
                                ) {
                                    Text(
                                        text = "Fahrzeug lädt 1-phasig",
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        Slider(
                            value = customAmps.toFloat(),
                            onValueChange = {
                                customAmps = it.toInt()
                                persistState(newAmps = it.toInt())
                            },
                            valueRange = 6f..32f,
                            steps = 25
                        )

                        if (activePreset.maxPhases > 1) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Phasenanzahl:",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                FilterChip(
                                    selected = customPhases == 1,
                                    onClick = {
                                        customPhases = 1
                                        persistState(newPhases = 1)
                                    },
                                    label = { Text("1 Phase") }
                                )
                                FilterChip(
                                    selected = customPhases == 3,
                                    onClick = {
                                        customPhases = 3
                                        persistState(newPhases = 3)
                                    },
                                    label = { Text("3 Phasen") }
                                )
                            }
                        }
                    }
                }

                // Efficiency display
                val isCalibrated = remember(activePreset.id, chargerName, calibrationTrigger) {
                    prefsManager.isChargerCalibrated(activePreset.id, chargerName)
                }
                val displayEfficiency = (currentEfficiency * 100f).toInt()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Effizienz: $displayEfficiency% " + if (isCalibrated) "(Kalibriert)" else "(Standard)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (isCalibrated) {
                        TextButton(
                            onClick = {
                                prefsManager.clearChargerEfficiency(activePreset.id, chargerName)
                                calibrationTrigger++
                            },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text("Zurücksetzen", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }

        // 4. Start Time & Calibration Controls
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Startzeit: $formattedTime",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedButton(onClick = { timePickerDialog.show() }) {
                        Text("Zeit wählen")
                    }
                    Button(onClick = { startTime = Calendar.getInstance() }) {
                        Text("Jetzt")
                    }
                }

                OutlinedButton(
                    onClick = { showCalibrationDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Ladevorgang kalibrieren")
                }
            }
        }
    }

    if (showCalibrationDialog) {
        CalibrationDialog(
            prefsManager = prefsManager,
            initialChargerType = chargerName,
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
        "Schuko (10A)",
        "16A / 11 kW",
        "32A / 22 kW",
        "Manuell"
    )

    var selectedChargerType by remember { mutableStateOf(initialChargerType) }
    var chargerDropdownExpanded by remember { mutableStateOf(false) }

    var startSocText by remember { mutableStateOf(initialStartSoc.toInt().toString()) }
    var targetSocText by remember { mutableStateOf(initialTargetSoc.toInt().toString()) }
    var durationHoursText by remember { mutableStateOf("") }
    var durationMinutesText by remember { mutableStateOf("") }
    var customPowerText by remember { mutableStateOf("11.0") }

    val startSocVal = startSocText.toFloatOrNull()
    val targetSocVal = targetSocText.toFloatOrNull()
    val hoursVal = durationHoursText.toIntOrNull()
    val minutesVal = durationMinutesText.toIntOrNull()

    val isStartSocValid = startSocVal != null && startSocVal in 0f..100f
    val isTargetSocValid = targetSocVal != null && targetSocVal in 0f..100f && (startSocVal == null || targetSocVal > startSocVal)
    val isDurationValid = (hoursVal != null && hoursVal >= 0) && (minutesVal != null && minutesVal in 0..59) && (hoursVal > 0 || minutesVal > 0)

    val activePreset = prefsManager.selectedPreset
    val powerVal = when (selectedChargerType) {
        "Schuko (10A)" -> 2.3f
        "16A / 11 kW" -> if (activePreset == VehiclePreset.LEAPMOTOR_T03) 3.68f else 11.0f
        "32A / 22 kW" -> if (activePreset == VehiclePreset.LEAPMOTOR_T03) 6.6f else 11.0f
        else -> customPowerText.toFloatOrNull() ?: 0f
    }
    val isPowerValid = selectedChargerType != "Manuell" || (customPowerText.toFloatOrNull() != null && customPowerText.toFloatOrNull()!! > 0f)

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

                if (selectedChargerType == "Manuell") {
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

                        val activeVehicleId = activePreset.id
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

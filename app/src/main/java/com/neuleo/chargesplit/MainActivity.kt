package com.neuleo.chargesplit

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.neuleo.chargesplit.model.VehiclePreset
import com.neuleo.chargesplit.ui.theme.ChargeSplitTheme
import kotlinx.coroutines.launch
import java.util.Locale

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ChargeSplitTheme {
                val context = LocalContext.current
                val prefsManager = remember {
                    PreferencesManager(context.getSharedPreferences("chargesplit_prefs", Context.MODE_PRIVATE))
                }
                var showSettingsDialog by remember { mutableStateOf(false) }
                var currentPreset by remember { mutableStateOf(prefsManager.selectedPreset) }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        TopAppBar(
                            title = { Text("ChargeSplit") },
                            actions = {
                                IconButton(onClick = { showSettingsDialog = true }) {
                                    Icon(
                                        imageVector = Icons.Default.Settings,
                                        contentDescription = "Einstellungen"
                                    )
                                }
                            }
                        )
                    }
                ) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        MainScreen(
                            prefsManager = prefsManager,
                            currentPreset = currentPreset,
                            onPresetChange = {
                                prefsManager.selectedPreset = it
                                currentPreset = it
                            }
                        )
                        if (showSettingsDialog) {
                            SettingsDialog(
                                prefsManager = prefsManager,
                                onDismiss = {
                                    currentPreset = prefsManager.selectedPreset
                                    showSettingsDialog = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MainScreen(
    prefsManager: PreferencesManager,
    currentPreset: VehiclePreset,
    onPresetChange: (VehiclePreset) -> Unit,
    modifier: Modifier = Modifier
) {
    val tabs = CalculatorUtils.TABS
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val coroutineScope = rememberCoroutineScope()

    val presets = remember(currentPreset) {
        if (currentPreset == VehiclePreset.CUSTOM) {
            listOf(VehiclePreset.TESLA_MODEL_S_90D, VehiclePreset.LEAPMOTOR_T03, VehiclePreset.CUSTOM)
        } else {
            listOf(VehiclePreset.TESLA_MODEL_S_90D, VehiclePreset.LEAPMOTOR_T03)
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Vehicle Quick Switcher directly under TopAppBar
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            presets.forEachIndexed { index, preset ->
                val capacityStr = String.format(Locale.GERMANY, "%.1f", preset.defaultCapacityKwh)
                SegmentedButton(
                    selected = currentPreset == preset,
                    onClick = { onPresetChange(preset) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = presets.size),
                    label = {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            Text(
                                text = preset.shortName,
                                fontWeight = if (currentPreset == preset) FontWeight.Bold else FontWeight.Medium,
                                style = MaterialTheme.typography.labelMedium
                            )
                            Text(
                                text = "$capacityStr kWh · ${preset.maxPhases}P",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                )
            }
        }

        TabRow(
            selectedTabIndex = pagerState.currentPage,
            indicator = { tabPositions ->
                if (tabPositions.isNotEmpty()) {
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[pagerState.currentPage])
                    )
                }
            }
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = pagerState.currentPage == index,
                    onClick = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(index)
                        }
                    },
                    text = { Text(title) }
                )
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            when (page) {
                0 -> ChargeSplitScreen(prefsManager = prefsManager, activePreset = currentPreset)
                1 -> WearScreen(prefsManager = prefsManager, activePreset = currentPreset)
                2 -> LadedauerScreen(prefsManager = prefsManager, activePreset = currentPreset)
            }
        }
    }
}

@Composable
fun ChargeSplitScreen(
    prefsManager: PreferencesManager,
    activePreset: VehiclePreset,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("chargesplit_prefs", Context.MODE_PRIVATE)

    val batteryCapacity = prefsManager.getBatteryCapacityForPreset(activePreset) * (1f - prefsManager.batteryDegradation / 100f)
    val costPerKWh = prefsManager.costPerKWh
    val efficiency = activePreset.acEfficiency

    var startSOC by remember { mutableStateOf(prefs.getFloat("startSOC", 60f)) }
    var endSOC by remember { mutableStateOf(prefs.getFloat("endSOC", 40f)) }
    var passengers by remember { mutableStateOf(prefs.getInt("passengers", 0)) }
    var resultText by remember { mutableStateOf("") }

    LaunchedEffect(startSOC, endSOC, passengers) {
        with(prefs.edit()) {
            putFloat("startSOC", startSOC)
            putFloat("endSOC", endSOC)
            putInt("passengers", passengers)
            apply()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "Fahrzeug: ${activePreset.displayName}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Kapazität: ${String.format(Locale.GERMANY, "%.1f", batteryCapacity)} kWh | Strompreis: ${String.format(Locale.GERMANY, "%.2f", costPerKWh)} €/kWh",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        Text(text = "Ladezustand am Start: ${startSOC.toInt()}%", style = MaterialTheme.typography.bodyLarge)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { if (startSOC > 0f) startSOC = (startSOC - 1f).coerceAtLeast(0f) },
                enabled = startSOC > 0f
            ) {
                Text(text = "-")
            }
            Slider(
                value = startSOC,
                onValueChange = { startSOC = it },
                valueRange = 0f..100f,
                steps = 98,
                modifier = Modifier.weight(1f)
            )
            Button(
                onClick = { if (startSOC < 100f) startSOC = (startSOC + 1f).coerceAtMost(100f) },
                enabled = startSOC < 100f
            ) {
                Text(text = "+")
            }
        }

        Text(text = "Ladezustand am Ende: ${endSOC.toInt()}%", style = MaterialTheme.typography.bodyLarge)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { if (endSOC > 0f) endSOC = (endSOC - 1f).coerceAtLeast(0f) },
                enabled = endSOC > 0f
            ) {
                Text(text = "-")
            }
            Slider(
                value = endSOC,
                onValueChange = { endSOC = it },
                valueRange = 0f..100f,
                steps = 98,
                modifier = Modifier.weight(1f)
            )
            Button(
                onClick = { if (endSOC < 100f) endSOC = (endSOC + 1f).coerceAtMost(100f) },
                enabled = endSOC < 100f
            ) {
                Text(text = "+")
            }
        }

        Text(text = "Anzahl Mitfahrer: $passengers", style = MaterialTheme.typography.bodyLarge)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { if (passengers > 0) passengers-- },
                enabled = passengers > 0
            ) {
                Text(text = "-")
            }
            Button(
                onClick = { if (passengers < 4) passengers++ },
                enabled = passengers < 4
            ) {
                Text(text = "+")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                val result = CalculatorUtils.calculateChargingCost(
                    startSOC = startSOC,
                    endSOC = endSOC,
                    batteryCapacity = batteryCapacity,
                    efficiency = efficiency,
                    costPerKWh = costPerKWh,
                    passengers = passengers
                )

                resultText = "Nachladen: %.2f kWh\nGesamtkosten: €%.2f\nKosten pro Person: €%.2f"
                    .format(result.requiredKWh, result.totalCost, result.costPerPerson)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Berechnen")
        }

        Spacer(modifier = Modifier.height(16.dp))
        if (resultText.isNotEmpty()) {
            Text(text = resultText, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
fun WearScreen(
    prefsManager: PreferencesManager,
    activePreset: VehiclePreset,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("chargesplit_prefs", Context.MODE_PRIVATE)

    val wearCostPer1600Km = prefsManager.getWearCostPer1600ForPreset(activePreset.id)
    val baseMileage = 1600f

    var kilometers by remember { mutableStateOf(prefs.getString("kilometers", "") ?: "") }
    var wearPassengers by remember { mutableStateOf(prefs.getInt("wearPassengers", 0)) }
    var wearResultText by remember { mutableStateOf("") }

    LaunchedEffect(kilometers, wearPassengers) {
        with(prefs.edit()) {
            putString("kilometers", kilometers)
            putInt("wearPassengers", wearPassengers)
            apply()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(text = "Verschleiß-Berechnung", style = MaterialTheme.typography.headlineMedium)
        Text(text = "Basis: €${String.format(Locale.GERMANY, "%.0f", wearCostPer1600Km)} für 1600km (${activePreset.shortName})", style = MaterialTheme.typography.bodyMedium)

        Spacer(modifier = Modifier.height(16.dp))

        Text(text = "Gefahrene Kilometer:", style = MaterialTheme.typography.bodyLarge)
        OutlinedTextField(
            value = kilometers,
            onValueChange = { kilometers = it },
            label = { Text("Kilometer") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        Text(text = "Anzahl Mitfahrer: $wearPassengers", style = MaterialTheme.typography.bodyLarge)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { if (wearPassengers > 0) wearPassengers-- },
                enabled = wearPassengers > 0
            ) {
                Text(text = "-")
            }
            Button(
                onClick = { if (wearPassengers < 4) wearPassengers++ },
                enabled = wearPassengers < 4
            ) {
                Text(text = "+")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                val km = kilometers.toFloatOrNull()
                if (km != null && km > 0) {
                    val result = CalculatorUtils.calculateWearCost(
                        kilometers = km,
                        wearCostPer1600Km = wearCostPer1600Km,
                        baseMileage = baseMileage,
                        passengers = wearPassengers
                    )

                    wearResultText = "Gesamtverschleiß: €%.2f\nKosten pro Person: €%.2f"
                        .format(result.totalWearCost, result.wearCostPerPerson)
                } else {
                    wearResultText = "Bitte geben Sie gültige Kilometer ein"
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Berechnen")
        }

        Spacer(modifier = Modifier.height(16.dp))
        if (wearResultText.isNotEmpty()) {
            Text(text = wearResultText, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

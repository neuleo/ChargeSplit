package com.neuleo.chargesplit

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import com.neuleo.chargesplit.ui.theme.ChargeSplitTheme

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
                        MainScreen(prefsManager = prefsManager)
                        if (showSettingsDialog) {
                            SettingsDialog(
                                prefsManager = prefsManager,
                                onDismiss = { showSettingsDialog = false }
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
fun MainScreen(prefsManager: PreferencesManager, modifier: Modifier = Modifier) {
    val tabs = CalculatorUtils.TABS
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val coroutineScope = rememberCoroutineScope()

    Column(modifier = modifier.fillMaxSize()) {
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
                0 -> ChargeSplitScreen()
                1 -> WearScreen()
                2 -> LadedauerScreen(prefsManager = prefsManager)
            }
        }
    }
}

@Composable
fun ChargeSplitScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("chargesplit_prefs", Context.MODE_PRIVATE)

    // EV variables loaded from preferences
    val batteryCapacity = prefs.getFloat("pref_battery_capacity", 70f)
    val costPerKWh = prefs.getFloat("pref_cost_per_kwh", 0.35f)
    val efficiency = prefs.getFloat("pref_efficiency", 0.90f)

    // Zustände für die Eingaben - mit gespeicherten Werten initialisieren
    var startSOC by remember { mutableStateOf(prefs.getFloat("startSOC", 60f)) }
    var endSOC by remember { mutableStateOf(prefs.getFloat("endSOC", 40f)) }
    var passengers by remember { mutableStateOf(prefs.getInt("passengers", 0)) }
    var resultText by remember { mutableStateOf("") }

    // Werte speichern bei Änderung
    LaunchedEffect(startSOC, endSOC, passengers) {
        with(prefs.edit()) {
            putFloat("startSOC", startSOC)
            putFloat("endSOC", endSOC)
            putInt("passengers", passengers)
            apply()
        }
    }

    // UI
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
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
fun WearScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("chargesplit_prefs", Context.MODE_PRIVATE)

    // Wear variables loaded from preferences
    val wearCostPer1600Km = prefs.getFloat("pref_wear_cost_per_1600", 60f)
    val baseMileage = 1600f

    // Zustände für die Eingaben - mit gespeicherten Werten initialisieren
    var kilometers by remember { mutableStateOf(prefs.getString("kilometers", "")!!) }
    var wearPassengers by remember { mutableStateOf(prefs.getInt("wearPassengers", 0)) }
    var wearResultText by remember { mutableStateOf("") }

    // Werte speichern bei Änderung
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
        Text(text = "Basis: €60 für 1600km", style = MaterialTheme.typography.bodyMedium)

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

@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("chargesplit_prefs", Context.MODE_PRIVATE)

    // Load initial values from SharedPreferences (efficiency is stored as factor e.g. 0.90f, so convert to 90f for UI)
    var batteryText by remember { mutableStateOf(prefs.getFloat("pref_battery_capacity", 70f).toString()) }
    var costText by remember { mutableStateOf(prefs.getFloat("pref_cost_per_kwh", 0.35f).toString()) }
    var efficiencyText by remember { mutableStateOf((prefs.getFloat("pref_efficiency", 0.90f) * 100f).toInt().toString()) }
    var wearText by remember { mutableStateOf(prefs.getFloat("pref_wear_cost_per_1600", 60f).toString()) }

    // Validation error states
    var batteryError by remember { mutableStateOf(false) }
    var costError by remember { mutableStateOf(false) }
    var efficiencyError by remember { mutableStateOf(false) }
    var wearError by remember { mutableStateOf(false) }

    // Save states on change if valid
    LaunchedEffect(batteryText) {
        val validated = SettingsValidator.validateBatteryCapacity(batteryText)
        if (validated != null) {
            prefs.edit().putFloat("pref_battery_capacity", validated).apply()
            batteryError = false
        } else {
            batteryError = true
        }
    }

    LaunchedEffect(costText) {
        val validated = SettingsValidator.validateCostPerKWh(costText)
        if (validated != null) {
            prefs.edit().putFloat("pref_cost_per_kwh", validated).apply()
            costError = false
        } else {
            costError = true
        }
    }

    LaunchedEffect(efficiencyText) {
        val validated = SettingsValidator.validateEfficiency(efficiencyText)
        if (validated != null) {
            prefs.edit().putFloat("pref_efficiency", validated).apply()
            efficiencyError = false
        } else {
            efficiencyError = true
        }
    }

    LaunchedEffect(wearText) {
        val validated = SettingsValidator.validateWearCost(wearText)
        if (validated != null) {
            prefs.edit().putFloat("pref_wear_cost_per_1600", validated).apply()
            wearError = false
        } else {
            wearError = true
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(text = "Einstellungen", style = MaterialTheme.typography.headlineMedium)
        Text(
            text = "Passen Sie die Fahrzeug- und Stromparameter an. Änderungen werden sofort gespeichert.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Battery Capacity
        OutlinedTextField(
            value = batteryText,
            onValueChange = { batteryText = it },
            label = { Text("Batteriekapazität (kWh)") },
            isError = batteryError,
            supportingText = {
                if (batteryError) {
                    Text("Geben Sie eine positive Zahl ein (z. B. 70.0)")
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        // Cost per kWh
        OutlinedTextField(
            value = costText,
            onValueChange = { costText = it },
            label = { Text("Strompreis (€/kWh)") },
            isError = costError,
            supportingText = {
                if (costError) {
                    Text("Geben Sie einen gültigen Preis ein (z. B. 0.35)")
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        // Efficiency
        OutlinedTextField(
            value = efficiencyText,
            onValueChange = { efficiencyText = it },
            label = { Text("Ladeeffizienz (%)") },
            isError = efficiencyError,
            supportingText = {
                if (efficiencyError) {
                    Text("Geben Sie einen Wert zwischen 1 und 100 ein (z. B. 90)")
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        // Wear cost per 1600km
        OutlinedTextField(
            value = wearText,
            onValueChange = { wearText = it },
            label = { Text("Verschleißkosten pro 1600km (€)") },
            isError = wearError,
            supportingText = {
                if (wearError) {
                    Text("Geben Sie einen gültigen Betrag ein (z. B. 60.0)")
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
    }
}
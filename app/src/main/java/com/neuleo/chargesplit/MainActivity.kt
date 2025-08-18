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
import androidx.compose.foundation.text.KeyboardOptions
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
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        TopAppBar(title = { Text("ChargeSplit") })
                    }
                ) { innerPadding ->
                    MainScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MainScreen(modifier: Modifier = Modifier) {
    val tabs = listOf("Ladekosten", "Verschleiß")
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
            }
        }
    }
}

@Composable
fun ChargeSplitScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("chargesplit_prefs", Context.MODE_PRIVATE)

    // Fest eingetragene Variablen
    val batteryCapacity = 70f      // in kWh
    val costPerKWh = 0.35f         // in Euro
    val efficiency = 0.90f         // Ladeeffizienz

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
                val deltaSOC = (startSOC - endSOC).coerceAtLeast(0f)
                val requiredKWh = (deltaSOC / 100f * batteryCapacity) / efficiency
                val totalCost = requiredKWh * costPerKWh
                val numPeople = passengers + 1
                val costPerPerson = totalCost / numPeople

                resultText = "Nachladen: %.2f kWh\nGesamtkosten: €%.2f\nKosten pro Person: €%.2f"
                    .format(requiredKWh, totalCost, costPerPerson)
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

    // Verschleiß-Basis: 60€ für 1600km
    val wearCostPer1600Km = 60f
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
                    // Verschleißkosten berechnen: (gefahrene_km / 1600km) * 60€
                    val totalWearCost = (km / baseMileage) * wearCostPer1600Km
                    val numPeople = wearPassengers + 1
                    val wearCostPerPerson = totalWearCost / numPeople

                    wearResultText = "Gesamtverschleiß: €%.2f\nKosten pro Person: €%.2f"
                        .format(totalWearCost, wearCostPerPerson)
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
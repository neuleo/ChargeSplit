package com.neuleo.chargesplit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
                    ChargeSplitScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun ChargeSplitScreen(modifier: Modifier = Modifier) {
    // Fest eingetragene Variablen
    val batteryCapacity = 70f      // in kWh
    val costPerKWh = 0.35f         // in Euro
    val efficiency = 0.90f         // Ladeeffizienz

    // Zustände für die Eingaben
    var startSOC by remember { mutableStateOf(60f) }
    var endSOC by remember { mutableStateOf(40f) }
    var passengers by remember { mutableStateOf(0) }  // 0 bis 4 Mitfahrer
    var resultText by remember { mutableStateOf("") }

    // UI
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(text = "Ladezustand an Start: ${startSOC.toInt()}%", style = MaterialTheme.typography.bodyLarge)
        Slider(
            value = startSOC,
            onValueChange = { startSOC = it },
            valueRange = 0f..100f,
            steps = 98  // (100 - 0 - 1)
        )

        Text(text = "Ladezustand am Ende: ${endSOC.toInt()}%", style = MaterialTheme.typography.bodyLarge)
        Slider(
            value = endSOC,
            onValueChange = { endSOC = it },
            valueRange = 0f..100f,
            steps = 98
        )

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
                // Berechnung:
                // Es wird davon ausgegangen, dass nur der Fall betrachtet wird,
                // in dem der StartSOC größer als der EndSOC ist.
                // Falls das nicht der Fall ist, wird deltaSOC als 0 angenommen.
                val deltaSOC = (startSOC - endSOC).coerceAtLeast(0f)
                // Benötigte kWh, um den verbrauchten Anteil wieder aufzuladen:
                // Hinweis: Aufgrund der Ladeeffizienz muss mehr Energie eingekauft werden.
                val requiredKWh = (deltaSOC / 100f * batteryCapacity) / efficiency
                // Gesamtkosten:
                val totalCost = requiredKWh * costPerKWh
                // Kosten pro Person (ich + Mitfahrer):
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

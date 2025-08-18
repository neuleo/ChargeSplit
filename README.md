# ChargeSplit

Eine Android-App zur fairen Aufteilung von Elektroauto-Kosten bei Fahrgemeinschaften.

## Funktionen

### 📱 Zwei-Tab-Interface
- **Ladekosten**: Berechnung der Stromkosten basierend auf Batteriezustand
- **Verschleiß**: Berechnung der Verschleißkosten basierend auf gefahrenen Kilometern
- Navigation per Tab-Klick oder Swipe-Geste

### ⚡ Ladekosten-Rechner
- Eingabe des Ladezustands am Start und Ende der Fahrt
- Präzise Einstellung über Slider + Plus/Minus Buttons
- Berücksichtigung der Ladeeffizienz (90%)
- Automatische Berechnung der benötigten kWh und Kosten
- Faire Aufteilung auf alle Personen (Fahrer + bis zu 4 Mitfahrer)

### 🚗 Verschleiß-Rechner  
- Eingabe der gefahrenen Kilometer
- Basis: 60€ Verschleißkosten pro 1600km
- Aufteilung auf alle Personen (Fahrer + bis zu 4 Mitfahrer)

### 💾 Datenspeicherung
- Automatische Speicherung aller Eingaben
- Werte werden beim nächsten App-Start wiederhergestellt
- Getrennte Speicherung für beide Tabs

## Technische Details

### Konfiguration
```kotlin
val batteryCapacity = 70f      // Batteriekapazität in kWh
val costPerKWh = 0.35f         // Strompreis in Euro pro kWh
val efficiency = 0.90f         // Ladeeffizienz (90%)
val wearCostPer1600Km = 60f    // Verschleißkosten für 1600km
```

### Technologie-Stack
- **Sprache**: Kotlin
- **UI Framework**: Jetpack Compose
- **Navigation**: HorizontalPager mit TabRow
- **Datenspeicherung**: SharedPreferences
- **Architektur**: Single Activity mit Compose

## Installation

1. Android Studio öffnen
2. Projekt klonen oder herunterladen
3. Projekt in Android Studio öffnen
4. App auf Gerät oder Emulator ausführen

## Verwendung

### Ladekosten berechnen
1. **Startladezustand** einstellen (Slider oder +/- Buttons)
2. **Endladezustand** einstellen (nach der Fahrt)
3. **Anzahl Mitfahrer** wählen (0-4)
4. **"Berechnen"** drücken
5. Ergebnis zeigt: benötigte kWh, Gesamtkosten, Kosten pro Person

### Verschleiß berechnen
1. Zum **"Verschleiß"** Tab wechseln
2. **Gefahrene Kilometer** eingeben
3. **Anzahl Mitfahrer** wählen (0-4)
4. **"Berechnen"** drücken
5. Ergebnis zeigt: Gesamtverschleiß, Kosten pro Person

## Berechnungslogik

### Ladekosten
```
Verbrauchte Energie = (StartSOC - EndSOC) × Batteriekapazität
Benötigte kWh = Verbrauchte Energie ÷ Ladeeffizienz
Gesamtkosten = Benötigte kWh × Strompreis
Kosten pro Person = Gesamtkosten ÷ (Mitfahrer + 1)
```

### Verschleiß
```
Verschleißfaktor = Gefahrene km ÷ 1600km
Gesamtverschleiß = Verschleißfaktor × 60€
Kosten pro Person = Gesamtverschleiß ÷ (Mitfahrer + 1)
```

## Anpassungsmöglichkeiten

Die Werte können in der `MainActivity.kt` angepasst werden:
- Batteriekapazität deines Elektroautos
- Aktueller Strompreis
- Ladeeffizienz deines Ladegeräts
- Verschleißkosten-Basis

## Lizenz

Dieses Projekt ist für den persönlichen Gebrauch entwickelt.

## Entwicklung

Entwickelt mit Android Studio und Jetpack Compose für eine moderne, intuitive Benutzeroberfläche.

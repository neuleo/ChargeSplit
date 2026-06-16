# Specification: EV Settings Customization

## Overview
Currently, the calculator constants (battery capacity, charging efficiency, electricity price, and wear cost base) are hardcoded in `MainActivity.kt`. This feature allows users to dynamically configure these parameters through a new "Einstellungen" (Settings) tab in the app's user interface.

## User Story
As an EV driver, I want to configure the specific battery capacity, charging efficiency, electricity cost, and wear-and-tear rate of my vehicle in the app so that the trip cost calculations are accurate for my car and energy tariff.

## Requirements

### 1. Data Persistence (SharedPreferences)
Store the following settings in the `chargesplit_prefs` SharedPreferences file:
- `pref_battery_capacity` (Float) - Default: `70f`
- `pref_cost_per_kwh` (Float) - Default: `0.35f`
- `pref_efficiency` (Float) - Default: `0.90f`
- `pref_wear_cost_per_1600` (Float) - Default: `60f`

### 2. Calculation Integration
Modify the calculation logic in `ChargeSplitScreen` and `WearScreen` to fetch these parameters from SharedPreferences rather than using local hardcoded values.

### 3. Settings User Interface ("Einstellungen" Tab)
- Add a third tab to the main pager: **Einstellungen**
- The screen should provide text fields for:
  - **Batteriekapazität (kWh)**: positive float (e.g. 70.0)
  - **Strompreis (€/kWh)**: positive float (e.g. 0.35)
  - **Ladeeffizienz (%)**: positive float up to 100 (e.g. 90.0, stored as 0.90)
  - **Verschleißkosten pro 1600km (€)**: positive float (e.g. 60.0)
- Validation:
  - Ensure inputs are valid positive numbers.
  - Save changes automatically using `LaunchedEffect` or explicit save handlers upon valid input.
  - Show error messages/formatting if inputs are invalid.

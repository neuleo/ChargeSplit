# Specification: Vehicle Presets & Charging Time Calculator

## Overview
This feature extends ChargeSplit with two major additions:
1. A **Vehicle Preset System** with a settings dialog (gear icon in TopBar), allowing users to choose from predefined car profiles (Tesla Model S P85, Leapmotor T03) or configure a custom vehicle.
2. A new **"Ladedauer"** tab implementing a charging time calculator that computes how long it takes to charge from a start SOC% to a target SOC%, based on the selected charging type and the active vehicle preset.

## User Story
As an EV driver, I want to select my specific vehicle (or configure a custom one), set my electricity price and degradation, and then calculate exactly how long it will take to charge from a given SOC to a target SOC with a specific charger — including the estimated end time and total cost.

---

## Part 1: Vehicle Preset System (Settings Dialog)

### 1.1 Settings Entry Point
- A gear icon (⚙️) button is added to the **TopBar** of the app, visible on all tabs.
- Tapping it opens a **Modal Bottom Sheet / Dialog** (Jetpack Compose `ModalBottomSheet` or `AlertDialog`).

### 1.2 Vehicle Presets
Users can select one of:

| Preset | Nominal kWh | Usable kWh | Max AC kW | Max DC kW | AC Efficiency | DC Efficiency |
|---|---|---|---|---|---|---|
| **Tesla Model S P85** | 85 kWh | 80.8 kWh | 11 kW | 120 kW | 91% | 93% |
| **Leapmotor T03** | 37.3 kWh | 36.0 kWh | 6.6 kW | 48 kW | 90% | 92% |
| **Custom** | user-defined | auto-calc | user-defined | user-defined | user-defined | user-defined |

When a preset is selected, all fields are auto-filled.

### 1.3 Configurable Parameters (all persisted in SharedPreferences)
- **Fahrzeugprofil** (Preset selector: Tesla S P85 / Leapmotor T03 / Custom)
- **Nominale Batteriekapazität (kWh)** — e.g. 85.0
- **Batteriedegradation (%)** — e.g. 5.0 → effective capacity = nominal × (1 - degradation/100)
- **Effective Nutzbare Kapazität (kWh)** — read-only, auto-calculated, displayed for clarity
- **Strompreis (€/kWh)** — e.g. 0.35
- **AC Ladeeffizienz (%)** — e.g. 91 (preset-specific or custom)
- **DC Ladeeffizienz (%)** — e.g. 93 (preset-specific or custom)

### 1.4 Persistence
All settings are stored in the existing `chargesplit_prefs` SharedPreferences file with new keys:
- `pref_vehicle_preset` (String: "tesla_s_p85" | "leapmotor_t03" | "custom")
- `pref_battery_nominal_kwh` (Float)
- `pref_battery_degradation` (Float)
- `pref_ac_efficiency` (Float)
- `pref_dc_efficiency` (Float)

---

## Part 2: Ladedauer Tab (Charging Time Calculator)

### 2.1 New Tab
- A new tab **"Ladedauer"** is added to the existing `HorizontalPager` tab layout.
- Tab order: Ladekosten | Verschleiß | Ladedauer | (Einstellungen as existing tab or replaced by modal)

### 2.2 Inputs
- **Start-SOC (%)** — slider + numeric field (0–100)
- **Ziel-SOC (%)** — slider + numeric field (0–100)
- **Startzeit** — time picker (default: current time)
- **Ladeart auswahl** (single select):
  - Schuko (230V / 10A) → 2.3 kW
  - Wallbox AC 11 kW
  - Wallbox AC 22 kW (capped by car's max AC onboard charger)
  - DC Schnelllader 50 kW (capped by car's max DC rate)
  - Benutzerdefiniert (user enters kW value manually)

### 2.3 Calculation Logic
1. `effectiveCapacity = nominalCapacity * (1 - degradation/100)`
2. `energyNeeded = effectiveCapacity * (targetSOC - startSOC) / 100`
3. `chargingPower = min(selectedChargerKW, vehicleMaxChargerKW)` — capped by vehicle's max power for that type
4. `efficiency = acEfficiency or dcEfficiency` (based on charger type)
5. `gridEnergyDrawn = energyNeeded / efficiency`
6. `durationMinutes = (gridEnergyDrawn / chargingPower) * 60`
7. `totalCost = gridEnergyDrawn * costPerKWh`
8. `estimatedEndTime = startTime + durationMinutes`

### 2.4 Results Display
- **Ladedauer**: e.g. "1h 23min"
- **Energie (Netz)**: e.g. "22.4 kWh"
- **Kosten**: e.g. "7,84 €"
- **Startzeit**: e.g. "14:30"
- **Geschätzte Fertigzeit**: e.g. "15:53"
- **Hinweis**: if selected charger power exceeds vehicle's max rate, show an info note explaining the capping.

---

## Acceptance Criteria
- [ ] Gear icon in TopBar opens Settings dialog on all tabs
- [ ] Selecting a preset auto-fills all vehicle parameters
- [ ] Degradation is applied: effective capacity is clearly shown
- [ ] All settings persist across app restarts
- [ ] Ladedauer tab is accessible via swipe and tab click
- [ ] Charging power is correctly capped by vehicle's onboard charger limit
- [ ] All results (time, energy, cost, end time) are correctly calculated
- [ ] Custom charger kW input works
- [ ] Start time picker works; estimated end time shown correctly

## Out of Scope
- Real-time charging curves (the calculator uses a simplified linear model)
- Network connectivity or cloud sync
- Multiple user profiles
- Navigation / route planning

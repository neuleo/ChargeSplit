# Plan: Vehicle Presets & Charging Time Calculator

## Phase 1: Vehicle Preset Data Model & Settings Infrastructure [checkpoint: ec2dd80]

- [x] Task: Define `VehiclePreset` data class with all fields (nominalKwh, usableKwh, maxAcKw, maxDcKw, acEfficiency, dcEfficiency) bbb654b
    - [x] Create `VehiclePreset.kt` in the model package
    - [x] Define the three presets as constants: `TESLA_S_P85`, `LEAPMOTOR_T03`, `CUSTOM`
- [x] Task: Extend SharedPreferences with new preference keys 672817a
    - [x] Add keys: `pref_vehicle_preset`, `pref_battery_nominal_kwh`, `pref_battery_degradation`, `pref_ac_efficiency`, `pref_dc_efficiency`
    - [x] Create/extend `PreferencesManager.kt` with typed read/write helpers for all settings
- [x] Task: Write unit tests for VehiclePreset data model and PreferencesManager 672817a
    - [x] Test preset auto-fill populates all fields correctly
    - [x] Test effective capacity calculation: `nominal * (1 - degradation/100)`
    - [x] Test persistence round-trip (write then read)
- [x] Task: Implement PreferencesManager (make failing tests pass) 672817a
- [x] Task: Conductor - User Manual Verification 'Phase 1' (Protocol in workflow.md) ec2dd80

## Phase 2: Settings Dialog (Gear Icon + Modal)

- [x] Task: Add gear icon button to TopBar (`MainActivity.kt` or top-level composable) 1ffff1e
    - [x] Add `IconButton` with `Icons.Default.Settings` to the TopAppBar
    - [x] Wire button to toggle a `showSettingsDialog` state
- [x] Task: Build `SettingsDialog` composable 1ffff1e
    - [x] Vehicle preset selector (segmented or dropdown: Tesla S P85 / Leapmotor T03 / Custom)
    - [x] Auto-fill all fields when preset changes
    - [x] Fields: Nominale Kapazität, Degradation %, Effective Kapazität (read-only, auto-calc), Strompreis, AC Effizienz, DC Effizienz
    - [x] Validation: positive numbers only, degradation 0–100, efficiencies 50–100
    - [x] Save button persists all values to SharedPreferences
    - [x] Cancel/dismiss discards unsaved changes
- [x] Task: Write tests for SettingsDialog composable 1ffff1e
    - [x] Test preset selection triggers correct auto-fill
    - [x] Test read-only effective capacity updates when nominal/degradation changes
    - [x] Test validation rejects invalid inputs
- [x] Task: Implement SettingsDialog (make tests pass) 1ffff1e
- [ ] Task: Conductor - User Manual Verification 'Phase 2' (Protocol in workflow.md)

## Phase 3: Ladedauer Tab - UI

- [ ] Task: Add "Ladedauer" tab to `HorizontalPager` tab layout in `MainActivity`
    - [ ] Register new tab with label "Ladedauer"
    - [ ] Create placeholder `LadedauerScreen` composable file
- [ ] Task: Implement `LadedauerScreen` input section
    - [ ] Start-SOC slider (0–100) + numeric input field
    - [ ] Ziel-SOC slider (0–100) + numeric input field
    - [ ] Start-Zeit picker (TimePickerDialog, defaulting to current time)
    - [ ] Ladeart selector (Radio buttons or DropdownMenu):
        - Schuko 2.3 kW
        - Wallbox AC 11 kW
        - Wallbox AC 22 kW
        - DC Schnelllader 50 kW
        - Benutzerdefiniert (custom kW text field shown when selected)
- [ ] Task: Write UI tests for LadedauerScreen inputs
    - [ ] Test SOC sliders enforce min/max and that Ziel ≥ Start
    - [ ] Test custom charger field appears only when "Benutzerdefiniert" is selected
- [ ] Task: Conductor - User Manual Verification 'Phase 3' (Protocol in workflow.md)

## Phase 4: Ladedauer Tab - Calculation Logic & Results

- [ ] Task: Implement `ChargingCalculator` utility object / class
    - [ ] Function `calculateChargingDuration(startSoc, targetSoc, chargerKw, vehiclePreset, electricityPrice): ChargingResult`
    - [ ] Apply vehicle max AC/DC cap: `effectiveChargerKw = min(chargerKw, vehicleMax)`
    - [ ] Use appropriate efficiency (AC or DC) based on charger type
    - [ ] Return: `durationMinutes`, `gridEnergyKwh`, `totalCostEur`, `estimatedEndTime`
- [ ] Task: Write unit tests for `ChargingCalculator`
    - [ ] Test Tesla S P85: 20%→80% via Wallbox 11kW → expected ~4h 3min, correct cost
    - [ ] Test Leapmotor T03: 10%→90% via DC 50kW (capped at 48kW) → expected ~37min
    - [ ] Test power capping: 22kW AC on Leapmotor T03 (max 6.6kW) → uses 6.6kW, shows info note
    - [ ] Test Schuko 2.3kW base case
    - [ ] Test custom kW input
- [ ] Task: Implement `ChargingCalculator` (make tests pass)
- [ ] Task: Wire calculation to `LadedauerScreen`
    - [ ] Read current vehicle settings from PreferencesManager
    - [ ] Display results: Ladedauer, Energie, Kosten, Startzeit, Fertigzeit
    - [ ] Show capping info note when applicable
- [ ] Task: Conductor - User Manual Verification 'Phase 4' (Protocol in workflow.md)

## Phase 5: Integration, Refinement & Cleanup

- [ ] Task: Integrate new settings (preset, degradation) into existing `ChargeSplitScreen` calculation
    - [ ] Replace hardcoded battery capacity with `effectiveCapacity` from PreferencesManager
    - [ ] Replace hardcoded AC efficiency with preset's `acEfficiency`
- [ ] Task: Lint & code style check
    - [ ] Run `./gradlew lint` and fix all warnings
- [ ] Task: Full test suite run
    - [ ] Run `./gradlew test` — all tests must pass
- [ ] Task: Conductor - User Manual Verification 'Phase 5' (Protocol in workflow.md)

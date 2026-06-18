# Implementation Plan: Charging Time Calibration & Preset Editing Overrides

## Phase 1: Preset Overrides and Charger-Specific Efficiency Storage [checkpoint: 8b2a0f0]
- [x] Task: Model and Preferences Layer Extensions (TDD) [ce9759b]
    - [x] Write unit tests in `PreferencesManagerTest.kt` for saving and loading charger-specific efficiencies (e.g. `pref_<vehicle_id>_efficiency_<charger_type>`) and vehicle-specific preset parameter overrides (e.g. `pref_<vehicle_id>_battery_nominal_kwh`).
    - [x] Implement reading and writing of these custom overrides in `PreferencesManager`.
- [x] Task: Settings Dialog Preset Overrides (TDD) [312fb28]
    - [x] Write unit tests verifying that editing fields in the settings dialog does not reset the profile selection to "Custom" and correctly saves specific preset overrides.
    - [x] Update `SettingsDialog` and `MainActivity` to persist overrides under the active vehicle preset name instead of resetting to "Custom".
- [x] Task: Conductor - User Manual Verification 'Phase 1: Preset Overrides and Charger-Specific Efficiency Storage' (Protocol in workflow.md) [8b2a0f0]

## Phase 2: Calibration Calculation Logic
- [ ] Task: Implement Calibration Formula (TDD)
    - [ ] Write unit tests in `ChargingCalculatorTest.kt` verifying that the calibration formula correctly calculates efficiency based on start SOC, target SOC, charger power, and actual duration.
    - [ ] Implement `ChargingCalculator.calculateCalibratedEfficiency` logic.
- [ ] Task: Conductor - User Manual Verification 'Phase 2: Calibration Calculation Logic' (Protocol in workflow.md)

## Phase 3: UI Integration and Wiring
- [ ] Task: Build Calibration Wizard Dialog UI (Manual Verification)
    - [ ] Implement a Calibration Dialog in `LadedauerScreen.kt` featuring input fields for charger type, start/target SOC, and measured actual duration.
    - [ ] Wire the "Kalibrieren" button to perform the calculation and save the result as a charger-specific override for the active vehicle preset.
- [ ] Task: Wire Ladedauer Calculation to Charger-Specific Efficiency (TDD)
    - [ ] Write unit tests verifying that the charging duration calculation loads and uses the charger-specific efficiency for the active vehicle and charger type.
    - [ ] Wire `LadedauerScreen` to use the charger-specific efficiency from the preferences manager during calculation.
- [ ] Task: Conductor - User Manual Verification 'Phase 3: UI Integration and Wiring' (Protocol in workflow.md)

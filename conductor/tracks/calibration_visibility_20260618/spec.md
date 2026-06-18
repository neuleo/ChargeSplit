# Specification: Calibration Values Visibility and Management

## Overview
Currently, the application allows users to calibrate their charging efficiency per charger type for each vehicle profile. However, once the calibration is done, the saved efficiency value is not displayed anywhere in the user interface. This specification addresses this by making the calibrated efficiency values visible on both the LadedauerScreen (charging duration screen) and inside the Vehicles Settings Dialog (Fahrzeugeinstellungen). It also adds capability to clear/reset these custom calibrations.

## Functional Requirements
1. **LadedauerScreen Visibility**:
   - Display the currently used charging efficiency under the "Ladeart" selector.
   - The label should show the current value (e.g., `Ladeeffizienz: 85% (Kalibriert)` or `Ladeeffizienz: 90% (Standard)`).
   - If the value is calibrated, provide a way (e.g., a "Zurücksetzen" button or link) to clear this calibration directly on the screen.
2. **SettingsDialog Visibility & Management**:
   - Inside the `SettingsDialog`, under the active vehicle preset, list all chargers that have calibrated values.
   - For each calibrated charger, show the charger type and the calibrated efficiency (e.g., `Schuko (2.3 kW): 86%`).
   - Provide a "Delete" or "Reset" (Mülleimer/Löschen) icon button next to each item to remove the custom calibration for that charger/vehicle combination.
3. **PreferencesManager Integration**:
   - Ensure the UI correctly queries `PreferencesManager` for whether a custom calibration exists.
   - Expose a reset/clear function in `PreferencesManager` (e.g., `clearChargerEfficiency(vehicleId, chargerType)`) to delete the specific key from SharedPreferences.
4. **State updates**:
   - Ensure that when a calibration is completed, cleared, or reset, the state is correctly refreshed on both the `LadedauerScreen` and inside the `SettingsDialog` without requiring app restarts.

## Acceptance Criteria
- Selecting a charger on the `LadedauerScreen` displays the correct efficiency with `(Standard)` or `(Kalibriert)` suffix.
- When calibrated, the suffix changes to `(Kalibriert)` and a "Zurücksetzen" (Reset) option is visible. Clicking "Zurücksetzen" deletes the calibration and falls back to standard.
- The `SettingsDialog` lists all calibrated efficiencies for the selected vehicle preset.
- Clicking the delete icon next to a calibrated charger in `SettingsDialog` deletes the calibration, updates the list, and falls back to standard efficiency.
- All unit tests pass and new unit tests are added to verify the custom efficiency reset logic.

## Out of Scope
- Adding manually editable efficiency fields for individual chargers (only editing via general preset or via calibration wizard).

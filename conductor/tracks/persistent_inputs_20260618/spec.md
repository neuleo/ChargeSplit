# Specification: Persistent Input Values for LadedauerScreen

## Overview
Currently, some screens like `ChargeSplitScreen` and `WearScreen` persist user inputs across app restarts, but the `LadedauerScreen` does not. When the app is closed and reopened, the selected Start-SOC, Target-SOC, Charger Type, and Custom Charger Power are reset to default values. This track implements persistence for these fields so that user inputs are retained across app restarts.

## Functional Requirements
1. **Inputs Persistence**:
   - Persist the following user inputs from `LadedauerScreen` across app restarts:
     - **Start-SOC** (default: `20f`)
     - **Target-SOC** (default: `80f`)
     - **Charger Type** (default: `"Wallbox AC 11 kW"`)
     - **Custom Charger Power** (default: `"22.0"`)
   - **Start Time (Startzeit)**:
     - Will not be persisted across app restarts (it will always default to the current system time when the app is launched).
     - During the active app session, changes to the Startzeit are kept in memory.
2. **PreferencesManager Integration**:
   - Add getters and setters to `PreferencesManager` for:
     - `ladedauerStartSoc`
     - `ladedauerTargetSoc`
     - `ladedauerChargerType`
     - `ladedauerCustomChargerKw`
   - Use distinct keys (e.g., `pref_ladedauer_start_soc`, etc.) in SharedPreferences.
3. **UI Integration**:
   - In `LadedauerScreen.kt`, load the initial state values from `PreferencesManager`.
   - Automatically save the new values to `PreferencesManager` when they change (e.g., using `LaunchedEffect` or setting them directly on value change / slider release).

## Acceptance Criteria
- Adjusting the Start-SOC, Target-SOC, Charger Type, or Custom Charger Power on `LadedauerScreen`, closing the app, and reopening it restores the last entered values.
- Reopening the app always initializes the Startzeit to the current system time.
- All unit tests pass and new unit tests are added to verify the getters/setters in `PreferencesManager` for Ladedauer inputs.

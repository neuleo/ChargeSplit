# Specification: Charging Time Calibration & Preset Editing Overrides

## Overview
This feature introduces a charging time calibration wizard to calculate and save actual charging efficiencies per charger type for each vehicle profile. It also resolves a bug where editing default vehicle preset parameters resets the active profile to "Custom", allowing direct overrides to default presets instead.

## Functional Requirements
1. **Calibration Wizard (Kalibrierungs-Assistent)**:
   - A new button labeled "Ladevorgang kalibrieren" is added to the `LadedauerScreen` near the calculations card.
   - Tapping it opens a dialog requesting the following inputs from the user:
     - Charger type (defaults to the currently selected charger on the screen).
     - Start SOC (%) and Target SOC (%).
     - Actual measured charging duration (Hours and Minutes).
   - Upon clicking "Kalibrieren", the app calculates the effective charging efficiency based on the actual duration and charger power:
     $$\text{efficiency} = \frac{\text{energyNeededKWh}}{\text{actualDurationHours} \times \text{chargerPowerKw}}$$
     where $\text{energyNeededKWh} = \text{effectiveBatteryCapacityKWh} \times \frac{\text{targetSoc} - \text{startSoc}}{100}$.
   - The calculated efficiency is saved as a charger-specific efficiency override for the active vehicle preset in `SharedPreferences`.
   - A success confirmation displays the calculated efficiency (e.g. "Calibrated efficiency: 82%").

2. **Charger-Specific Efficiencies**:
   - Store and retrieve separate charging efficiency values for each combination of vehicle preset and charger type in `SharedPreferences` (e.g., `pref_<vehicle_id>_efficiency_<charger_type>`).
   - If no calibrated override exists, the following default values are used:
     - Schuko: 85%
     - Wallbox AC 11 kW: 90% (or the vehicle preset's AC efficiency)
     - Wallbox AC 22 kW: 90% (or the vehicle preset's AC efficiency)
     - DC 50 kW: 92% (or the vehicle preset's DC efficiency)
     - Custom: 90% (or the vehicle preset's custom AC/DC efficiency)

3. **Fahrzeugeinstellungen Dialog Preset Editing Overrides**:
   - In the settings dialog, modifying vehicle capacity, efficiency, or wear cost parameter fields directly does NOT reset the selected vehicle profile to "Custom".
   - The modifications are saved as specific overrides for the currently selected vehicle preset (e.g., `pref_<vehicle_id>_battery_nominal_kwh`, `pref_<vehicle_id>_ac_efficiency`, etc.).
   - The selected preset name remains active in the settings dropdown.

## Acceptance Criteria
- Tapping "Ladevorgang kalibrieren" displays the wizard dialog.
- The wizard validates input ranges and correctly calculates the calibrated efficiency.
- The recalculated charging duration on `LadedauerScreen` uses the newly calibrated efficiency for that charger type.
- Editing fields in the settings dialog for the Tesla Model S P85 or Leapmotor T03 preserves the selection of the profile and persists the customized overrides.

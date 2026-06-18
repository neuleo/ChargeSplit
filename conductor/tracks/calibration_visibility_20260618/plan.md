# Plan: Calibration Values Visibility and Management

## Phase 1: Backend and Preferences Updates

- [x] Task: Update PreferencesManager with Calibration Status & Reset Methods (ad651a4)
    - [x] Write failing unit tests in `PreferencesManagerTest.kt` for `isChargerCalibrated` and a new method to clear/reset charger efficiency.
    - [x] Implement `isChargerCalibrated`, `getChargerKey`, and `clearChargerEfficiency` in `PreferencesManager.kt`.
    - [x] Verify unit tests pass.
- [ ] Task: Conductor - User Manual Verification 'Phase 1: Backend and Preferences Updates' (Protocol in workflow.md)

## Phase 2: UI Implementation and Integration

- [ ] Task: Implement Calibration Display and Reset on LadedauerScreen
    - [ ] Add active efficiency display below the "Ladeart" selector showing whether it is calibrated or default.
    - [ ] Add a "Zurücksetzen" (Reset) option when the current charger is calibrated, which calls the reset method and triggers UI updates.
- [ ] Task: Implement Calibration List and Reset in SettingsDialog
    - [ ] Add a list of calibrated chargers for the selected vehicle preset in `SettingsDialog`.
    - [ ] Add a delete/reset button next to each calibrated charger entry to clear the calibration.
    - [ ] Verify all tests pass, run Android Lint and check code coverage.
- [ ] Task: Conductor - User Manual Verification 'Phase 2: UI Implementation and Integration' (Protocol in workflow.md)

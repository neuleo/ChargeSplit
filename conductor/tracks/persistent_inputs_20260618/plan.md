# Plan: Persistent Input Values for LadedauerScreen

## Phase 1: Extend PreferencesManager and Add Unit Tests

- [ ] Task: Update PreferencesManager with Ladedauer properties
    - [ ] Write failing unit tests in `PreferencesManagerTest.kt` verifying getters/setters for the new properties: `ladedauerStartSoc`, `ladedauerTargetSoc`, `ladedauerChargerType`, and `ladedauerCustomChargerKw`.
    - [ ] Implement new properties in `PreferencesManager.kt`.
    - [ ] Verify unit tests pass.
- [ ] Task: Conductor - User Manual Verification 'Phase 1: Extend PreferencesManager and Add Unit Tests' (Protocol in workflow.md)

## Phase 2: UI Integration in LadedauerScreen

- [ ] Task: Connect UI States to PreferencesManager
    - [ ] Update state initialization in `LadedauerScreen.kt` to load from `prefsManager`.
    - [ ] Add `LaunchedEffect` or direct setter invocation to save values to `PreferencesManager` when they change.
    - [ ] Verify all tests pass, run Android Lint and check code coverage.
- [ ] Task: Conductor - User Manual Verification 'Phase 2: UI Integration in LadedauerScreen' (Protocol in workflow.md)

# Implementation Plan - EV Settings Customization

## Phase 1: Calculation Logic Refactoring & Unit Tests (TDD) [checkpoint: 97b3031]

- [x] Task: Refactor cost calculations to a testable helper class [3aeb4ad]
    - [x] Write Tests: Create `CalculatorUtilsTest.kt` containing failing/placeholder unit tests validating charging and wear calculations.
    - [x] Implement Feature: Create `CalculatorUtils.kt` with pure calculation functions and refactor `MainActivity.kt` screens to use them.
- [x] Task: Integrate customizable parameters into calculations [94d1c97]
    - [x] Write Tests: Add unit tests in `CalculatorUtilsTest.kt` to verify that changing battery capacity, efficiency, price, and wear cost changes calculations as expected.
    - [x] Implement Feature: Update calculation calls in `MainActivity.kt` to pass parameters fetched from `SharedPreferences`.
- [x] Task: Conductor - User Manual Verification 'Phase 1: Core Calculations and Unit Testing' (Protocol in workflow.md) [97b3031]

## Phase 2: Settings UI and Preferences Bindings

- [x] Task: Implement the Settings Composable Screen [1cc136d]
    - [x] Write Tests: Create tests in `SettingsScreenTest.kt` or validator unit tests verifying settings text validation (e.g. rejection of negative values or invalid floats).
    - [x] Implement Feature: Create `SettingsScreen` Composable with state-backed text fields and automated `SharedPreferences` saving.
- [ ] Task: Integrate Settings tab into Main Pager
    - [ ] Write Tests: Create a JVM unit test or UI test checking that 3 tabs are present in the tabs list.
    - [ ] Implement Feature: Update `MainScreen` TabRow and HorizontalPager to include the "Einstellungen" tab.
- [ ] Task: Conductor - User Manual Verification 'Phase 2: UI Integration and Settings Persistence' (Protocol in workflow.md)

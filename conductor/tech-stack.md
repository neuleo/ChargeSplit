# Technology Stack

## Core Development
- **Language**: Kotlin (for safe, concise, and modern Android development)
- **UI Framework**: Jetpack Compose (using Material 3 guidelines and components)
- **Local Storage**: Android SharedPreferences (simple key-value persistence for caching user inputs)
- **Build System**: Gradle with Kotlin DSL (`.gradle.kts`)

## Architecture & Conventions
- **App Structure**: Single Activity (`MainActivity`) hosting composable screens (e.g., `ChargeSplitScreen`, `WearScreen`, and `SettingsScreen`) navigated using a tab-based `HorizontalPager`.
- **State Management**: Compose `runtime` state tracking (`remember`, `mutableStateOf`, `LaunchedEffect`).

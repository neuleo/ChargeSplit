# Product Guidelines

## Design & UI Guidelines
- **UI Framework**: Follow Material Design 3 guidelines for Jetpack Compose.
- **Visual Style**: Clean, minimalist, modern. Maintain clear margins (16.dp for screen padding, 12.dp for spaced elements).
- **Themes**: Support system-default Light and Dark modes. Colors should feel modern and eco-friendly (e.g., shades of green and teal for EV charging context).
- **Control Patterns**: Provide multiple input modalities for number ranges (e.g., sliders for rapid adjustment and +/- buttons for precise control).

## UX & Interaction Principles
- **Immediate Value**: Calculations must update instantly on button click. If invalid data is entered, show a clear inline helper or error message instead of failing silently or crashing.
- **Zero Configuration**: Keep the settings simple. Prefer smart defaults (e.g., 70 kWh battery capacity, 90% charging efficiency, €0.35/kWh) while saving state seamlessly.
- **State Persistence**: Persist the user's state automatically between app launches. Users should never lose their input when swapping apps or rotating the screen.

## Language & Copy Style
- **Language**: German (Deutsch) is the primary target language for the user interface.
- **Tone**: Professional, clear, and direct. Avoid technical jargon where possible, and use commonly understood terms (e.g., "Ladezustand" instead of "SOC" in user-facing labels).
- **Precision**: Show precise monetary outputs formatted in Euros with two decimal places (e.g., `€12,34`).

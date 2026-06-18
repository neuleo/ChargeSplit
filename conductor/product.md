# Initial Concept

An Android application for electric vehicle drivers to easily and fairly calculate and split charging and wear-and-tear costs with carpool passengers.

# Product Guide: ChargeSplit

## Overview
ChargeSplit is a mobile utility app that allows electric vehicle (EV) owners to split trip-related costs fairly among passengers. By providing clear inputs and predefined calculations, it ensures transparent expense sharing for both electricity consumption and vehicle wear-and-tear.

## Target Audience
- EV owners who drive friends, colleagues, or family in carpools.
- Commuters looking to share the cost of daily travel.

## Core Features
1. **Charging Cost Calculator (Ladekosten-Rechner)**:
   - Inputs for Battery State of Charge (SOC) at the start and end of the trip.
   - Interactive sliders and fine-tuning buttons (+/-) for ease of entry.
   - Battery capacity, cost per kWh, and charging efficiency variables customizable via the Settings Dialog.
   - Calculation of energy consumed, total cost, and equal division among all occupants.

2. **Wear & Tear Calculator (Verschleiß-Rechner)**:
   - Input for kilometers driven during the trip.
   - Base wear-and-tear rate customizable in settings (falling back to a default of €60 per 1600 km).
   - Equal division of the wear cost among the driver and passengers.

3. **Charging Time Calculator (Ladedauer-Rechner)**:
   - Calculates charging duration, energy drawn from grid, and cost from start to target SOC.
   - Supports Schuko, Wallbox AC 11 kW, Wallbox AC 22 kW, DC 50 kW, and custom charger inputs.
   - Warns user if the charger power exceeds vehicle's onboard AC or DC charging limits.
   - Includes a calibration wizard to calculate and save actual charging efficiencies per charger type for each vehicle profile.

4. **Settings Customization (Fahrzeugeinstellungen Dialog)**:
   - Accessible via a gear icon (⚙️) in the TopBar on all tabs.
   - Allows selecting vehicle preset profiles (Tesla Model S P85, Leapmotor T03, or Custom).
   - Auto-fills values for battery nominal capacity, AC efficiency, and DC efficiency, with support for battery degradation configuration.
   - Modifying default vehicle preset parameters saves custom overrides directly to the active preset without resetting the profile to "Custom".

5. **Persistent Inputs & Configuration**:
   - User inputs (SOC, mileage, passengers) and custom settings configuration are saved locally on the device (via SharedPreferences) and restored on startup.

6. **Tabbed User Interface**:
   - Seamless swipe and click-based navigation between charging cost calculations, wear-and-tear calculations, and charging time calculator.

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
   - Battery capacity, cost per kWh, and charging efficiency variables customizable in settings (falling back to defaults of 70 kWh, €0.35, and 90% respectively).
   - Calculation of energy consumed, total cost, and equal division among all occupants.

2. **Wear & Tear Calculator (Verschleiß-Rechner)**:
   - Input for kilometers driven during the trip.
   - Base wear-and-tear rate customizable in settings (falling back to a default of €60 per 1600 km).
   - Equal division of the wear cost among the driver and passengers.

3. **Settings Customization (Einstellungen)**:
   - Interactive forms to configure battery capacity, electricity cost, charging efficiency percentage, and wear-and-tear rate with real-time positive validation.

4. **Persistent Inputs & Configuration**:
   - User inputs (SOC, mileage, passengers) and custom settings configuration are saved locally on the device (via SharedPreferences) and restored on startup.

5. **Tabbed User Interface**:
   - Seamless swipe and click-based navigation between charging cost calculations, wear-and-tear calculations, and configuration settings.

# Proposal

## Why

On a ride with the app open, the maintainer's Pixel turned its display off after the usual timeout, while the iPhone stayed on, likely because of a phone setting. The maintainer expects the display to stay on while navigating with the app open, as in car navigation apps. Riders who want to save battery can still turn the display off with the power button, and navigation continues as before.

## What Changes

- While navigating and the app is in the foreground, the display stays on, on Android and iOS.
- Leaving the app (home, another app, the power button) or ending navigation (Stop, arriving) gives the display back to the system's normal timeout. With the display off, navigation continues with voice, notification and Live Activity as before.

Out of scope: a setting in the app, the display's brightness, keeping the display on outside navigation, and the battery requirement of `add-knot-navigation`, which is measured with the display off and stays unchanged.

## Capabilities

### New Capabilities

### Modified Capabilities
- `knot-navigation`: a new requirement that the display stays on while navigating with the app open. Its main spec is created when `add-knot-navigation` is archived, so that change must be archived first.

## Impact

- Android: `PlannerScreen.kt` or `MainActivity.kt`, keeping the window's display on while the state says navigating
- iOS: `KnoopByKnoopApp.swift` or `PlannerView.swift`, `UIApplication.isIdleTimerDisabled` while navigating
- No new dependencies, no core changes

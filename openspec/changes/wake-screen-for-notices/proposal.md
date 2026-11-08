# Proposal

## Why

With the screen off, the rider hears the spoken notices, but has to wake the phone to see what to do at the next knot, often with gloves or both hands on the handlebar. The maintainer asked on 2026-11-08 whether the screen could wake by itself close to a knot or when something needs the rider's attention.

An app cannot turn an iPhone's screen on directly. It can update its Live Activity with an alert, which the system presents to the person. The app updates its Live Activity silently today.

## What Changes

- At the moments that already have a spoken notice and need the rider's attention, the app alerts outside the app, so the locked phone shows the sign plate, the arrow back to the route, or the arrival:
  - the reminder before the next knot;
  - off route;
  - riding the wrong way;
  - a ferry on the route closing or closed;
  - arriving.
- Always on, without a setting, and silent: the spoken notice is the sound.
- **iPhone:** an alerting Live Activity update with a silent sound.
- **Android:** the navigation notification alerts once at these moments, waking the screen.
- Other updates stay silent, as today.

## Out of Scope

- **A setting** to turn it off. The maintainer decided on always on; a setting can follow if riders ask.
- **Sounds or vibration.**
- **Waking the screen when the app is open:** the screen is on then anyway.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `knot-navigation`: a new requirement "Waking the screen for notices".

## Impact

- **Core:** the update tells the apps whether to alert, from its notice kinds.
- **iOS:** `Navigator` passes an `AlertConfiguration` on those updates; a short silent sound file in the bundle.
- **Android:** `NavigationService` alerts with the navigation notification on those updates.

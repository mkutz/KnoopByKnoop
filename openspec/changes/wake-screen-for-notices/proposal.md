# Proposal

## Why

With the screen off, the rider hears the spoken notices, but has to wake the phone to see what to do at the next knot, often with gloves or both hands on the handlebar. The maintainer asked on 2026-11-08 whether the screen could wake by itself close to a knot or when something needs the rider's attention.

An app cannot turn an iPhone's screen on directly. It can update its Live Activity with an alert, which the system presents to the person. The app updates its Live Activity silently today.

## What Changes

- With every spoken notice, the app also shows it on the screen: a locked phone lights up with the notice and what the panel shows. That is when the rider looks down to check what they heard (the maintainer, 2026-11-08).
- Always on, without a setting, and silent: the spoken notice is the sound.
- **iPhone:** a silent notification with the notice's text; the lock screen shows it above the Live Activity.
- **Android:** the navigation notification alerts once at each notice, waking the screen.
- Updates without a spoken notice stay silent, as today.

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

- **Core:** nothing; every update with notices alerts.
- **iOS:** `Navigator` posts a silent notification for each spoken notice while the app is not on screen; the permission to show notifications is asked when navigation starts for the first time.
- **Android:** `NavigationService` alerts with the navigation notification on those updates.

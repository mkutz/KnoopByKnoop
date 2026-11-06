# Proposal

## Why

An Android tester found switching the appearance in the info sheet slow: the sheet froze half-way, with old and new labels over each other. Measured on the maintainer's Pixel 7 on 2026-11-06 while switching Light and Dark four times: 59 of 868 frames slow, all on the main thread, the slowest 1% at 200 ms; the GPU was never slow. On the emulator, the map's style reload and the app's layers on top of it took about 190 ms on the main thread, right while the sheet animates its new colours.

## What Changes

- **Android:** while the info sheet is open, choosing an appearance switches the sheet and the controls at once; the map follows when the sheet closes. With the sheet closed, e.g. when the system switches at sunset, the map switches at once as before.

## Out of Scope

- **iOS,** where no slow switch was reported.
- **Making the map's style reload cheaper,** e.g. keeping badges and layers across styles. Only if the deferred reload isn't enough.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `map-planning`: "Light and dark appearance" lets the map follow when the info sheet closes.

## Impact

- **Android:** `PlannerScreen` passes whether the info sheet is open to `NetworkMap`, which holds back its style reload until then.

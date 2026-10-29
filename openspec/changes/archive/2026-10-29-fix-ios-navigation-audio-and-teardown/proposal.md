# Proposal

## Why

The maintainer found two bugs on the iPhone after a ride:
- **Music stays quiet:** while navigating, music stayed quieter the whole time the app ran, and only got loud again when the app was ended. The spec says it is lowered only while a notice is spoken ("Music playing").
- **Location indicator after closing:** when the app was closed while navigating, the blue location indicator stayed. The fix from `add-knot-navigation` only removes it at the next start of the app, but a rider who swipes the app away expects navigation to end right then.

## What Changes

- **iOS: music back up after each notice.** Other audio is lowered only while a notice is spoken: the audio session is deactivated as soon as nothing is left to speak, and failures to deactivate are logged instead of ignored.
- **iOS: closing the app ends navigation.** Location updates stop, the background location session ends, and the Live Activity disappears. The cleanup at the next start stays as a fallback for when iOS ends the app without telling it.

Out of scope: Android. There, navigation runs in a foreground service that keeps navigating when the app is swiped away, with Stop in its notification. The Android "Music playing" check is still open from `add-knot-navigation` and isn't part of this change.

## Capabilities

### New Capabilities

### Modified Capabilities
- `knot-navigation`: "Arriving and stopping" states that the location is used only while navigating, so the system's location indicator shows only then, and gets the scenario "Closing the app while navigating" for iOS. "Music playing" stays as it is: the code is brought in line with it.

## Impact

- iOS only: `Navigator.swift` (speech and audio session, a teardown on termination) and `KnoopByKnoopApp.swift` (`applicationWillTerminate` in the `AppDelegate`)

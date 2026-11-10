# Manual checks

## How it was checked

Checked on 2026-11-10:

- **Core:** `PathRouterTest` "Broken path data finds no way and the next search works again", with a square source that throws as an unpackable square does. It passes on the JVM and on the iOS simulator (`iosSimulatorArm64Test`), where an exception reaching Swift would end the app.
- **Android:** the debug build builds without the catch of `Exception` in `NavigationService.findWay`.
- **iOS:** the app builds (`xcodebuild` as in the Build workflow); no code change.
- **On a phone:** not checked; a square that passes its checksum but can't be unpacked can't be made on a phone without tools.

## Scenarios

| Scenario | Android | iOS | Notes |
|---|---|---|---|
| Broken path data | unit test | unit test (iOS simulator) | `PathRouterTest`; the guard is in the core's router, which both apps' searches go through, while navigating (`findWay`) and while planning (`RoutePointWays`) |

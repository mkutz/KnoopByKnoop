# Manual checks

## How it was checked

Checked on 2026-11-10:

- **Core:** `PathRouterTest` "Broken path data finds no way and the next search works again", with a square source that throws as an unpackable square does. It passes on the JVM and on the iOS simulator (`iosSimulatorArm64Test`), where an exception reaching Swift would end the app.
- **Android:** the debug build builds without the catch of `Exception` in `NavigationService.findWay`.
- **iOS:** the app builds (`xcodebuild` as in the Build workflow); no code change.
- **Android emulator** (`Medium_Phone_API_37.0`, debug builds), 2026-11-10: route 06 → 08 near Titz with a stop away from the network, its 12 squares of path data replaced by bytes that can't be unpacked, with matching checksums in `squares.json`, and the emulator offline (online, the app downloads the squares again and repairs them):
  - **Before the fix** (`c777357`): the app crashed on every start with `GraphFormatException: Can't unpack: incorrect header check`, from `PathRouter.way` → `PathStore.segments` → `inflate`, in the planner's search for the stop's way. The app could not be opened at all.
  - **With the fix** (`74a7c18`): the app opened, the stop kept its straight line with "The way to a chosen place is only a straight line so far" ([screenshot](screenshots/android-planning.png)). Navigating 800 m off the route inside the broken squares: `guided way: null m … 0 squares` in the log, the dashed straight line back to 06, no crash ([screenshot](screenshots/android-navigating.png)).
  - Afterwards the emulator got its empty route back, the broken squares were removed and airplane mode switched off.
- **iPhone:** not checked; a square that passes its checksum but can't be unpacked can't be made on a phone without tools.

## Scenarios

| Scenario | Android | iOS | Notes |
|---|---|---|---|
| Broken path data | ✅ emulator, planning and navigating | unit test (iOS simulator) | `PathRouterTest`; the guard is in the core's router, which both apps' searches go through, while navigating (`findWay`) and while planning (`RoutePointWays`) |

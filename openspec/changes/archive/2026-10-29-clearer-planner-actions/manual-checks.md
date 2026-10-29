# Manual checks

## Contrast ("Readable actions")

Computed with the WCAG 2.x formula from the colors in the code. Labels need at least 4.5:1, icons at least 3:1.

| What | Colors | Ratio |
|---|---|---|
| Before: green text links on the panel | `#2E9E5B` on `#F3EDF7` | 2.97 ❌ |
| Navigate: label and icon | white on `#237A46` | 5.32 ✅ |
| Stop: label and icon | white on `#B3261E` | 6.54 ✅ |
| Android: Back to start and Clear icons | `#237A46` on `#D7EEDF` | 4.36 ✅ |
| iOS: Back to start and Clear icons, `.bordered` (tint at about 15 %) on the glass panel | `#237A46` over white / `#EEF1F4` / `#D9DEE3` | 4.34 / 3.87 / 3.29 ✅ |
| Navigate and Stop fills against the panel | `#237A46` / `#B3261E` on `#F3EDF7` | 4.63 / 5.69 ✅ |

## Android

Emulator `Medium_Phone_API_37.0`, 2026-10-28, on the route 08 → 31 → 97 at the Kaag (5.2 km).

| Scenario | Android | Notes |
|---|---|---|
| Navigate stands out | ✅ | Distance, Back to start, Clear and Navigate in one row ([screenshot](screenshots/android-planning.png)); also with "123.4 km" on a 360 dp wide screen (density 480, a debug build showing that distance, [screenshot](screenshots/android-header-360dp-123km.png)) and in landscape ([screenshot](screenshots/android-landscape.png)) |
| Stop stands out | ✅ | "At 08 follow 31" with a red Stop ([screenshot](screenshots/android-stop.png)); Stop ends navigation, the service is gone and the route stays |
| Name of an icon button on Android | ✅ | Long press on Clear shows "Clear" ([screenshot](screenshots/android-clear-long-press.png)) and does not clear the route. Needs a real touch: `adb shell input` touches don't open tooltips, the emulator console's `event send` does |
| Icon buttons with a screen reader | ✅ | Checked by the maintainer with TalkBack on their Android phone (0.1.1 or later); the emulator's TalkBack could not be driven past the map |

## iOS

Simulator (iPhone 18 Pro), 2026-10-28: the planning header looks like on Android ([screenshot](screenshots/ios-simulator-planning.png)). The simulator cannot be tapped by script; iPhone checks by the maintainer, 2026-10-29.

| Scenario | Simulator | iPhone | Notes |
|---|---|---|---|
| Navigate stands out | ✅ | ✅ | |
| Stop stands out | – | ✅ | |
| Icon buttons with a screen reader | – | ✅ | VoiceOver |

## Found and fixed

- **iOS: dark sheet in dark mode.** The views over the map are kept light, because the map is light; the sheet and the landscape panel were not and followed the system's dark mode. On the dark sheet, the round Back to start and Clear buttons were hardly visible (found by the maintainer on the iPhone). The panel is now light in dark mode too ([screenshot](screenshots/ios-simulator-dark-mode.png), simulator in dark mode). Following the system's appearance in the whole app is its own change, `follow-system-appearance`.

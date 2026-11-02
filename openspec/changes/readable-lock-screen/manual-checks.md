# Manual checks

## Scenarios

Android checked 2026-11-02 on the emulator (`Medium_Phone_API_37.0`, Android 17) with the debug build and the published data, route 02 → 06 → … → 85 from Siegburg with every knot as a stop, positions mocked with `adb emu geo fix`. Route, camera, language, lock setting and Live Update permission were restored.

iOS: the project builds and the app runs in the simulator (iPhone 18 Pro, iOS 27), but the simulator here runs headless, so the lock screen cannot be opened and `simctl` screenshots do not show the Dynamic Island. The Live Activity's real views were rendered in the app by temporary code (removed again) to check their look. A real Live Activity goes to the steps below.

| Scenario | Android | iOS | Notes |
|---|---|---|---|
| Live Update | ✅ | — | Flagged `PROMOTED_ONGOING`: the words, the distance, the bar with marks, the large badge and Stop, on the lock screen and in the shade; chip "06" on the home screen ([screenshot](screenshots/android-live-update-far-chip-lock.png)). The bar draws at most 4 marks, with 12 offered |
| Far from the next knot outside the app | ✅ | ✅ views | "Next 06, then 05" ([iOS views](screenshots/ios-live-activity-views.png)). In German "Nächster Knotenpunkt 06, dann 05" fits ([screenshot](screenshots/android-live-update-german.png)). `NavigationTest` |
| Lock screen | ✅ | ✅ views | Close: "At 06 follow 05", "168 m to 06 · end around 15:30", with 05 as the badge ([screenshot](screenshots/android-live-update-close-off-route-turned-off.png), left) |
| Off route outside the app | ✅ | ✅ views | "Off route, next 06", "397 m south to 06", the bar's dot red (middle) |
| Live Update turned off | ✅ | — | With the app op denied: the same card with the bar, no chip (right) |
| Stop from the lock screen | ✅ | ⏳ | Stop in the notification ended navigation and the notification |
| Distance on the way | ✅ | unit test | With the app in the background, "6.9 km to 06" became "168 m to 06" with no knot passed, at the next fix, about every 30 s. `RouteProgressTest` "distance on the way" |
| Progress outside the app | unit test | unit test | `RouteProgressTest` |
| Progress bar in the Live Activity | — | ✅ iPhone | In a real Live Activity on the maintainer's iPhone, off route, light and dark; the far and close forms and the green part ridden as views in the app |
| Dynamic Island | — | ⏳ | Not visible in the simulator's screenshots |
| Older Android | by code | — | No emulator before API 36 here: the bar is behind a version check for API 36, promotion and chip behind one for 36.1 (lint enforces both), and the large badge is outside them |

Light and dark, checked 2026-11-02:
- **Android:** far, close and off route in the shade, off route on the lock screen, and the chip, all in dark ([card](screenshots/android-live-update-dark.png), [chip](screenshots/android-live-update-chip-dark.png)); light as above. The card follows the system. The stops' marks ahead are dimmer in dark, as the system dims what is ahead, but still visible.
- **iOS views:** far, close, off route and the end in dark ([screenshot](screenshots/ios-live-activity-views-dark.png)), and far, close, off route and the end in light.

Found on the iPhone on 2026-11-02 in a real Live Activity: the line and the bar's track were unreadable, dark-mode colors on a light card. Opaque colors alone did not fix it. With its own background the card was readable, but dark in light mode too (confirmed by the maintainer), so the app now reports the system's mode, see the design.

Stop in the Live Activity is now red with white text, as in the app, after the maintainer found green on dark green weak on the iPhone ([views](screenshots/ios-live-activity-stop.png)). Not yet seen in a real Live Activity.

### Steps for the maintainer

1. **iPhone, lock screen:** start navigating and lock the phone. Far from a knot, "Next 29", "then 25", the distance and the bar; close to it, "At 29 follow 25". Stop works.
2. **iPhone, Dynamic Island:** in another app, the compact form shows the next knot and the number to follow; long-press shows the bar.
3. ✅ **iPhone, light and dark:** the card is light in light mode and dark in dark mode, all text readable. Switch the mode while navigating: it follows at the next position. Checked by the maintainer in a real Live Activity, off route, 2026-11-02: both within the same minute.
4. **Both, on a ride:** the distance moves on with the screen off, and the bar moves with it.
5. **Pixel:** the chip in the status bar shows the next knot, and close to it the number to follow.

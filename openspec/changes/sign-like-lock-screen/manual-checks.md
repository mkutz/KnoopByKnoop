# Manual checks

## How it was checked

Checked on 2026-11-07 with the locally built cycling data with sign regions, on the route Troisdorf 25 → 32 → 29:

- **Android:** the emulator (`Medium_Phone_API_37.0`, Android 16 QPR2 with Live Updates) with the debug build. The notification shade and the status bar were captured with `adb`.
- **iOS:** the simulator runs headless here, so it can't be locked, and its screenshots don't show the Dynamic Island. The Live Activity's real views (the lock screen card, the compact and the minimal Dynamic Island) were rendered in the app for sample states by temporary code, removed again before committing ([views](screenshots/ios-live-activity-views.png)).
- **Afterwards:** both devices got their own data back.

## Scenarios

| Scenario | Android | iOS | Notes |
|---|---|---|---|
| Far from the next knot in German | ✅ | unit test | `NavigationTest`: the same title in German. Emulator, app in German at font size 1.3: "02 · 1,6 km › 28" in one line ([shade](screenshots/android-german-larger-font.png)) |
| Lock screen | ✅ | ✅ views | Android "32 → 25", "172 m · ■ 16:55" with the 25 plate as large icon ([close](screenshots/android-close.png)); iOS card A |
| Far from the next knot outside the app | ✅ | ✅ views | "32 · 4.4 km › 25", "■ 17:05", 32 as large icon ([far](screenshots/android-far.png)); iOS card B |
| Dynamic Island | — | ✅ views | Compact: 32 → plate 25 and "150 m" |
| Dynamic Island with room for one number | — | ✅ iPhone | Minimal: the 25 plate close, 32 far, the light tile off route |
| Off route outside the app | ✅ | ✅ views | Android: guided title "↗ 80 m · 600 m", the symbol on a dark disc ([off route](screenshots/android-off-route.png)); iOS card C inverted, light on the dark system |
| Off route in the Dynamic Island | — | ✅ views | The arrow on a light tile, › 02 and "1.7 km" |
| On the way to the route outside the app | ✅ | ✅ views | "↰ 340 m · 2.0 km" with the symbol on a light disc ([on the way](screenshots/android-on-the-way.png)); iOS card D in its usual appearance |
| Stop from the lock screen | ✅ | open | Emulator: "Beenden" in the expanded notification over the lock screen ends navigation, the notification and the service are gone. iOS on the iPhone; the iOS Stop is an icon now |
| Progress outside the app | ✅ | ✅ views | |
| Distance on the way | ✅ | — | The title follows the rounded distance |
| Live Update | ✅ | — | Chip "32" far from 32; stops as points in their plate colour |
| Chip off route | ✅ | — | The to-the-route symbol and "↩500 m" on the guided way ([chip](screenshots/android-chip-off-route.png)); `NavigationTest` for "←300 m" |
| Progress bar in the Live Activity | — | ✅ views | The end arrival beside the bar, the ✕ dot off route, a ferry at risk dark |
| Live Activity in light and dark mode | — | ✅ views | Light and dark cards, inverted off route and riding the wrong way |
| Live Update turned off | ✅ | — | Live Updates off in the app's notification settings: an ordinary notification with the same title, large icon, progress bar and "Beenden", no chip ([shade](screenshots/android-live-updates-off.png)) |
| Older Android | ✅ | — | API 31 emulator: an ordinary notification with the title "02 · 1.6 km › 28", the plate as large icon and Stop, no progress bar ([shade](screenshots/android-older-android.png)). Found: its texts in the phone's language, not the app's (below) |
| Lock screen off route | ✅ | ✅ views | No compass word |
| Updated on the way | unit test | unit test | `NavigationTest` "updated when the arrow turns" |
| Back on route outside the app | ✅ | open | Emulator, the app in the background, the recorded ride `siegburg-going-on` off route and back: "Zurück auf der Route, nächster Knotenpunkt 28" spoken, the notification back to "28 · 669 m" ([shade](screenshots/android-back-on-route.png)) |
| On the lock screen (guided way) | ✅ | — | `NavigationTest` "guided way on the lock screen" for "↰ 120 m · 1.9 km" |
| Wrong direction outside the app | unit test | ✅ views | `NavigationTest`: "↩ 45 · 700 m", chip "↩45"; iOS card inverted with the U-turn |

TalkBack on the notification: not checked yet. The title is made of arrow characters, so how TalkBack reads it is open.

Checked on 2026-11-11 on the emulator (Android 16 QPR2, and `KbK_API_31`) with the debug build and the published data of 2026-11-08, riding the fixes of the recorded real ride `siegburg-going-on` (01 → 02 → 28, off route south-west of 02 and back) with `adb emu geo fix`.

## Found, not fixed here

- **Android 12 and older: the navigation's notification and spoken notices in the phone's language, not the app's.** With the app set to German on an English phone (API 31), the app showed German, but the notification said "Stop" and "1.6 km" and the voice "Next 2, then 28" in English. Before Android 13 the chosen language reaches only the activity; `AppViewModel.text()` works around it, `NavigationService` doesn't. Not caused by this change; a change of its own.

## Found and fixed

- **The compact Dynamic Island tried to show too much.** On the maintainer's ride of 8 November, with a podcast sharing the island, the compact view showed two cut badges and "2....": the miniature row (next knot, arrow, number to follow) and the distance don't fit its room. The compact island now shows one element on the leading side, the same as the minimal one (the plate close to a knot, the next knot far from it, the tile off route), and the distance scales down instead of being cut off. The project builds; the simulator did not show the Live Activity, so the check is on the iPhone (step 2 below).

The maintainer's walk home → gym in Siegburg on 2026-11-09 (iPhone, dark mode, a route of a start and an end flag, path data installed): with a radio podcast sharing the Dynamic Island, the app's part showed the end flag and "1.1 km" alone, nothing cut off (step 2). On the ride of 2026-11-08 it had shown two cut badges.

## For the maintainer

1. **With the screen off on a ride,** look at the lock screen far from a knot, close to one (the sign plate as the large icon on the Pixel, on the card on the iPhone), off route, and riding the wrong way.
2. **On the iPhone, use another app while navigating, and listen to a podcast:** the Dynamic Island shows one knot or plate and the distance, nothing cut off; also with a screen recording, where it shows the element alone.
3. **On the Pixel, the status bar chip:** "→12" close to a knot, the next knot far from it.
4. **TalkBack once on the notification:** note how the title is read.
5. **Stop from the lock screen** on both phones.

# Manual checks

## Scenarios

Android checked 2026-11-02 on the emulator (`Medium_Phone_API_37.0`) with the debug build and the published data, on the route 02 → 28 → 27 → 28 → 02, positions mocked with `adb emu geo fix`, the language set with `cmd locale set-app-locales` while navigating. Route, camera and language were restored.

| Scenario | Android | iOS | Notes |
|---|---|---|---|
| Words and line outside the app | ✅ | ⏳ | "Next 28, then 27", "1.9 km to 28 · end around 17:10" became "Nächster Knotenpunkt 28, dann 27", "1,9 km bis 28 · Ende gegen 17:10" with no new position ([screenshots](screenshots/android-language-change.png), first two). `NavigationTest` |
| Off route | ✅ | ⏳ | German "Abseits der Route, nächster … West", then Dutch "Van de route af, volgende … west" (third and fourth) |
| Later notices | ✅ | ⏳ | Logged "spoken in DE" and then "spoken in NL" after each change, with the texts in that language |
| No voice for the new language | unit test | — | `NavigationTest`. The emulator has the voices |
| Ferry ahead | unit test | — | `FerryWarningTest`: in German, and not warned twice |
| Nothing else changes | ✅ | ⏳ | No notice logged at the change; the strip's place and distances stayed. `NavigationTest` |

### Steps for the maintainer

1. **Pixel:** while navigating, change the app's language in the system settings: the lock screen and the panel change at once, and the next notice is in the new language.
2. **iPhone:** while navigating, change the app's language in the iPhone's settings, then return to the app. Expect the app to restart with navigation's texts in the new language; report what really happens, as this has not been tried.

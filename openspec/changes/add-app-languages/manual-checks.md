# Manual checks

## Scenarios

Android checked 2026-10-31 on the emulator (`Medium_Phone_API_37.0`), system language English, the app's language set per app with `cmd locale set-app-locales`, on the round trip 93 → 29 (stop) → 94 in Arnhem with mocked positions. iOS installed on the maintainer's iPhone, not yet checked there.

| Scenario | Android | iOS | Notes |
|---|---|---|---|
| German phone | ✅ | ⏳ | "Bei 94 folge 41", "467 m", "Beenden"; planning "4,1 km", "etwa 15 min", "Navigieren" ([planning](screenshots/android-planning-de.png), [navigating](screenshots/android-navigating-de.png)) |
| Dutch phone | ✅ | ⏳ | "4,1 km", "ongeveer 15 min", "Navigeren", "Fietsen / Wandelen" ([planning](screenshots/android-planning-nl.png), [navigating](screenshots/android-navigating-nl.png)) |
| French phone | ✅ (core test for off route) | ⏳ | "Au 94 suivre 41", "Arrêter", "Naviguer" ([navigating](screenshots/android-navigating-fr.png)); "Hors itinéraire, suivant 12" in `RouteProgressTest` "french phone" (no courses on the emulator to go off route) |
| Other language | ✅ | ⏳ | Spanish: all texts English ([screenshot](screenshots/android-planning-es.png)) |
| Per-app language | ✅ | ⏳ | All of the above with English as the system language |
| Place names stay | core | ⏳ | Place names come from Photon unchanged; not searched on the emulator |
| Decimal comma | ✅ | ⏳ | "2,2 km", "4,0 km" on the strip in German, Dutch and French |
| Date of a ferry's next opening | core test | ⏳ | `FerryStatusTest` "date of a ferry's next opening" |
| English unchanged | ✅ | ⏳ | ([screenshot](screenshots/android-planning-en.png)), and all earlier core tests unchanged |
| German notice | ✅ | ⏳ | Logged: "Nächster Knotenpunkt 94, dann 41, spoken in DE" |
| Dutch arrival | core test | ⏳ | `RouteProgressTest` "dutch arrival"; Dutch notices logged "spoken in NL" |
| No voice for the language | core test | ⏳ | `RouteProgressTest` "no voice for the language"; the emulator has voices for all three languages, so the fallback could not be seen there |
| Notification in German | ✅ | ⏳ | "Bei 94 folge 41" over "573 m bis 94 · Ende gegen 19:45"; Dutch "573 m naar 94 · einde rond 19:50", French "573 m jusqu'à 94 · arrivée vers 19:50" |
| Screen reader in Dutch | ✅ | ⏳ | `uiautomator dump`: "Knooppunt 29, tussenstop, 2,2 km, 19:45" |
| Location prompt in French | n/a | ⏳ | Android's location prompt has no app reason; it is in the system's language |
| Large text, German | ✅ | ⏳ | Font scale 2.0: the header wraps "Bei 94 folge" with 41 and the distance below, the labels complete ([planning](screenshots/android-planning-de-2.0.png), [navigating](screenshots/android-navigating-de-2.0.png)) |

### Native speaker review

The Dutch and French texts are in `apps/android/src/main/res/values-nl` and `values-fr`, `apps/ios/KnoopByKnoop/Localizable.xcstrings` and `modules/core/.../Texts.kt` (`DutchTexts`, `FrenchTexts`); the shared terms in `glossary.md`. Not yet reviewed.

iOS built and checked in the simulator (iPhone 18 Pro, iOS 27) with `-AppleLanguages (de)` on 2026-10-31: the planner shows "Radfahren / Wandern", "5,2 km", "etwa 19 min", "Navigieren", "Orte suchen" and the ferry status "Geöffnet"; the layout holds. Not yet checked on the iPhone.

### iOS steps for the maintainer

Set the language per app in Settings → Apps → Knoop by Knoop → Language, or the iPhone's language in Settings → General → Language & Region.

1. German phone: navigate a route; the header shows "Bei 45 folge 12" with the distance, e.g. "800 m", and Stop reads "Beenden".
2. Dutch phone: plan a cycling route; the panel shows the distance with a decimal comma, e.g. "3,5 km", "ongeveer 13 min" and "Navigeren".
3. French phone: navigate and leave the route; the header shows "Hors itinéraire, suivant 02".
4. Other language: with the app set to Spanish (or any language other than English, German, Dutch, French), all texts are English.
5. Per-app language: with the iPhone in English and the app set to German, the app is German.
6. Place names stay: in German, search "Arnhem Centraal"; the hit is shown as "Arnhem Centraal".
7. Decimal comma: in German, the strip shows e.g. "5,2 km" below a stop.
8. Date of a ferry's next opening: in German, a ferry closed for the season shows e.g. "Geschlossen bis 1. April 07:00" in the ferry list.
9. English unchanged: in English, everything looks as before ("5.2 km", "about 19 min", "Closed until 1 April 07:00").
10. German notice: navigating in German, passing a knot is spoken with a German voice, e.g. "Nächster Knotenpunkt 45, dann 12", the numbers in German.
11. Dutch arrival: in Dutch, arriving is spoken "Je bent aangekomen" with a Dutch voice.
12. No voice for the language: with a language whose voice is not installed, the notices are spoken in English (iOS ships voices for German, Dutch and French, so this may only be seen by removing one under Settings → Accessibility → Spoken Content → Voices).
13. Notification in German: with the screen off, the Live Activity shows "Bei 45 folge 12" and e.g. "0,8 km bis 45 · Ende gegen 19:05"; its button reads "Beenden".
14. Screen reader in Dutch: with VoiceOver, a row of the coming knots reads e.g. "Knooppunt 30, tussenstop, 5,2 km, 18:40".
15. Location prompt in French: after deleting and reinstalling the app with the app or phone in French, the location prompt shows its reason in French.

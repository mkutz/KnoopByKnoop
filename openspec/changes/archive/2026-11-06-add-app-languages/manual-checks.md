# Manual checks

## Scenarios

Android checked 2026-10-31 on the emulator (`Medium_Phone_API_37.0`), system language English, the app's language set per app with `cmd locale set-app-locales`, on the round trip 93 → 29 (stop) → 94 in Arnhem with mocked positions. iOS installed on the maintainer's iPhone, not yet checked there; checked in the simulator on 2026-11-05, see below.

| Scenario | Android | iOS | Notes |
|---|---|---|---|
| German phone | ✅ | ✅ sim | "Bei 94 folge 41", "467 m", "Beenden"; planning "4,1 km", "etwa 15 min", "Navigieren" ([planning](screenshots/android-planning-de.png), [navigating](screenshots/android-navigating-de.png)) |
| Dutch phone | ✅ | ✅ sim | "4,1 km", "ongeveer 15 min", "Navigeren", "Fietsen / Wandelen" ([planning](screenshots/android-planning-nl.png), [navigating](screenshots/android-navigating-nl.png)) |
| French phone | ✅ (core test for off route) | ✅ sim | "Au 94 suivre 41", "Arrêter", "Naviguer" ([navigating](screenshots/android-navigating-fr.png)); "Hors itinéraire, suivant 12" in `RouteProgressTest` "french phone" (no courses on the emulator to go off route) |
| Other language | ✅ | ✅ sim | Spanish: all texts English ([screenshot](screenshots/android-planning-es.png)) |
| Per-app language | ✅ | ✅ sim | All of the above with English as the system language |
| Place names stay | core | ✅ sim | Place names come from Photon unchanged; not searched on the emulator |
| Decimal comma | ✅ | ✅ sim | "2,2 km", "4,0 km" on the strip in German, Dutch and French |
| Date of a ferry's next opening | core test | ✅ sim | `FerryStatusTest` "date of a ferry's next opening" |
| English unchanged | ✅ | ✅ sim | ([screenshot](screenshots/android-planning-en.png)), and all earlier core tests unchanged |
| German notice | ✅ | text ✅ sim, sound ⏳ | Logged: "Nächster Knotenpunkt 94, dann 41, spoken in DE" |
| Dutch arrival | core test | text ✅ sim, sound ⏳ | `RouteProgressTest` "dutch arrival"; Dutch notices logged "spoken in NL" |
| No voice for the language | core test | ⏳ | `RouteProgressTest` "no voice for the language"; the emulator has voices for all three languages, so the fallback could not be seen there |
| Notification in German | ✅ | ✅ sim | "Bei 94 folge 41" over "573 m bis 94 · Ende gegen 19:45"; Dutch "573 m naar 94 · einde rond 19:50", French "573 m jusqu'à 94 · arrivée vers 19:50" |
| Screen reader in Dutch | ✅ | labels ✅ sim, VoiceOver ⏳ | `uiautomator dump`: "Knooppunt 29, tussenstop, 2,2 km, 19:45" |
| Location prompt in French | n/a | ✅ sim | Android's location prompt has no app reason; it is in the system's language |
| Large text, German | ✅ 2026-11-05 | ✅ sim 2026-11-05, Live Activity after its fix | See "Android at large text" below  |

### Native speaker review

The Dutch and French texts are in `apps/android/src/main/res/values-nl` and `values-fr`, `apps/ios/KnoopByKnoop/Localizable.xcstrings` and `modules/core/.../Texts.kt` (`DutchTexts`, `FrenchTexts`); the shared terms in `glossary.md`. Not yet reviewed: the testers check the languages on Android with 0.6.0 from Google Play's internal testing (2026-10-31). Archived on 2026-11-06 with this review still open: the maintainer asks Dutch and French friends to check the texts; their corrections become a change of their own.

iOS built and checked in the simulator (iPhone 18 Pro, iOS 27) with `-AppleLanguages (de)` on 2026-10-31: the planner shows "Radfahren / Wandern", "5,2 km", "etwa 19 min", "Navigieren", "Orte suchen" and the ferry status "Geöffnet"; the layout holds. Not yet checked on the iPhone.

### iOS in the simulator, 2026-11-05

Checked in the simulator (iPhone 18 Pro, iOS 27), Debug build of 3b7d73d, with the simulator's whole language and region set and rebooted for each of German, Dutch, French and Spanish, driven by a throwaway XCUITest runner outside the repository, positions mocked with `simctl location`, on the round trip 93 → 29 → 94 in Arnhem, 93 → 94 for the arrival and the ferry 16 → 17 at Loozen. ✅ sim means seen there, not on the iPhone.

| Scenario | Seen |
|---|---|
| German phone | "Bei 94 folge 41", "155 m", "Beenden" ([navigating](screenshots/ios-navigating-de.png)); "4,1 km", "etwa 15 min", "Navigieren" ([planning](screenshots/ios-planning-de.png)) |
| Dutch phone | "4,1 km", "ongeveer 15 min", "Navigeren", "Fietsen / Wandelen" ([planning](screenshots/ios-planning-nl.png), [navigating](screenshots/ios-navigating-nl.png)) |
| French phone | "Hors itinéraire, suivant 94", "901 m", "sud-est", "Arrêter" ([off route](screenshots/ios-offroute-fr.png), [planning](screenshots/ios-planning-fr.png)) |
| Other language | Spanish: "4.1 km", "about 15 min", "Navigate", "Next … then", "Stop" ([screenshot](screenshots/ios-planning-es.png)) |
| Per-app language | Simulator in English, Settings → Apps → Knoop by Knoop → Language → Deutsch: "Radfahren", "3,5 km", "etwa 13 min" ([screenshot](screenshots/ios-per-app-de.png)) |
| Place names stay | In German, the hit reads "Arnhem Centraal", its second line "Station, Arnheim" as Photon sends it ([screenshot](screenshots/ios-search-de.png)) |
| Decimal comma | "2,3 km", "4,1 km" below the stops on the strip in German and Dutch |
| Date of a ferry's next opening | "Geschlossen bis 1. April 07:00" ([screenshot](screenshots/ios-ferry-de.png)) |
| English unchanged | "3.5 km", "about 13 min", "Closed until 1 April 07:00" ([screenshot](screenshots/ios-ferry-en.png)) |
| German notice | Logged: "Nächster Knotenpunkt 94, dann 41", "94 voraus, dann 41". The simulator has the voice Anna for `de-DE`; nothing was listened to |
| Dutch arrival | Logged: "94 komt eraan, het einde", "Je bent aangekomen". The simulator has the voice Xander for `nl-NL`; nothing was listened to |
| No voice for the language | Not checked: the simulator has voices for all three languages |
| Notification in German | Locked: "Bei 41 folge 29", "73 m bis 41 · Ende gegen 16:10", "Beenden" ([screenshot](screenshots/ios-liveactivity-de.png)) |
| Screen reader in Dutch | Accessibility labels read: "Knooppunt 29, tussenstop, 2,3 km, 16:05", "Knooppunt 94, einde, 4,1 km, 16:15" ([rows](screenshots/ios-rows-nl.png)). VoiceOver itself was not running |
| Location prompt in French | "Knoop by Knoop utilise votre position pour vous montrer sur la carte où vous êtes et, pendant la navigation, vous indiquer le numéro à suivre au prochain p…" ([screenshot](screenshots/ios-location-prompt-fr.png)), after resetting the location permission rather than reinstalling |
| Large text, German | After the fix below: at AX2, about Android's scale 2.0, "Bei 94 folge 41", "58 m" and "Beenden" complete ([planning](screenshots/ios-planning-de-ax2.png), [navigating](screenshots/ios-navigating-de-ax2.png)). At AX5, the largest, also complete, with the search field and the credit in full ([planning](screenshots/ios-planning-de-ax5.png), [navigating](screenshots/ios-navigating-de-ax5.png), [off route](screenshots/ios-offroute-de-ax5.png)); Dutch and French hold there too ([Dutch planning](screenshots/ios-planning-nl-ax5.png), [French navigating](screenshots/ios-navigating-fr-ax5.png)). The default size looks as before. Language set with `-AppleLanguages` for this row |

## Found and fixed

Large text on iOS, German most of all:

- Close to a knot, Stop stayed next to the header at accessibility sizes, which cut them to "… 94 fol… 41" and "Beend…". Stop now goes above the header in every state, and the number to follow can take two lines.
- The strip's labels touched ("1,8 km3,6 km"): the overlap check used the marks' places, while a label at the end of the strip is moved inwards. Now the words must also stay apart where they end up, so at AX2 the stop's label gives way on this route ([screenshot](screenshots/ios-navigating-de-ax2.png)), also on 170 km ([AX2](screenshots/ios-strip-long-de-ax2.png)), and at the default size all labels stay ([170 km](screenshots/ios-strip-long-de.png)).
- At AX5, the actions were wider than the panel and clipped it on both sides. Navigate now gets its own line, and "Knotenpunkte hinzufügen" wraps. The switch below it scrolls sideways where it does not fit; "Ans Ende" could be reached and chosen ([screenshot](screenshots/ios-switch-de-ax5.png)).
- The taller smallest sheet hid the search field, as it counted as pulled up. At its smallest while planning, it no longer does.
- The credit was cut off for lack of room: its text now stops growing at the last size before the accessibility sizes, like the network switch.

### Android at large text

Found 2026-11-05 on the emulator (`Medium_Phone_API_37.0`) at font scale 2.0, the app's language set per app, positions from `emu geo fix`, and fixed:

- The strip's labels collided: "107 km170 km" on a 170 km route with the stop at about 63%, for the same reason as on iOS. The stop's label now gives way there ([screenshot](screenshots/android-strip-long-de-2.0.png)), and on 93 → 29 → 94 both labels stay ([screenshot](screenshots/android-navigating-de-2.0.png)).
- The switch cut "Dazwischen" to "Dazwische" and "Aan het eind" to "Aan het ein". Its parts are as wide as the longer one, so it now scrolls sideways where it does not fit: at scale 2.0, "Ans Ende" and "Aan het eind" are partly out of view until scrolled ([German](screenshots/android-switch-de-2.0.png), [Dutch, scrolled](screenshots/android-switch-nl-2.0.png)).
- Off route in Dutch, "zuidoost" broke as "zuidoos" and "t". The direction now goes below the distance where both do not fit ([screenshot](screenshots/android-offroute-nl-2.0.png)).

Holding at scale 2.0: the header close to a knot, Stop and the actions, in German and Dutch on the final build; in French before these fixes, which do not touch those parts ([screenshot](screenshots/android-navigating-fr-2.0.png)). The default size looks as before ([planning](screenshots/android-planning-de.png), [navigating](screenshots/android-navigating-de.png)). Android's font scale ends at 2.0, so there is no counterpart to iOS's AX5.

The final results on both platforms come from builds of the commit before this change's fixes with only these fixes on top, as another session's unfinished work shared the working tree and the emulator at the time.

### iOS at large text, final check

Checked 2026-11-05 in the simulator on the build with this change's fixes and without other work, close to a knot, far from it, off route and planning: German, Dutch and French at AX2 and AX5, English at AX5, German at the default size. Language set with `-AppleLanguages`.

In the app, the number to follow, the distance, Stop, Navigate and the off-route text are complete ([Dutch off route](screenshots/ios-offroute-nl-ax5.png), [French off route](screenshots/ios-offroute-fr-ax5.png), [French planning](screenshots/ios-planning-fr-ax5.png), [English planning](screenshots/ios-planning-en-ax5.png), [English navigating](screenshots/ios-navigating-en-ax5.png)).

Found and fixed: the Live Activity on the lock screen broke words at accessibility sizes close to a knot, "B/ei 94 fol/ge 41" in German at AX1, AX2 and AX5, "Bi/j 94 v/ol/g 41" in Dutch at AX5; English fit. "At" and "follow" had no fixed size, unlike "Next", so they were squeezed beside Stop. Now the words keep their width, and where "Bei 94 folge 41" does not fit in one line, it takes two, "Bei 94" and "folge 41". Checked again on the fixed build: two lines in German, Dutch and French at AX5 ([German](screenshots/ios-liveactivity-de-ax5.png), [Dutch](screenshots/ios-liveactivity-nl-ax5.png), [French](screenshots/ios-liveactivity-fr-ax5.png)), the same in German at AX1 and AX2, as the lock screen stops the text from growing there; one line in English at AX5 ([screenshot](screenshots/ios-liveactivity-en-ax5.png)) and in German at the default size ([screenshot](screenshots/ios-liveactivity-de-default.png)). Far from the knot, "Nächster 94" and "dann 41" are whole at AX5 ([screenshot](screenshots/ios-liveactivity-far-de-ax5.png)). The Dynamic Island and the end of the route ("ist das Ende" beside the badge, which now wraps between words) were not checked at large text.

Not fixed, for a change of its own: the zoom buttons are covered by the search field and the panel at accessibility sizes, partly at AX2 and fully at AX5 while planning; pinching still zooms.

### Open

With a Dutch phone, the map's buttons have the English screen reader labels "Compass" and "About this map"; in German and French they are "Kompass" / "Über diese Karte" and "Compas" / "À propos de cette carte". The Spanish phone reads them in English too, as expected there.

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

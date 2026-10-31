# Manual checks

## Scenarios

Android checked 2026-10-31 on the emulator (`Medium_Phone_API_37.0`, Android 17), system language English. iOS checked by the maintainer on the iPhone on 2026-10-31: "Works".

| Scenario | Android | iOS | Notes |
|---|---|---|---|
| Choosing German on an English phone | ✅ | ✅ | The info sheet stays open and turns German ("Darstellung", "Sprache", "Knotenpunktnetze"); the planner behind it "Orte suchen", "4,1 km", "etwa 15 min" ([English](screenshots/android-info-english.png), [Deutsch](screenshots/android-info-deutsch.png)) |
| Back to the phone's language | ✅ | ✅ | Choosing "Systeem" while Dutch: English again, the per-app language empty (`cmd locale get-app-locales`: `[]`) |
| Kept after a restart | ✅ | ✅ | German after force-stop and start |
| Notices in the chosen language | ✅ | ✅ | The service reads the app's language as set per app; logged "spoken in DE/NL/FR" with the per-app language in `add-app-languages` |
| Same as the system setting on Android | ✅ | n/a | Choosing Deutsch sets the per-app language to `[de]`; setting `nl` from outside shows "Nederlands" in the app's choice ([screenshot](screenshots/android-info-set-in-system-nl.png)) |
| Names of the languages | ✅ | ✅ | The menu reads "Systeem, English, Deutsch, Nederlands, Français" in Dutch |

iOS built and checked in the simulator (iPhone 18 Pro, iOS 27, system languages English and German) on 2026-10-31, the choice written from outside with `defaults write com.knoopbyknoop language` while the app ran, and the info sheet opened at full height by a temporary launch flag (removed again):

- Launched with each choice, the planner shows "5,2 km · etwa 19 min · Navigieren · Orte suchen · Geöffnet" in German, "ongeveer … · Navigeren · Plaatsen zoeken · Open" in Dutch, "environ … · Naviguer · Rechercher un lieu · Ouvert" in French, "5.2 km · about 19 min · Navigate" in English: SwiftUI's `locale` environment is enough for its `Text` literals, without the system's language.
- Switched while running, without a restart: German → Dutch → System (English) and French → German → System; the info sheet ("Darstellung"/"Weergave"/"Appearance", "Fertig"/"Klaar"/"Done"), the texts built in code ("Données du …" → "Daten vom …") and the route summary ("about 19 min" → "etwa 19 min") follow right away.
- The choice is stored as the app's `AppleLanguages` (seen in the app container's preferences).

Found and fixed: the info sheet's title stayed in the previous language when switching while it was open, as the navigation bar keeps its title; the sheet is rebuilt when the language changes.

### iOS steps for the maintainer

1. **Choosing German on an English phone**: with the iPhone in English, open Info, choose Sprache/Language → Deutsch. The info sheet ("Darstellung", "Sprache", "Fertig") and, after closing it, the planner ("Navigieren", "etwa … min", "Orte suchen", distances with a comma) are German right away.
2. **Back to the phone's language**: choose System: everything is in the iPhone's language again, right away.
3. **Kept after a restart**: choose Nederlands, close the app in the app switcher and start it again: the texts are Dutch. Settings → Apps → Knoop by Knoop → Language shows Dutch as well.
4. **Notices in the chosen language**: choose Français and navigate a short route: the notices are spoken in French ("Prochain point-nœud …") if the iPhone has a French voice, else in English.
5. **Names of the languages**: in every language, the choices read "English", "Deutsch", "Nederlands", "Français", and "System"/"Systeem"/"Système" in the app's language.
6. **Same as the system setting**: change the language in Settings → Apps → Knoop by Knoop → Language while the app's choice is System, and start the app: it follows that language. (A choice made in the app is shown there from the next start.)
7. The location prompt and the Live Activity follow the chosen language from the next start of the app.

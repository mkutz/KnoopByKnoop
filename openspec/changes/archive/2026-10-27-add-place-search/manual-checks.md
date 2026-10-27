# Manual checks

Checked 2026-10-27 on the Android emulator (`Medium_Phone_API_37.0`) against the public Photon instance. The iOS simulator cannot take typed text or go offline by script in this setup, so iOS was checked by the maintainer on an iPhone the same day. Scenarios marked – were not checked on the iPhone; their outcome comes from the shared core (`PlaceSearch.merge`), which both apps use unchanged.

On the iPhone, the hits first showed in the accent color (green on dark grey in dark mode), because the default button style tinted them; fixed with the plain button style and checked again.

| Scenario | Android | iOS | Notes |
|---|---|---|---|
| Station | ✅ | ✅ | "Utrecht Centraal": one hit "Train station, Utrecht", Photon's ten features merged ([screenshot](screenshots/android-station.png)) |
| Address | ✅ | – | "Oudegracht 158 Utrecht": the address "Oudegracht 158B, Utrecht" and the businesses there ([screenshot](screenshots/android-address.png)) |
| Business | ✅ | – | "Cafe Olivier Utrecht": "Olivier, Achter Clarenburg 6A, Utrecht" first ([screenshot](screenshots/android-business.png)) |
| No hits | ✅ | – | "Nothing found", after about 2 s, which is how long Photon takes for this query ([screenshot](screenshots/android-no-hits.png)) |
| Town far away | ✅ | ✅ | Map at Helmond, "Sluis": the town in Zeeland first, then hamlets, then the locks nearby ([screenshot](screenshots/android-town-far-away.png)) |
| Same name in several towns | ✅ | – | Covered by the core test with responses recorded near the Zeeland border; Assenede comes first |
| Place outside the covered area | ✅ | – | "Paris": no city of Paris, only businesses named "Paris" in Brussels, Ghent and Düsseldorf ([screenshot](screenshots/android-paris.png)) |
| Tapping a hit | ✅ | ✅ | Map at Utrecht Centraal, zoom 14, marker under the knots, route unchanged at 19.9 km ([screenshot](screenshots/android-hit-chosen.png)) |
| Removing the marker | ✅ | ✅ | ✕ clears the search and removes the marker |
| Airplane mode | ✅ | ✅ | Field disabled: "Search needs an internet connection" ([screenshot](screenshots/android-offline.png)) |
| Internet access is back | ✅ | ✅ | The field works again without a restart |
| Search service unavailable | ✅ | – | Simulated with an invalid private DNS host: "Search is not available right now", planning keeps working ([screenshot](screenshots/android-unavailable.png)) |

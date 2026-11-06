# Manual checks

## Scenarios

Checked 2026-11-06 with the published path file of 3 November, on a build of committed code plus these changes only. Android: emulator `Medium_Phone_API_37.0`, Android 17, with the app's data cleared. iOS: iPhone 18 Pro simulator, iOS 27, with the app uninstalled and installed again, driven by a throwaway UI test. Route 02 → 80 from Siegburg, 13 km, in English unless noted.

| Scenario | Android | iOS | Notes |
|---|---|---|---|
| First start | ✅ | ✅ | No setting touched. 18 path files (15 squares, the list, the directory and its entry); the panel says the ways are loaded |
| Route planned at home | ✅ | ✅ | Same as above |
| Opening the info sheet with path data | ✅ | ✅ | "Downloaded data" with Cycling, Hiking and "Ways for guidance", "Data of 2026-11-03 · 1.4 MB", Delete |
| Explanation | ✅ | ✅ | English, German, Dutch and French. Sizes use a decimal comma in German, Dutch and French ("1,4 MB") |
| Deleting the ways | ✅ | ✅ | Only the empty list of squares stays, the entry says "None downloaded" without a button, the networks stay installed |
| No path data installed | ✅ | ✅ | Seen right after deleting |
| Deleting with a route planned | ✅ | ✅ | No path files after 20 s and no status in the panel. Adding 30 to the route: the squares came back and "Ways for this route are loaded". Deleting again, then Navigate: the squares came back |
| Delete button with a screen reader | ✅ | ✅ | From the accessibility tree: one button labelled "Delete downloaded ways" ("Heruntergeladene Wege löschen", "Gedownloade paden verwijderen", "Supprimer les chemins téléchargés"). TalkBack and VoiceOver were not listened to |
| Status with a screen reader | ✅ | ✅ | The status is a plain text in the tree, as before |

Not checked by hand, covered by tests: "Changing the route", "Starting away from the route", "Far off route", "Starting before the download is done", "A hiking route", "Without network access", "Still downloading", "Not available", and the scenarios of "Guided way back to the route".

## For the maintainer

| Check | Pixel | iPhone | Notes |
|---|---|---|---|
| Install this build over the one with the switch, with a cycling route planned | ⏳ | ⏳ | The info sheet has no switch; the panel says the ways are loading, then loaded, also where the switch was off before |
| The info sheet's new entry, Delete, and the panel afterwards | ⏳ | ⏳ | Does "Ways for guidance" read well next to the networks? |

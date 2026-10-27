# Manual checks

Checked 2026-10-27 with the first merged publication (data of 2026-10-25), on the Android emulator (`Medium_Phone_API_37.0`), the iOS simulator (iPhone 18 Pro) and, by the maintainer, on an iPhone. ⏳ marks checks that are open or need to be repeated after a fix.

| Scenario | Android | iOS | Notes |
|---|---|---|---|
| Planning right after installation | ✅ | ✅ | Fresh install in airplane mode: the route is planned on the bundled graph, a tapped knot becomes a stop, connections are straight lines ([screenshot](screenshots/android-planning-offline.png)). iPhone: straight lines and the note in airplane mode |
| No network access on first start | ✅ | ⏳ | The note says the courses need an internet connection once ([screenshot](screenshots/android-first-start-offline.png)); back online, they download by themselves and the base map loads ([screenshot](screenshots/android-back-online.png)). iPhone: the courses arrived by themselves, but the base map only after a restart, and the start took several seconds; fixed, to be checked again |
| First start | ✅ | ✅ | Online: bundled graphs installed, both geometry files downloaded without any action; iOS after 30 s ([screenshot](screenshots/ios-after-sync.png)) |
| Leaving the app during the download | ✅ | ⏳ | Android: app left 1 s after start, all files installed 40 s later with the launcher in front |
| Interrupted download | ✅ | ⏳ | Android: covered by airplane mode during the first start; the download continued when network access was back |
| Cycling is ready first | – | – | Downloads finish within seconds, too fast to see; the order is covered by the core tests ("First start without bundled graphs …") |
| Opening the info sheet | ✅ | ⏳ | Data dates of both networks and the ODbL attribution ([screenshot](screenshots/android-info-sheet.png)). iPhone: the first tap showed nothing and hid the waypoint list; after a restart it worked; fixed, to be checked again |
| Airplane mode | ✅ | ✅ | Installed networks, restarted three times in airplane mode: route restored, courses drawn ([screenshot](screenshots/android-airplane-mode.png)). iPhone: route kept after a restart in airplane mode |
| Cycling network of the Netherlands | ✅ | ✅ | No gap at the border between Zeeland and Flanders |
| Route from the Netherlands into Belgium | ✅ | ✅ | Sluis (98) to Damme (6), 10.7 km across the border ([Android](screenshots/android-across-the-border.png), [iOS](screenshots/ios-first-start.png)) |
| Per-region data switches to the new networks (7.2) | ✅ | ⏳ | Android: route kept, merged networks installed, per-region files and `regions/` removed ([screenshot](screenshots/android-per-region-switch.png)). iOS: the simulator's per-region data was removed before it could be checked |

## Found and fixed during the checks

- Per-region graphs dated newer than the merged data (2026-10-26 vs. 2026-10-25) were kept, and the merged data was never installed. Installed files of another area now always count as outdated (core test "per-region data is replaced although its data date is newer").
- Replaced files stayed on disk. Stale graph and geometry files and `regions/` are removed on every start.
- Offline on first start, MapLibre waits for the base map instead of failing, so nothing was drawn. The offline style is used after 3 s and the base map is loaded when network access is back.
- Android: restoring the route raced with the first sync update, so the route was sometimes lost on start. The sync is only followed once the route is restored, on iOS as well.
- iOS: the info sheet was presented on top of the always-shown waypoint sheet, which iOS does not reliably show and which could dismiss the waypoint sheet. The info is now shown inside the waypoint sheet, with a Done button.
- iOS: the base map had no timeout and was not loaded again once internet access was back. Same behavior as on Android now.
- Both apps: "network" meant both the knot network and the internet connection. Texts about connectivity now say "internet connection".

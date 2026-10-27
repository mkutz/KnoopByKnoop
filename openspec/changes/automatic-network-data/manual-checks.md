# Manual checks

Checked 2026-10-27 with the first merged publication (data of 2026-10-25), on the Android emulator (`Medium_Phone_API_37.0`) and the iOS simulator (iPhone 18 Pro). Taps and network access cannot be scripted on the iOS simulator, so the checks that need them are open there (⏳).

| Scenario | Android | iOS | Notes |
|---|---|---|---|
| Planning right after installation | ✅ | ⏳ | Fresh install in airplane mode: the route is planned on the bundled graph, a tapped knot becomes a stop, connections are straight lines ([screenshot](screenshots/android-planning-offline.png)) |
| No network access on first start | ✅ | ⏳ | The note says the courses need network access once ([screenshot](screenshots/android-first-start-offline.png)); back online, they download by themselves and the base map loads ([screenshot](screenshots/android-back-online.png)) |
| First start | ✅ | ✅ | Online: bundled graphs installed, both geometry files downloaded without any action; iOS after 30 s ([screenshot](screenshots/ios-after-sync.png)) |
| Leaving the app during the download | ✅ | ⏳ | Android: app left 1 s after start, all files installed 40 s later with the launcher in front |
| Interrupted download | ✅ | ⏳ | Android: covered by airplane mode during the first start; the download continued when network access was back |
| Cycling is ready first | – | – | Downloads finish within seconds, too fast to see; the order is covered by the core tests ("First start without bundled graphs …") |
| Opening the info sheet | ✅ | ⏳ | Data dates of both networks and the ODbL attribution ([screenshot](screenshots/android-info-sheet.png)) |
| Airplane mode | ✅ | ⏳ | Installed networks, restarted three times in airplane mode: route restored, courses drawn ([screenshot](screenshots/android-airplane-mode.png)) |
| Cycling network of the Netherlands | ✅ | ✅ | No gap at the border between Zeeland and Flanders |
| Route from the Netherlands into Belgium | ✅ | ✅ | Sluis (98) to Damme (6), 10.7 km across the border ([Android](screenshots/android-across-the-border.png), [iOS](screenshots/ios-first-start.png)) |
| Per-region data switches to the new networks (7.2) | ✅ | ⏳ | Android: route kept, merged networks installed, per-region files and `regions/` removed ([screenshot](screenshots/android-per-region-switch.png)). iOS: the simulator's per-region data was removed before it could be checked |

## Found and fixed during the checks

- Per-region graphs dated newer than the merged data (2026-10-26 vs. 2026-10-25) were kept, and the merged data was never installed. Installed files of another area now always count as outdated (core test "per-region data is replaced although its data date is newer").
- Replaced files stayed on disk. Stale graph and geometry files and `regions/` are removed on every start.
- Offline on first start, MapLibre waits for the base map instead of failing, so nothing was drawn. The offline style is used after 3 s and the base map is loaded when network access is back.
- Android: restoring the route raced with the first sync update, so the route was sometimes lost on start. The sync is only followed once the route is restored, on iOS as well.

# Manual checks

## Done on the emulator and in the simulator (2026-11-11)

The check route is 14 → 15 straight across the ferry Mondorf–Graurheindorf, with the device at knot 14. The ferry closes at 19:00 on winter weekdays, and the checks ran at about 21:20. The ferry Lülsdorf–Wesseling from the task wasn't used: its operating times have Easter-based ranges and gave no status at 18:50 that day, so neither the warning nor the question would come then. Later that evening they did give one.

| Scenario | Android (emulator) | iOS (simulator) |
|---|---|---|
| Ferry closed on the way | ✅ "The ferry Autofähre Mondorf-Graurheindorf is closed when you get there around 21:20, it opens tomorrow at 06:30", "Ride around it · 13 km longer, about 49 min", "Keep the ferry" (`screenshots/android-question.png`) | ✅ the same texts (`screenshots/ios-question.png`) |
| Map while asking | ✅ zoomed to the ferry and the dashed way around over the bridge in Bonn, above the question | ✅ the same, above the question card |
| Riding around it | ✅ navigation on 14 → 79 → 80 → 81 → 82 → 27 → 83 → 15; the saved route has `avoidedFerries` (`screenshots/android-around.png`) | ✅ navigation on the way around, 14 km (`screenshots/ios-around.png`) |
| Map after the answer | ✅ the dashed line is gone, the route is the way around | ✅ the same |
| Keeping the ferry | ✅ navigation over the ferry, the plan unchanged, the spoken ferry warning right after (`screenshots/android-keep.png`) | — |
| Question in all languages | ✅ German (`screenshots/android-question-de.png`): "Fähre geschlossen", "Außen herum · 13 km länger, etwa 49 min", "Mit der Fähre" | — |

Also seen on the way:
- Dismissing the question (Back on Android) starts nothing and removes the line, as with the start question.
- With a stale position 85 km away, the arrival was estimated at 02:35. That's correct for that position, and shows the way to the start counts.
- Without a position within the 10 seconds Navigate waits, it navigates without asking, as it does for the start question. This happened once in the simulator.

## After the maintainer's first look on the iPhone (2026-11-11)

On a 378 km route with four closed ferries in dark mode, the question was hard to read (the system dialog is see-through glass over the busy map), the map zoomed out to half of North Rhine-Westphalia because the line ran from the first to the last change, and one ferry was listed twice. Fixed:
- iOS shows the question in the panel at half height instead of a dialog, as the info and the confirmations are shown.
- The way around is split into its separate detours; all are drawn, and the map zooms to the nearest closed ferry and the way around it.
- Each warning is listed once.

Checked again in the simulator in dark mode, route 21 → 15 with both ferries closed (`screenshots/ios-question.png`), and on the emulator (`screenshots/android-question.png`).

## Steps for the maintainer

On a phone, in the evening after a ferry's last crossing (Mondorf–Graurheindorf closes at 19:00 on winter weekdays):

1. Plan a route over the ferry, e.g. 14 → 15 at Mondorf, and press Navigate near it.
2. The question names the ferry, when you'd get there and when it opens, and the way around with its extra length and time. The map shows the ferry and the dashed way around above the question.
3. "Ride around it": navigation follows the way around. Stop, add a knot at the end: the route still avoids the ferry, also after restarting the app.
4. Plan over the ferry again (clear the plan first), press Navigate, "Keep the ferry": navigation goes over the ferry and says the ferry warning.
5. In the daytime, while the ferry runs, Navigate doesn't ask.

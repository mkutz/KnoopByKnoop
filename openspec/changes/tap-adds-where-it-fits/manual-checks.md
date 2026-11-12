# Manual checks

## Done on the emulator (2026-11-12)

Starting from a route 33 → 41 near Venray:

| Scenario | Seen |
|---|---|
| Note after a tap | ✅ Tapping 29 beside the route: "29 added as a stop" with "Make it the start", "Make it the end", "Undo" in the app's green (`screenshots/android-note-stop.png`) |
| Note for a new end | ✅ Tapping 28 far beyond the end: "28 added as the end" with "Make it the start" and "Undo" (`screenshots/android-note-end.png`) |
| Undo the tap | ✅ After adding 72, Undo brings the stored route back without it |
| Make it the end | ✅ 09 added as a stop, then "Make it the end": 09 is the end and the earlier end 28 a stop |
| Note goes away | ✅ It's gone after about 6 seconds |
| No note for the first two taps | ✅ After clearing the route, tapping 33 and 41 shows no note |

Found and fixed on the way:
- The note first sat in the bottom sheet's snackbar slot. It now sits in its own layer above the sheet, so it always gets its taps.
- Its buttons were Material's default purple. The theme now has its own `inversePrimary` greens.

## Done in the simulator (2026-11-12)

Route 33 → 41, then a tap on 06 near Lohmar, far beyond the end, driven by a throwaway UI test:

| Scenario | Seen |
|---|---|
| Note for a new end | ✅ "06 added as the end" with "Make it the start" and "Undo", above the map's attribution (`screenshots/ios-note-end.png`) |
| Undo the tap | ✅ The stored route is 33 → 41 again |
| Make it the start | ✅ The waypoints are 06, 33 as a stop, and 41 |

## Not yet checked

- Note with a screen reader (TalkBack, VoiceOver)
- How it feels on the phones: the maintainer's trial, to decide whether to keep it

## Steps for the maintainer

1. Plan a route by tapping a start and an end. No note appears.
2. Tap a knot beside the route: it becomes a stop, and the note offers to change it.
3. Tap a knot beyond the end: it becomes the new end. Try "Make it the start" and "Undo".
4. Plan a few real routes and judge whether the guess is right often enough, and whether the note helps or gets in the way.

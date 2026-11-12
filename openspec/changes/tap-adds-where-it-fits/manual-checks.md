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

## After the first trial: the note as a preview (2026-11-12)

The maintainer found the guesses good, but the text note too big and too wordy. It now shows badges.

| Scenario | Seen |
|---|---|
| Note after a tap (emulator) | ✅ Tapping 36 beside the route: the top row "… 43 36 43 …" with 36 highlighted and popping in; below it "36 43 60 …" as the start, "… 60 43 36" as the end, and an Undo icon; no text (`screenshots/android-preview-stop.png`) |
| Note for a new end (simulator) | ✅ Tapping 06 far beyond the end: "… 31 32 06", with 06 as the start and 06 as a stop (`screenshots/ios-preview-end.png`) |

On the way, the Android note shrank twice: first smaller badges, then Undo moved into the row of choices. It's now about as tall as the text note, without any text. The previews come from the real alternative routes, so an out-and-back detour shows as such.

## After the second trial: one line and three buttons (2026-11-12)

The maintainer liked the previews once used to them, but wondered at first what they were seeing. They asked for a little text, a fixed order and fewer badges.

| Scenario | Seen |
|---|---|
| Note after a tap (emulator) | ✅ "52 added as a stop", then [52 33] [51 52 51, selected] [09 52] and Undo, about 92 dp tall (`screenshots/android-three-buttons.png`) |
| Note for a new end (simulator) | ✅ "06 added as the end", then [06 33] [32 06 32] [41 06, selected] and Undo (`screenshots/ios-three-buttons.png`) |
| Make it the start (simulator) | ✅ "As the start": the waypoints are 06, 33 and 41 |
| Undo the tap (simulator) | ✅ The route is 33 → 41 again |

## After the third trial: the start as a candidate (2026-11-12)

On the phones, the maintainer saw a knot beyond the start (60 before 05 on Amsterdam → Zandvoort) become a stop, never the start. The start is now a candidate like the end.

| Scenario | Seen |
|---|---|
| Before the start (emulator) | ✅ Tapping 95 beyond the start 33: "95 added as the start", with the start button selected (`screenshots/android-new-start.png`) |

## Not yet checked

- Note with a screen reader (TalkBack, VoiceOver)
- How it feels on the phones: the maintainer's third trial, with one line and three buttons

## Steps for the maintainer

1. Plan a route by tapping a start and an end. No note appears.
2. Tap a knot beside the route: it becomes a stop, and the note offers to change it.
3. Tap a knot beyond the end: it becomes the new end. Try "Make it the start" and "Undo".
4. Plan a few real routes and judge whether the guess is right often enough, and whether the note helps or gets in the way.

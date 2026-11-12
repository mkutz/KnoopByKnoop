## Context

`PlanningSession.tap(knot, newKnots)` turns a tapped knot into a stop with `Route.insertStop(knot)`, which picks the leg with the least added distance, or into the new end with `Route.withNewEnd(knot)`. `NewKnots` comes from the switch: on Android `AppState.newKnots` with `KEY_NEW_KNOTS`, on iOS `AppModel.newKnots` in `UserDefaults`.

`startAt(point)` and `endAt(point)` already exist for "Start here" and "End here". Android shows short messages through a `SnackbarHostState`. iOS has no snackbar of its own.

## Goals / Non-Goals

**Goals:**
- One tap, one likely result, changeable right away. Quick to build, so it can be felt on the phones.
- The placement in the core, tested once for both apps.

**Non-Goals:**
- Places and route points, which keep their choices.
- Remembering anything between taps.

## Decisions

### Placement: the end as one more candidate

`Route.insertWhereItFits(knot)` computes the added distance for each leg, as `insertStop` does, plus one candidate after the end: the length of the shortest path from the end to the knot. A round trip (first waypoint equals last) gets no end candidate. On a tie the stop wins, since it changes the route least.

If no candidate is reachable, the knot goes before the end, as `insertStop` does today. The result says which it became (`STOP` or `END`), for the note.

`PlanningSession.tap(knot)` uses it once a route exists. The first two taps stay as they are.

*Alternative:* a fixed default, always a stop. That was rejected because extending the route, the main use of "At end", would then always need the note.

### Changing it: whole sessions, not edits

The app keeps the session from before the tap and the tapped knot while the note is shown.
- "Undo" shows that earlier session again.
- "Make it the start" is `before.startAt(knot)`, and "Make it the end" is `before.endAt(knot)`. They start from the earlier session, so the result is exactly what "Start here" or "End here" would have given, and the knot isn't removed and re-added.
- Each goes through the usual update, which stores the route.

### The note

Mockups: the "Compact note: badge previews instead of text" row (and the earlier text version in the "A tap adds where it fits" row) of https://claude.ai/artifact/NPjcSYcmLEZyvwu3CFSjne. It shows the iPhone with "27 added as a stop" and Android with "22 added as the end".

- **Android:** a small card above the panel, not a `Snackbar`. A snackbar has only one action, and Material advises against more. The card has the message and up to three text buttons.
- **iOS:** the same card in glass, above the sheet.

Both go away after 6 s, on a choice, or with the next tap (which shows its own). With TalkBack or VoiceOver on, they stay until used, and the message is announced as a polite live region on Android or an announcement on iOS. The texts are in all four languages.

### The note as a preview (after the first trial, 2026-11-12)

The maintainer found the text note too big and too wordy. It now shows badges.

The core's `TapOptions.of(before, after, knot)` gives:
- what the knot became, with a preview;
- one choice for each other role (start: `before.startAt(knot)`, end: `before.endAt(knot)`, stop: `before.stopAt(knot)`), each with its session and preview.

A preview is a window of up to three badges from that session's route knots around the tapped knot, with "…" where the route goes on. It comes from the real alternative route, so it shows what will happen, not a guess.

The apps draw the preview with their `KnotBadge`, including the start and end markers. The tapped knot is highlighted and pops in. Each choice is a button with its own preview, and Undo is an icon. Screen readers get sentences: "27 added as a stop between 14 and 15", "Make 27 the start".

The choices are computed off the main thread, together with the tap.

### Removing the switch

`NewKnots` goes from the core, along with the switch composable and view, their icons and strings, and the stored setting. A stale stored value is simply ignored.

## Risks / Trade-offs

- **The guess is wrong for a knot near both a leg and the end** → The note fixes it with one tap. The trial on the phones is meant to show whether that's too often.
- **The note covers part of the map** → It sits just above the panel and is small. It disappears quickly.
- **More taps while the note is shown** → Each tap replaces the note; "Undo" only ever undoes the last tap.

## Migration Plan

None, beyond ignoring the stored switch value. If the trial goes badly, the change is reverted before archiving.

## Context

Navigate goes through finding the position, maybe a start question and maybe ferry questions before navigation runs. `close-tap-note-on-navigate` closes the tap note at the press of Navigate by clearing it and counting the note id on, which also drops a note still being worked out. The other planning tools live in the same state: on Android `AppState.offer` (place card and "Add to route"), `addingAt`, `marker`, `locating`/`findingNearest` with `nearestKnots`, and the screen's `searching`; on iOS the same names in `AppModel`, and `searching` in `PlannerView`.

## Goals / Non-Goals

**Goals:** nothing from planning stays over the map or acts on it once Navigate is chosen.

**Non-Goals:** the info sheet (settings, not a planning tool); the position dot from "Show my position", replaced by the rider's position while navigating; the place card while navigating, which stays as specified.

## Decisions

- **At the press, not when navigation runs.** Same moment as the tap note, so the map clears at once. Cancelling the start question leaves them closed; reopening is one tap, and the alternative needs a saved state to restore.
- **Drop pending work by its id.** A spot being added already checks the tap-note id, so counting it on drops the result; `addingAt` is cleared too. "Show my position" and "Nearest knots" get a check that navigation was not chosen since they were asked for, before moving the map.
- **Search closes like when navigation starts today.** Android already closes it when navigation starts (`LaunchedEffect(state.navigating)`); it closes at the press now, with the marker. On iOS `searching` is closed the same way from `PlannerView`, watching the model.

## Risks / Trade-offs

- A rider who chose Navigate by mistake and cancels loses the open card or search hit. → Reopening is one tap; the route itself is untouched.

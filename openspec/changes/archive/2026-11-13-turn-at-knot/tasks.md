# Tasks

## 1. Core

- [x] 1.1 Extract `directionOf` from `PathRouter` into a shared function with the same limits. `PathRouterTest` still passes unchanged.
- [x] 1.2 Compute the turn per route knot from the route line, 40 m either side, across knots and between the signposts of a merged knot, and cache it with the route geometry. Tests in `NetworkGeometryTest`:
  - "Right turn at a knot", "Straight on" and "Slight left".
  - "Turning back at an out-and-back knot".
  - "Knot right after the next one" (46 10 m after 45).
  - "Knot with several signposts" (02 by the river and near the L 16).
  - "Without courses" and "Start knot", both `null`.
- [x] 1.3 Put `turn` on `HeaderRow.Close`, the turn character in `notificationTitle`, and the turn words in the spoken screen reader text. Tests in `NavigationTest`: "Far from the knot" (no turn on the far row), "Turn on the lock screen" (title "45 → ↱12") and "Turn with a screen reader".

## 2. Android

- [x] 2.1 Draw the plate's arrow per turn in the header and in the notification's large icon. Check on the emulator with the published data: a route with a right turn, a straight-on knot, an out-and-back knot and Siegburg 02 (two signposts). Take screenshots of the header and the notification.

## 3. iOS

- [x] 3.1 The same in the header, the Live Activity and the Dynamic Island. The project builds, and simulator screenshots show the same routes as 2.1.

## 4. Wrap-up

- [x] 4.1 `manual-checks.md` lists every scenario with how it was checked, plus ride steps for the maintainer: compare the plate's arrow with the real signpost at five knots, including a roundabout or offset junction if the ride passes one. `./gradlew check` and `npx @fission-ai/openspec validate --all` pass. A CHANGELOG entry goes under Unreleased (create the section if missing).

## Workflow follow-up

- Apply after `sign-like-header` and `sign-like-lock-screen`.
- If the arrows prove right on rides, a spoken direction in the reminder ("45 ahead, then right to 12") can be its own change.

## Notes

- 3.1, 2026-11-09: the header and the Live Activity on the lock screen checked in the simulator. 2026-11-11: the Dynamic Island too, with the app in the background; the compact island now shows the turn's arrow; its look around the real camera is for the maintainer's iPhone.

# Tasks

## 1. Core

- [x] 1.1 Add `HeaderRow.notificationTitle`, `notificationLine`, `chipText` and `minimal`, with the eight-way direction characters, the guided turn characters and ↩. Tests in `NavigationTest`:
  - "Far from the next knot in German": the same title in German and English.
  - "Far from the next knot outside the app", "Progress outside the app", "Off route outside the app", "On the way to the route outside the app" and the guided "On the lock screen", for the titles and lines.
  - "Live Update" and "Chip off route", for the chip texts, each at most 7 characters.
  - "Wrong direction outside the app": title and chip.
  - "Dynamic Island with room for one number": `minimal` per state.
- [x] 1.2 Update when the off-route arrow turns by 45° or the distance changes by 100 m. Tests in `NavigationTest`: "Updated on the way", and a turn of 50° without getting closer gives an update.
- [x] 1.3 Stop using `NavigationDisplay.text` and `offRouteText` outside the app. They stay in the core: their `Texts` entries also build the screen reader texts of the header, and the route point naming tests check them, so removing them would only move those tests. `./gradlew :modules:core:jvmTest` passes.

## 2. Android

- [x] 2.1 `NavigationService.kt`, covering:
  - the title and line from `HeaderRow`
  - large icons per state (plate, badge, symbol on dark or light disc, U-turn)
  - small icons for the to-the-route symbol and the U-turn
  - the chip text
  - ProgressStyle points in plate colours, with a dark ferry warning and the rider arrow as tracker

  Check on the emulator (Android 16 QPR2) with screenshots of the shade, the lock screen and the chip for: "Lock screen", "Far from the next knot outside the app", "Off route outside the app", "On the way to the route outside the app", "Live Update", "Chip off route", "Lock screen off route", "Back on route outside the app", "Wrong direction outside the app" and the guided "On the lock screen".
- [x] 2.2 Fallbacks. Check: "Live Update turned off" (Live Updates off in the settings), "Older Android" (an API 34 emulator), "Far from the next knot in German" at a larger font, and "Stop from the lock screen".
- [ ] 2.3 TalkBack on the notification. Note in `manual-checks.md` how the title is read. If it's unusable, record it as a follow-up idea.

## 3. iOS

- [x] 3.1 `ContentState` with `row` and `inverted`, and `Shared/Badges.swift` on the plate scheme. `NavigationLiveActivity.swift` draws:
  - the lock screen card with its own background
  - the expanded island
  - the compact and minimal island per state

  The project builds. Simulator screenshots of the lock screen and the island cover: "Lock screen", "Far from the next knot outside the app", "Dynamic Island", "Dynamic Island with room for one number", "Off route outside the app", "Off route in the Dynamic Island", "On the way to the route outside the app", "Progress bar in the Live Activity", "Wrong direction outside the app" and the guided "On the lock screen".
- [ ] 3.2 Light and dark. Simulator screenshots of "Live Activity in light and dark mode", including the inverted card in both modes, and "Stop from the lock screen".

## 4. Wrap-up

- [x] 4.1 `manual-checks.md` lists every scenario with how it was checked, plus the maintainer's ride steps on the Pixel and the iPhone:
  - lock screen and island in each state
  - the chip
  - a wrong turn and a start from home

  `./gradlew check` and `npx @fission-ai/openspec validate --all` pass. A CHANGELOG entry goes under Unreleased (create the section if missing).

## Workflow follow-up

- Apply after `knot-colours-from-signs` and `sign-like-header`.
- The maintainer checks it on rides before `/opsx:archive sign-like-lock-screen`.

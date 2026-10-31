# Tasks

## 1. Core

- [ ] 1.1 `Gpx` writer in `modules/core` with file name; tests on the test network: valid GPX 1.1 structure, waypoints before the track, track points equal the course ("Track along the network"), waypoints in riding order with numbers and types ("Knots as waypoints"), round trip ("Round trip"), file name ("Export a route"), XML escaping and coordinates without locale commas
- [ ] 1.2 A function telling whether a route can be exported (complete and with a course), with tests ("No export without a complete route")

## 2. Android

- [ ] 2.1 `FileProvider` and the share intent; the Export GPX icon button in the planner header, hidden while navigating; verified on the emulator: share sheet with the file name, file pulled and read back, hidden with only a start and while navigating, offline export, header in one row at 360 dp, TalkBack name ("Export a route", "No export while navigating", "Export while offline", "Navigate stands out", "Icon buttons with a screen reader"), with screenshots
- [ ] 2.2 Contrast of the new icon in light and dark measured from screenshots ("Readable actions", "Readable actions in dark mode")

## 3. iOS

- [ ] 3.1 The icon button and `UIActivityViewController` from the planner sheet; builds with `xcodebuild`; steps in `manual-checks.md` (share sheet, Save to Files, open in another app, VoiceOver, offline, contrast)

## 4. Wrap-up

- [ ] 4.1 `./gradlew check` and `npx @fission-ai/openspec validate --all` pass; CHANGELOG entry under "Unreleased"

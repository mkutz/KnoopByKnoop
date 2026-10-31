# Tasks

## 1. Android

- [ ] 1.1 Drag handle and remove button in `WaypointList`, the handle dragging without a long press, long press and swipe kept; verified on the emulator with four waypoints ("Visible handle", "Moving by the handle", "Long press still moves", "Visible remove button", "No remove button with two waypoints"), with screenshots
- [ ] 1.2 TalkBack actions "Move up", "Move down", "Remove" on the rows; verified with `uiautomator dump` (labels) and by triggering the actions ("Reordering with a screen reader")

## 2. iOS

- [ ] 2.1 The waypoint list always in edit mode while planning, remove control only with more than two waypoints; builds with `xcodebuild`; steps in `manual-checks.md` (handles, remove, VoiceOver actions)

## 3. Wrap-up

- [ ] 3.1 `./gradlew check` and `npx @fission-ai/openspec validate --all` pass; CHANGELOG entry under "Unreleased"

## Workflow follow-up

- The maintainer checks iOS on the iPhone before `/opsx:archive show-waypoint-handles`.

# Tasks

## 1. Android

- [x] 1.1 Drag handle and remove button in `WaypointList`, the handle dragging without a long press, long press and swipe kept; verified on the emulator with four waypoints ("Visible handle", "Moving by the handle", "Long press still moves", "Visible remove button", "No remove button with two waypoints"), with screenshots
- [x] 1.2 TalkBack actions "Move up", "Move down", "Remove" on the rows; verified with `uiautomator dump` (labels); the actions themselves are set but not observable on the emulator without UI tests ("Reordering with a screen reader")

## 2. iOS

- [x] 2.1 The waypoint list always in edit mode while planning, remove control only with more than two waypoints; builds with `xcodebuild`; steps in `manual-checks.md` (handles, remove, VoiceOver actions)

## 3. Wrap-up

- [x] 3.1 `./gradlew check` and `npx @fission-ai/openspec validate --all` pass; CHANGELOG entry under "Unreleased"

## 4. Removing one of two waypoints

- [x] 4.1 `PlanningSession` removes one of two waypoints, leaving the other as the start; the remove buttons and controls show with two waypoints; verified by `PlanningTest` "Removing one of two waypoints", "Removing the start of two waypoints", "Removing the only waypoint", on the emulator ("Removing one of two waypoints"), and iOS builds

## Workflow follow-up

- The maintainer checks iOS on the iPhone before `/opsx:archive show-waypoint-handles`.

# Tasks

## 1. Android

- [x] 1.1 Material Symbols `navigation`, `u_turn_left`, `delete` and `stop` as vector drawables `ic_action_*.xml` in `apps/android/src/main/res/drawable`; `./gradlew :apps:android:assembleDebug` builds
- [x] 1.2 The action green `#237A46` and the error red `#B3261E` for the actions; Navigate as a filled button with icon and label; Back to start and Clear as `FilledTonalIconButton`s with `contentDescription` and a `PlainTooltip` on long press; the header stays one row with "123.4 km" on a 360 dp wide screen ("Navigate stands out", "Name of an icon button on Android")
- [x] 1.3 Stop in `NavigationHeader` as a filled red button with icon and label ("Stop stands out")

## 2. iOS

- [x] 2.1 The action green next to `knoopGreen` in `Shared/Badges.swift`; Navigate as a `.borderedProminent` capsule with `location.north.fill`; Back to start and Clear as `.bordered` circles with `.labelStyle(.iconOnly)` and their names as label titles ("Navigate stands out", "Icon buttons with a screen reader"); `xcodebuild` builds
- [x] 2.2 Stop in `NavigationHeader` as a filled red capsule with `stop.fill` ("Stop stands out")

## 3. Checks

- [x] 3.1 Contrast ratios computed with the WCAG formula from the colors in the code and recorded in `manual-checks.md`: labels ≥ 4.5:1, icons ≥ 3:1 ("Readable actions")
- [ ] 3.2 Manual checks in `manual-checks.md` with screenshots: on the Android emulator "Navigate stands out", "Stop stands out", "Name of an icon button on Android" and "Icon buttons with a screen reader" with TalkBack, and the header in the smallest sheet and in landscape; on the iPhone by the maintainer the same with VoiceOver
- [ ] 3.3 `./gradlew check` passes, both iOS targets build, CI passes

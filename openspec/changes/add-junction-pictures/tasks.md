# Tasks

## 1. Doubtful junctions

- [x] 1.1 Core: find doubtful junctions on a guided way, merging parts within 40 m. Tests: "A similar exit close by", "Crossing a main road", "Leaving the road for a cycle path", "A plain turn".
- [x] 1.2 Measure the share of doubtful junctions on the eight public trips of `add-offline-paths` with the real path file and report it. **The maintainer decides whether to narrow the rules before any further work** (43 % in the rough test).
- [x] 1.3 Sentences and speech in all app languages. Tests: "Crossing a main road and turning", "Cycle path after a road", "A similar exit".

## 2. The picture

- [x] 2.1 Core: `JunctionPicture` from the way and the squares. Tests: "Picture of a junction", "Footways at a busy junction", "Roads that can't be ridden", the riding direction is up, and it fits in 1 KB for the six junctions of `sketches/`.
- [x] 2.2 Core: when a picture applies. Tests: "Approaching a doubtful junction in the app" (on the core's state), "After the junction", "Leaving the way before the junction", "No picture on an easy turn", "Screen comes on at a junction".

## 3. iOS

- [x] 3.1 The picture's view in SwiftUI, in the planner's header. A simulator ride with screenshots, in light and dark; "Picture with a screen reader" with VoiceOver's label.
- [x] 3.2 The picture in a real Live Activity on the maintainer's iPhone, using a test junction. **Stop and ask the maintainer if it's unreadable,** before going on. Check the content state's limit against the documentation.
- [ ] 3.3 Live Activity from the core's state, with the Dynamic Island as text. Checked by the maintainer: "On the iPhone's lock screen", "Dynamic Island at a doubtful junction", "Back to text", "Screen comes on at a junction".

## 4. Android

- [ ] 4.1 The picture in Compose in the planner's header. A mocked ride on the emulator with screenshots, in light and dark, and TalkBack's label for "Picture with a screen reader".
- [x] 4.2 `NavigationService` shows the sentence. On the emulator, "Text on the Android lock screen", also in German and French to check the length.

## 5. Wrap-up

- [x] 5.1 `manual-checks.md` with the scenarios and the maintainer's steps, including a real ride that notes whether each picture helped and whether any was wrong.
- [x] 5.2 `./gradlew check` and `npx @fission-ai/openspec validate --all` pass; a CHANGELOG entry under Unreleased.

## Workflow follow-up

- Start after `add-offline-paths` has been applied and ridden.

## Found and fixed

- 2026-11-09, 3.2: the picture in the Live Activity is built; a debug-only launch switch, `-junctionTest YES`, shows a test junction on the lock screen ([simulator](screenshots/ios-live-activity-test-junction.png)). The maintainer on the iPhone: "I think this might work but it's hard to tell without seeing the reality to the picture", so a provisional yes; the real verdict comes from a ride. The switch is removed again. The Live Activity takes the real picture from the core's state (3.3's code); 3.3 stays open for the maintainer's checks on a ride.
- 2026-11-09: a way to a route point lost its doubtful junctions when the network's course was put in front of it, and a picture would have left the panel taller for the rest of the ride; both fixed, see `manual-checks.md`. 4.1 stays open for Android in dark mode.

- Building the pictures compared every segment of up to five path-data squares with every other one, per doubtful junction, so a way search took 0.6 to 1.6 s on the JVM and much longer on the iPhone, where the way from the start flag was still a straight line after a minute (the maintainer, 2026-11-09). Segments far from the junction are now left out with a cheap check first: 9 to 19 ms on the JVM for the same trips, with the same pictures.

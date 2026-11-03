# Tasks

## 1. Android

- [x] 1.1 Vector drawables from Material Symbols "directions_bike" and "hiking" (rounded), with the source in a comment like the other icons.
- [x] 1.2 The icons in the `leadingIcon` of the `FilterChip`s in `PlannerScreen`, without a content description.
- [x] 1.3 Check on the emulator, with screenshots, restoring the route, camera, language, font scale, rotation and appearance: "The icons", "Selected network", "Switching with a route", "On a narrow phone" in all four languages at 360 dp, "Larger text" in German and French at 130%, "Very large text", "In landscape", "Screen reader" in the UI tree, and "Readable in both appearances" with measured contrast.

## 2. iOS

- [x] 2.1 `NewKnotsChoices` in `PlannerActions` becomes a generic control with a selected value and choices of a word and an icon, and the new knots switch uses it, looking and working as before.
- [x] 2.2 `networkSwitch` in `PlannerView` uses it with `networkBinding`, the SF Symbols `bicycle` and `figure.hiking`, and the label "Knot network"; the fixed width goes.
- [x] 2.3 Check in the simulator, with temporary launch flags that are removed afterwards, a grep that none is left, and screenshots, restoring the route, camera and settings: "The icons", "Selected network", "Switching with a route", "On a narrow phone" as far as the simulator allows, in English, German, Dutch and French, "Larger text", "Very large text", and "Readable in both appearances" with measured contrast. Also the new knots switch again.

## 3. Wrap-up

- [x] 3.1 `manual-checks.md` with the scenarios, what was and wasn't checked, and the maintainer's steps: tapping both controls on the iPhone, iPhone landscape, TalkBack and VoiceOver, and judging the map lost to the wider switch and the look on iOS.
- [x] 3.2 A CHANGELOG entry under Unreleased; `./gradlew check` and `npx @fission-ai/openspec validate --all` pass.

## Workflow follow-up

- These are UI changes without core logic, so the checks in 1.3 and 2.3 cover the scenarios instead of unit tests. The maintainer tries it on both phones before `/opsx:archive add-network-icons`, after `add-new-knots-switch` is archived.

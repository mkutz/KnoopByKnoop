# Tasks

## 1. Dark style

- [x] 1.1 `DarkStyle` generator in `pipeline` with the color mapping of the design and `:pipeline:darkStyle`; tests on liberty's colors (water blue and dark, background dark, labels light, halos dark, parks green) and that all layers are kept
- [x] 1.2 Generate and check in `map-styles/liberty-dark.json`; the Android and iOS builds ship it

## 2. Android

- [x] 2.1 `NetworkMap` loads the shipped dark style; verified on the emulator in dark mode at Renkum (river, woods, paths) and in a town, light and dark side by side ("Details on the dark map", "Dark mode"), with screenshots
- [x] 2.2 Appearance setting in the info sheet, stored, applied to theme, system bars and map without moving the map; verified on the emulator ("Appearance in the info sheet", "Choosing light", "Choosing dark", "Back to the system", "Choice kept", "Switching while the app is open"), with screenshots

## 3. iOS

- [x] 3.1 The shipped dark style, the setting in `InfoSheet.swift` with `preferredColorScheme`; builds with `xcodebuild`; steps in `manual-checks.md`

## 4. Wrap-up

- [x] 4.1 `./gradlew check` and `npx @fission-ai/openspec validate --all` pass; CHANGELOG entry under "Unreleased"

## Workflow follow-up

- The maintainer checks iOS on the iPhone before `/opsx:archive improve-dark-map`.

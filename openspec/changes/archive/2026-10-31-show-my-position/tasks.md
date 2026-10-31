# Tasks

## 1. Android

- [x] 1.1 Position button below the zoom buttons (hidden while navigating), a single fix with a 15 s timeout, centering at least at knot zoom, the dot; verified on the emulator with `adb emu geo fix` ("Centering on my position", "No tracking while planning", "Not while navigating"), with screenshots
- [x] 1.2 Permission on the first tap and on the first start (no `camera.json`, no route), messages for denied access (with Settings) and no fix; verified on the emulator after `pm revoke` / a fresh install ("First tap asks", "First start", "Later starts", "Access denied", "No position found")

## 2. iOS

- [x] 2.1 The same with `requestLocation` on its own manager, the usage text in `project.yml`; builds with `xcodebuild`; steps in `manual-checks.md`

## 3. Wrap-up

- [x] 3.1 `./gradlew check` and `npx @fission-ai/openspec validate --all` pass; CHANGELOG entry under "Unreleased"

## Workflow follow-up

- The maintainer checks iOS on the iPhone before `/opsx:archive show-my-position`.

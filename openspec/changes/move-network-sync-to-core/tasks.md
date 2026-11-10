# Tasks

## 1. Core

- [x] 1.1 `NetworkSyncState` with `installed`, `rejected`, `downloadFailed`, `finished`, `retry`, `retryDelayMillis` and JSON; `NetworkInstaller.check`. Tests in `NetworkSyncTest`:
  - "Corrupted download" and "Retrying a corrupted download": a corrupted rejection sets `retry`; delays 30 s, 60 s, 120 s, … capped at 5 h; a clean `finished()` resets them.
  - "Unsupported file version" and "Asked to update until it applies no more": the update need survives `finished()` and a JSON round trip.
  - "Updated app": installing that network's file clears it; installing the other network's doesn't.
  - `check`: a geometry without an installed graph is `MISMATCHING_GEOMETRY`.
  `./gradlew :modules:core:jvmTest :modules:core:iosSimulatorArm64Test` passes.

## 2. Android

- [ ] 2.1 `NetworkSync` on `NetworkSyncState` and `NetworkInstaller.check`, stored as JSON with the one-time migration; `NetworkSyncWorker` retries on `state.retry`. Emulator: a published file in an unsupported format, simulated by a local index with a graph whose format version the app can't read, served on the Mac and reached through `adb reverse` by a throwaway debug build with the base URL overridden (not committed), shows the request in the info sheet, still after a restart; a corrupted file is retried by WorkManager (logcat). `./gradlew check` passes.

## 3. iOS

- [ ] 3.1 `AppModel` on `NetworkSyncState` in `UserDefaults` and `NetworkInstaller.check`; the retry `Task` with `retryDelayMillis`. Simulator: the same two checks with a local index (the app's base URL overridden only in a throwaway debug build). The project builds.

## 4. Wrap-up

- [ ] 4.1 `manual-checks.md` with every scenario and how it was checked. `./gradlew check` and `npx @fission-ai/openspec validate --all` pass. CHANGELOG under Unreleased: Android now asks to update the app when the network data needs it, both apps keep asking until updated, and the iPhone retries a broken download by itself.

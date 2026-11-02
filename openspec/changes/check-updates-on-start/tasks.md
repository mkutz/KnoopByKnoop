# Tasks

## 1. Core

- [x] 1.1 Remove `UpdateCheck` from `Networks.kt` and the test "the update check is due once a day". "Nothing new" is covered by `NetworkSyncTest` "current data is installed" (no files to fetch); check that test still holds.

## 2. Android

- [x] 2.1 `AppViewModel` enqueues `NetworkSyncWorker` at every start; remove `NetworkSync.isDue` and `last-check`. On the emulator:
  - start the app and see the sync run in logcat ("Data published since the last start");
  - start it again right away and see it run again;
  - with the installed data current, the run fetches only the index ("Nothing new").

## 3. iOS

- [x] 3.1 `AppModel` runs `sync()` at every start; remove `isSyncDue` and `network-sync-last-check`. The project builds. In the simulator, two starts in a row both fetch the index, and an offline start followed by an online one leaves no "failed" in the info sheet.

## 4. Wrap-up

- [x] 4.1 `manual-checks.md` with the scenarios and the maintainer's steps; `./gradlew check` and `npx @fission-ai/openspec validate --all` pass; a CHANGELOG entry under Unreleased.

## Workflow follow-up

- The maintainer checks it on the Pixel and the iPhone with the next published data, before `/opsx:archive check-updates-on-start`.

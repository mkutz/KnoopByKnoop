# Manual checks

## How it was checked

Checked on 2026-11-10 with throwaway builds of both apps (not committed) whose base URL pointed at a local server on the Mac (`http://localhost:8765/`, reached through `adb reverse` from the emulator; cleartext and local networking allowed only in those builds). The server held the published files of 2026-11-10 and three indexes:

- **unsupported:** the cycling graph replaced by a copy with format version 99, size and checksum matching
- **corrupted:** the cycling graph listed with a checksum that doesn't match
- **current:** the published files as they are

The server's request logs are in `checks/`.

- **Android emulator** (`Medium_Phone_API_37.0`):
  - unsupported: the graph downloaded and rejected, the state `{"failed":["cycling"],"needsAppUpdateFor":["cycling"]}`, and the info sheet asked to update the app ([screenshot](screenshots/android-update-1.png)); before this change Android cleared the request in the same sync and never showed it.
  - restart with the server stopped: still asked ([screenshot](screenshots/android-update-2.png)).
  - corrupted: downloaded at 14:15:11, again at 14:17:12 by WorkManager (its 30 s backoff, started later by the job scheduler), `retries` counted; then, with the current index, the waiting retry at 14:21:12 fetched only the index, as the installed files were current, and the state became `{}`: no failure, no request ([screenshot](screenshots/android-update-3.png)).
- **iOS simulator** (iPhone 18 Pro), the info sheet opened by a throwaway UI test:
  - unsupported: asked to update the app ([screenshot](screenshots/ios-update-1.png)), state stored in `UserDefaults`.
  - restart with the server stopped: still asked ([screenshot](screenshots/ios-update-2.png)); before this change iOS forgot it at each start.
  - corrupted: downloaded at 14:25:27, again at 14:25:57 (30 s) and 14:27:01 (60 s and the download); before, iOS didn't retry until the next start.
  - current: the request and failure gone ([screenshot](screenshots/ios-update-3.png)). This showed `retries` staying at 3 when a sync had nothing to fetch, as iOS only ends a run after a download; fixed in the core (`started` with nothing to fetch ends the retries), tested, and checked again with a rebuilt throwaway build: `{}`.
- Afterwards both devices got their normal debug builds back, the server was stopped and the throwaway worktree removed.

## Scenarios

| Scenario | Android | iOS | Notes |
|---|---|---|---|
| Corrupted download | ✅ | ✅ | discarded, network kept, retried; `NetworkSyncTest` |
| Unsupported file version | ✅ | ✅ | the request in the info sheet; `NetworkSyncTest` |
| Retrying a corrupted download | ✅ (WorkManager, later than 30 s) | ✅ 30 s, 60 s | `NetworkSyncTest`: delays, the 5 h limit, one count per run |
| Asked to update until it applies no more | ✅ | ✅ | after a restart without the server; `NetworkSyncTest`: JSON round trip |
| Updated app | unit test | unit test | `NetworkSyncTest`: installing the network's file ends the request, the other network's doesn't |
| Published data readable again | ✅ | ✅ | the current index; `NetworkSyncTest` |

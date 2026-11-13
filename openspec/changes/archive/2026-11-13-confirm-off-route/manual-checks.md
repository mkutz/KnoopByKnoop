# Manual checks

## Scenarios

Android checked 2026-11-08 on the emulator (`Medium_Phone_API_37.0`), debug build, the route 06 → 02 near Lohmar (7.7 km), screen off. Positions came from the emulator's test GPS provider (`cmd location providers set-test-provider-location gps --accuracy …`), which sets the accuracy, along the course from the installed network. The shell's mock location permission and the test provider were removed again afterwards.

iOS checked in the simulator (iPhone 18 Pro, iOS 27), debug build, the planned round trip 02 → 06 → 02, with the app in the background. A throwaway XCUITest runner outside the repository started and stopped navigation, and `simctl location start` rode 1.6 km along the course from 02, then away from it sideways. `simctl` cannot set the accuracy, so the single 40 m fix was not reproduced in the simulator; the core decides it for both apps.

| Scenario | Android | iOS | Notes |
|---|---|---|---|
| A coarse position far from the course | ✅ emulator | unit test | Android: a single 40 m fix 200 m off the course at 1 km set the sampling to FINE at 19:55:31 without a notice; the next 5 m fix on the course set it back to COARSE at 19:56:07 |
| Leaving the route for real | ✅ emulator | ✅ simulator | Android: 10 m fixes moving away gave "Off route, next 02, 3.9 km to the south" once; the guided way was searched after it. iOS: "Off route, next 06, 5.1 km to the north" once, then the guided way |
| The same position delivered again | unit test | ✅ simulator | iOS: four precision changes in the whole run (FINE, LIVE, COARSE at the start, FINE off route), no bursts |
| Passing a knot not on the route | unit test | unit test | Unchanged code path |

### Steps for the maintainer

1. Ride with the iPhone and the screen off for an hour or more on the network.
2. Afterwards read the app's log (`log collect` as for the ride of 2026-11-08): no "Off route" while you were on the route, and no bursts of "precision:" lines within one second.
3. Leave the route once on purpose by more than 100 m: "Off route" comes within a few seconds.

# Manual checks

Checked 2026-10-27 on the Android emulator (`Medium_Phone_API_37.0`), rotated with `adb shell settings put system user_rotation`. The iOS simulator cannot be rotated by script in this setup, so iOS was checked by the maintainer on an iPhone.

| Scenario | Android | iOS | Notes |
|---|---|---|---|
| Planning in landscape | ✅ | ✅ | Map and waypoint list side by side ([screenshot](screenshots/android-landscape.png)); tapping knot 7 adds it as a stop, the list shows it right away ([screenshot](screenshots/android-landscape-planning.png)) |
| Rotating with a route | ✅ | ✅ | Route from Sluis via 7 to Damme, rotated to landscape and back: route, map position, zoom and network unchanged ([screenshot](screenshots/android-rotated-back.png)) |
| Map controls in landscape | ✅ | ✅ | Network switch, Info, zoom buttons and attribution visible; Info opens over map and list ([screenshot](screenshots/android-landscape-info.png)) |

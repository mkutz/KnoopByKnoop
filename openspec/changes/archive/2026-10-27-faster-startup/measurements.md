# Measurements

Release builds, times since the process started, from the startup marks (`adb logcat -s Startup` on Android; Console.app, subsystem `com.knoopbyknoop`, category `Startup` on iOS). A mark is set when the map has rendered the first feature of that layer, not when the data was handed to MapLibre: a first version that marked the handover was about 0.5 s too optimistic.

Targets ("Startup times"), compared against the slowest of five starts: map ≤ 1 s, knots ≤ 2 s.

## Baseline (before the change)

### Android emulator `Medium_Phone_API_37.0`, 2026-10-27

| Start | map | knots | courses |
|---|---|---|---|
| Normal (installed, online) | 980, 964, 1013, 1064, 1064 ms | 1505, 1484, 1544, 1546, 1571 ms | 1657, 1641, 1694, 1714, 1724 ms |
| First start, airplane mode | 4003, 3946, 3984, 3924, 4061 ms | 4419, 4355, 4361, 4295, 4428 ms | – |

Normal start: knots within target, the map 64 ms over. First start offline: the map waits for the 3 s base map timeout, and knots cannot be drawn before there is a style.

### iPhone

Skipped by the maintainer's decision.

## After the change

### Android emulator `Medium_Phone_API_37.0`, 2026-10-27

| Start | map | knots | courses |
|---|---|---|---|
| Normal (installed, online) | 796, 759, 776, 795, 805 ms | 1623, 1439, 1738, 1623, 1448 ms | 1626, 1561, 1743, 1643, 1452 ms |
| First start, airplane mode | 343, 383, 367, 405, 349 ms | 1430, 1529, 1453, 1517, 1502 ms | – |

Both targets met: the map within 1 s (slowest 805 ms), the knots within 2 s (slowest 1738 ms). The first start offline improved most, from about 4 s to 0.4 s for the map and from 4.4 s to 1.5 s for the knots, because the offline style is used right away.

### iOS simulator (iPhone 18 Pro), for comparison only

Not a reference device. Normal start: map 386–402 ms, knots 609–1048 ms, courses 626–1048 ms.

### iPhone

No numbers recorded. Started from the home screen (release build, without the debugger), the maintainer "hardly saw the load screen". Started from Xcode with the debugger attached, the first start took about 10 s; the launch screen with the app icon now covers that time.

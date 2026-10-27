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

To be measured by the maintainer.

## After the change

To be measured.

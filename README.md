# Knoop by Knoop

A navigator and route planner for the knooppunt networks for cycling and hiking in the Netherlands, Belgium and beyond.

- **Plan freely:** set a start and an end, then add, move and remove stops anywhere in between.
- **Navigate knoop by knoop:** the app only tells you the next knot, so the screen can stay off and the battery lasts.
- **Share routes** as links and QR codes.
- **Offline:** download a region once, plan and ride without a connection.

Network data © [OpenStreetMap](https://www.openstreetmap.org/copyright) contributors, available under the ODbL. Base map by [OpenFreeMap](https://openfreemap.org), © [OpenMapTiles](https://openmaptiles.org), data from OpenStreetMap.

## What the apps can do

- Download the knooppunt networks of the Netherlands, Belgium and Nordrhein-Westfalen; they are rebuilt weekly from OpenStreetMap
- Show the cycling or hiking network on a map, along the actual paths
- Plan a route by tapping knots: start, end and stops anywhere in between; reorder and remove stops in the waypoint list
- Keep the current route when the app is closed
- Work offline once a region is downloaded (the base map needs network access)

> Work in progress. See [`openspec/changes`](openspec/changes) for what is being built next.


## Repository layout

| Path | What |
|---|---|
| [`openspec/`](openspec) | Specs (`specs/`, the current truth) and proposed changes (`changes/`) |
| [`modules/core`](modules/core) | Kotlin Multiplatform core: network model, routing, sharing codec |
| [`apps/android`](apps/android) | Android app (Jetpack Compose) |
| [`apps/ios`](apps/ios) | iOS app (SwiftUI), project generated with XcodeGen |
| [`pipeline`](pipeline) | CLI turning OpenStreetMap data into graph files for the apps |


## Building

Requires JDK 21+. The Android app is only built when an Android SDK is found (`ANDROID_HOME` or `local.properties`).

```bash
./gradlew check
```

For the iOS app (requires Xcode and [XcodeGen](https://github.com/yonaskolb/XcodeGen)). Xcode builds the Kotlin core with Gradle and looks for a JDK in `JAVA_HOME`, the macOS JDK registry, SDKMAN and Android Studio, see [`apps/ios/build-core.sh`](apps/ios/build-core.sh):

```bash
xcodegen --spec apps/ios/project.yml && open apps/ios/KnoopByKnoop.xcodeproj
```


## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md).


## License

[Apache-2.0](LICENSE)

# Knoop by Knoop

A navigator and route planner for the knooppunt networks for cycling and hiking in the Netherlands, Belgium and beyond.

- **Plan freely:** set a start and an end, then add, move and remove stops anywhere in between.
- **Navigate knoop by knoop:** the app only tells you the next knot, so the screen can stay off and the battery lasts.
- **Share routes** as links and QR codes.
- **Offline:** download a region once, plan and ride without a connection.

Network data © [OpenStreetMap](https://www.openstreetmap.org/copyright) contributors, available under the ODbL.

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

For the iOS app (requires Xcode and [XcodeGen](https://github.com/yonaskolb/XcodeGen)):

```bash
xcodegen --spec apps/ios/project.yml && open apps/ios/KnoopByKnoop.xcodeproj
```


## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md).


## License

[Apache-2.0](LICENSE)

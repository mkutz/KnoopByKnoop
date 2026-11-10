# Tasks

## 1. Core

- [x] 1.1 `PathRouter.way` catches exceptions, forgets the loaded squares and returns `null`. Tests in `PathRouterTest` for "Broken path data": a square source that throws gives no way and no exception, and after it a search over working squares finds its way again. `./gradlew :modules:core:jvmTest :modules:core:iosSimulatorArm64Test` passes.

## 2. Apps

- [x] 2.1 Android: drop the catch of `Exception` in `NavigationService.findWay`, keep the one of `OutOfMemoryError`. `./gradlew :apps:android:assembleDebug` builds. iOS: no change; the project builds (`xcodebuild` as in the Build workflow).

## 3. Wrap-up

- [x] 3.1 `manual-checks.md` lists the scenario with how it was checked (the test on JVM and iOS simulator; a broken square can't be made on a phone without tools). `./gradlew check` and `npx @fission-ai/openspec validate --all` pass. A CHANGELOG entry under Unreleased: the app no longer stops when the path data is damaged.

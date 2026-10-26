# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.


## Project Overview

Knoop by Knoop is a navigator and route planner for knooppunt (node) networks for cycling and hiking, with native Android and iOS apps sharing a Kotlin Multiplatform core.
Network data comes only from OpenStreetMap.


## Spec-Driven Workflow

Behavior is specified with OpenSpec before it is implemented:

- `openspec/specs/` is the current truth, `openspec/changes/` holds proposed changes.
- Start new behavior with `/opsx:propose`, implement with `/opsx:apply`, finish with `/opsx:archive`.
- Every scenario in a spec delta needs a test that covers it.
- If code and spec disagree, raise it instead of silently changing either.


## Build Commands

```bash
# Run all checks (tests, formatting)
./gradlew check

# Format code
./gradlew spotlessApply

# Core tests on the JVM / on the iOS simulator (needs Xcode)
./gradlew :modules:core:jvmTest
./gradlew :modules:core:iosSimulatorArm64Test

# Android debug build (needs an Android SDK)
./gradlew :apps:android:assembleDebug

# iOS project (needs Xcode and XcodeGen)
xcodegen --spec apps/ios/project.yml

# Validate specs
npx @fission-ai/openspec validate --all
```


## Module Structure

- **modules/core** - Kotlin Multiplatform (JVM + iOS) core: network model, routing, sharing codec. No platform APIs.
- **apps/android** - Android app, Jetpack Compose. Consumes the core's JVM target. Only included when an Android SDK is found.
- **apps/ios** - iOS app, SwiftUI. Links the core as the static framework `KnoopByKnoopCore`, built by a pre-build script.
- **pipeline** - JVM CLI turning OpenStreetMap extracts into graph files for the apps.


## Code Conventions

- Kotlin, formatted with ktfmt Google style via Spotless (two-space indentation)
- Package root `com.knoopbyknoop`
- Prefer immutable data types
- Do not abbreviate variable names
- Test names describe behavior in backticks, e.g. `` `knots with the same ref in different places are different knots` ``

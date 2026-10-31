# Tasks

## 1. Skipping platforms without credentials

- [x] 1.1 `release.yml`: the `check` job outputs per platform whether its secrets are set, with a notice for a skipped platform; the platform jobs run only when their output is `true`; the release fails when both are skipped ("No App Store Connect credentials", "Credentials set")
- [ ] 1.2 Run a release without the Apple secrets: Android is delivered, iOS is skipped with the notice, the run is green

## 2. Apple setup (manual, once the Apple Developer Program membership exists)

- [ ] 2.1 Create the app `com.knoopbyknoop` in App Store Connect, the App ID for the widget extension `com.knoopbyknoop.widgets`, and an App Store Connect API key with the Admin role
- [ ] 2.2 Store the App Store Connect secrets as repository secrets and document their names in CONTRIBUTING.md

## 3. First iOS releases

- [ ] 3.1 Verify "Successful release", "Build numbers increase" and "Release notes" with two releases to TestFlight, including the Live Activity widget

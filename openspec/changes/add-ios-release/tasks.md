# Tasks

## 1. Skipping platforms without credentials

- [x] 1.1 `release.yml`: the `check` job outputs per platform whether its secrets are set, with a notice for a skipped platform; the platform jobs run only when their output is `true`; the release fails when both are skipped ("No App Store Connect credentials", "Credentials set")
- [x] 1.2 Run a release without the Apple secrets: Android is delivered, iOS is skipped with the notice, the run is green (0.4.0, run 41212734826)

## 2. Apple setup (manual, once the Apple Developer Program membership exists)

- [x] 2.1 Create the app `com.knoopbyknoop` in App Store Connect, the App ID for the widget extension `com.knoopbyknoop.widgets`, and an App Store Connect API key with the Admin role
- [x] 2.2 Store the App Store Connect secrets as repository secrets and document their names in CONTRIBUTING.md

## 3. First iOS releases

- [ ] 3.1 Verify "Successful release", "Build numbers increase" and "Release notes" with two releases to TestFlight, including the Live Activity widget

Progress on 3.1, 2026-11-06: first release to TestFlight, 0.10.0 (24), run 42227321864, green on the first try ("Successful release"). Installed from TestFlight on the maintainer's iPhone; the Live Activity works. Open: "Release notes" ("What to Test" stayed empty; d26a9f8 passes it for en-US and logs what Apple stored) and "Build numbers increase", both with the next release.

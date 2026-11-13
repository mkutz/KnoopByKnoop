# Design

## Context

- Releases already use fastlane on iOS. `release-ios.yml` writes a Fastfile at run time and authenticates with `app_store_connect_api_key`.
- Android releases upload through `r0adkll/upload-google-play` with `PLAY_SERVICE_ACCOUNT_JSON`. Neither workflow touches store listings.
- `design/store` holds the feature graphic, the 512 px icon and five Android screenshots from 2026-10-30, in English only.
- The apps speak English, German, French and Dutch (`app-languages`).
- The local App Store Connect key `NCMTDFGN37` has the Developer role. That role can read TestFlight feedback but not edit app metadata. The role of the release key in the repository secrets is unknown.

## Goals / Non-Goals

**Goals:**
- Texts and screenshots reviewable in git
- Pushing them is one click, repeatable, and cannot release anything

**Non-Goals:**
- Generating screenshots automatically (fastlane `snapshot`/`screengrab`). For now, five screenshots per language don't justify UI-test targets with demo data.

## Decisions

### fastlane metadata folders, one tree per store

The texts go in the folders `deliver` and `supply` read as they are:

```
fastlane/metadata/ios/<locale>/{name,subtitle,description,keywords,promotional_text,support_url,privacy_url}.txt
fastlane/metadata/ios/{primary_category,secondary_category,copyright}.txt
fastlane/metadata/android/<locale>/{title,short_description,full_description}.txt
fastlane/metadata/android/<locale>/images/{featureGraphic,icon}.png
fastlane/metadata/android/<locale>/images/phoneScreenshots/{1..5}-*.png
fastlane/screenshots/ios/<locale>/{1..5}-*.png
```

- **Locales:** `en-US`, `de-DE`, `fr-FR` and `nl-NL` on both stores.
- **Shared description:** `android/<locale>/full_description.txt` is a relative symlink to `ios/<locale>/description.txt`. Both stores then get the same text from one file. The check still compares the two, so a replaced symlink is caught.
- **Rejected:**
  - One shared source with a generator: one more tool for eight files.
  - Leaving the texts in the consoles: not reviewable, and drifts between the stores.

### Categories and age rating

- **Categories:**
  - App Store: primary category Navigation, secondary Health & Fitness.
  - Google Play: category Maps & Navigation, set once in Play Console. `supply` doesn't set categories.
- **Age rating:**
  - App Store: from an `app_rating_config.json` with every content question answered "none".
  - Play: content rating questionnaire filled in by hand, which already happened for closed testing.
- **Export compliance:** `ITSAppUsesNonExemptEncryption` is false. The app only uses HTTPS from the system. It goes in `project.yml` so no build asks again.

### The listing workflow

`.github/workflows/release-store-listing.yml`, run by hand (`workflow_dispatch`, `main` only). An optional input names the App Store version to write to. A check job first runs the listing check and looks for the credentials; then there are two store jobs. The `release-` prefix puts it under the existing secrets check, which lets only release workflows use secrets and only start them by hand.

**iOS job:** `deliver` with

| Option | Value |
|---|---|
| `skip_binary_upload` | `true` |
| `submit_for_review` | `false` |
| `automatic_release` | `false` |
| `overwrite_screenshots` | `true` |
| `run_precheck_before_submit` | `false` |
| `force` | `true` |
| `app_rating_config_path` | the JSON file |

It writes to the version being prepared. When none exists, `deliver` creates one with the version from `project.yml`.

**Android job:** `supply` with

| Option | Value |
|---|---|
| `skip_upload_apk` | `true` |
| `skip_upload_aab` | `true` |
| `skip_upload_changelogs` | `true` |
| `track` | `internal` (only as `supply` requires one; no release changes) |
| `sync_image_upload` | `true` (only changed images are uploaded) |

**Credentials:** the check job looks for each store's secrets and skips a store with a `::notice::`, as `release.yml` does.

**Rejected:** pushing the listing in every release. The listing changes rarely, and a typo fix shouldn't need a version.

### App Store credentials

`deliver` needs a key with the App Manager or Admin role. The release key in the `APP_STORE_CONNECT_API_KEY*` secrets is a team key with the Admin role (`CONTRIBUTING.md`), so the workflow uses it. The local Developer key `NCMTDFGN37` stays for reading feedback.

### Screenshots

Taken by hand with `adb` and `xcrun simctl`, in a throwaway script kept outside the repo.

**Devices:**
- Android: emulator at 1080 × 1920. Play wants 9:16 screenshots for its featured placements; the phone's own 20:9 would only be taken as an ordinary screenshot.
- iOS: iPhone 17 Pro Max simulator at 1320 × 2868.

**Status bar:** clean on both.
- Android: System UI demo mode, 12:00, full battery and signal, no notifications.
- iOS: `simctl status_bar override`.

**The route:** `fastlane/screenshots/demo-route.knoop`, a 12 km cycling loop from Kinderdijk, written with `RouteFile.write` from the published network: over the Kinderdijk ferry, along the south bank of the Lek to Nieuw-Lekkerland, over the Lekkerkerk ferry and back along the north bank. Every language shows the same route.

**Time of day:** ferry states, arrival times on the strip and "open now" follow the real clock. The screenshots are therefore taken while all the route's ferries are open: Saturday 10:30–16:30 or weekdays until 18:00. Taken in the evening, Navigate asks about closed ferries and the waypoint list says "Closed until tomorrow".

**Opening the route on Android:** the app reads a file opened from adb only from its own storage, so it is copied there with `run-as` and viewed as `file:///data/user/0/com.knoopbyknoop/files/demo-route.knoop`.

**The five screenshots, the same on both platforms:**
1. The planned route on the map with its panel
2. Navigating: the sign-like header and the next knot
3. The waypoint list with place names and Undo
4. A tapped campsite with its opening hours, and "Add to route"
5. The same route in dark mode

**Language:** each set is taken with the app set to that language. The device's language is left alone, as per-app languages make that unnecessary.

**No frames or captions.** The apps' own text shows the language, and plain screenshots need no design tooling. Captions can come later.

A checker reads the sizes from the PNG header (bytes 16–23) with `od`. This needs no ImageMagick or `sips`, so it also runs on the Ubuntu runner.

### iPhone only

- `apps/ios/project.yml` sets `TARGETED_DEVICE_FAMILY: "1"` for the app and the widget extension.
- The iPad orientations key (`UISupportedInterfaceOrientations~ipad`), if present, is removed.
- A shell test reads the setting from `project.yml`. This keeps the spec scenario "iPhone only" under test without building on the runner.

### The texts

The texts are written in English first, then in German, French and Dutch in the apps' own terms: "knooppunt" in Dutch, "Knotenpunkt" in German, "point-nœud" in French.

**Contents:**
- Planning by tapping knots
- Knot-by-knot navigation with the screen off
- Offline
- Ferries
- Places
- Sharing as `.knoop` files
- Free, no ads, no tracking
- The OpenStreetMap attribution

**Keywords:** no repeated words from the name. They are per language, e.g. `fietsknooppunt,fietsroute,wandelroute,…`.

**URLs:**
- Support: the GitHub repository's issues page
- Privacy: `PRIVACY.md` on GitHub, as Play Console has it today

## Risks / Trade-offs

- **Translations by Claude may read stiff to native speakers.** → The maintainer reads the German. Dutch and French can go to testers before the listing goes public.
- **Screenshots go stale** with each UI change. → `CONTRIBUTING.md` says when to retake them: after changes to the planner panel, navigation header or map controls.
- **`deliver` creates a 1.0 version** if no version is being prepared. → The workflow logs the version it wrote to. The maintainer runs it after the release that is meant for review.
- **iPhone only can't be undone for users on iPad** who installed an earlier TestFlight build. → TestFlight builds aren't App Store versions, so Apple's rule doesn't apply yet. Those testers get iPhone mode with the next build.

## Migration Plan

- `design/store/*` moves into the fastlane tree. `design/store/feature-graphic.svg` stays as the source of the graphic.
- Rollback: the stores keep what was last pushed. Pushing again from an older commit restores it.

## Open Questions

- Should a marketing URL (e.g. a `knoopbyknoop.com` page) be added? This can be decided after this change; it is one optional file.

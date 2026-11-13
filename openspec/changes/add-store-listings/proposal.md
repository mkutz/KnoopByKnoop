# Proposal

## Why

The first public release is close. Apple is only waiting for the App Store page, and Android's two weeks of closed testing are still running. Today, the Play listing exists only in Play Console, with screenshots from 30 October. Those predate the map controls redesign, Undo, the "Plan a ride" panel and route sharing. The App Store page doesn't exist yet. Both store pages should say the same thing, in the four languages the apps speak, and show the apps as they are now.

## What Changes

- The store texts live in the repository as fastlane metadata, for `en-US`, `de-DE`, `fr-FR` and `nl-NL`:
  - App Store: name, subtitle, description, keywords, promotional text, support and privacy URLs.
  - Google Play: title, short description, full description.
  - Both stores get the same description.
- A check in the build fails when a language misses a text, when a text is over its store's limit, or when the two descriptions differ.
- New screenshots for both platforms, in all four languages: five per language, taken on the same demo route from a `.knoop` file. A check fails on a missing screenshot or a size the store doesn't take.
- A manually started workflow, "Store listing", pushes the texts and screenshots from `main` to both stores. It uploads no build and submits nothing for review. As in releases, a platform whose credentials are missing is skipped with a notice.
- **BREAKING** (App Store only): the iOS app is for iPhone only (`TARGETED_DEVICE_FAMILY` 1). iPads run it in iPhone mode. This has to happen before the first App Store release, because Apple doesn't let an app drop iPad support once it has shipped with it. It also means Apple needs no iPad screenshots.
- `PRIVACY.md` and `README.md` say routes are shared as files, not as links and QR codes. The stores link to the privacy policy, so it has to be right.

## Out of Scope

- **Submitting for review and going to production.** The maintainer submits the App Store version by hand. Promoting the Play release to production waits for closed testing to end and is also done by hand.
- **Settings the API can't make**, done by hand in App Store Connect and listed in the design:
  - the App Privacy answers ("Data Not Collected", in line with `PRIVACY.md`)
  - the price (free) and availability
  - the content rights declaration
  - the Play Console data safety form
- **The tip jar's in-app purchases** (`add-tip-jar`). The listings mention no purchases until that change is done.
- **App previews (videos)** and a marketing website.
- **"What's New" on the App Store** from the changelog. Not needed for the first version; it can join the release workflow later.
- **iPad layouts.**

## Capabilities

### New Capabilities

- `store-listing`: what the stores show about the apps, kept in the repository and complete in every app language, and how it gets to the stores.

### Modified Capabilities

None. Releases (`release-delivery`) stay as they are and don't touch the listings.

## Impact

- **New files:**
  - `fastlane/metadata/ios/<locale>/*.txt` and `fastlane/metadata/android/<locale>/*.txt`
  - `fastlane/screenshots/ios/<locale>/` and `fastlane/metadata/android/<locale>/images/phoneScreenshots/`
  - the feature graphic and icon moved there from `design/store`
- **Workflows:**
  - new `.github/workflows/store-listing.yml`
  - `.github/scripts/store-listing-check.sh` with its test, run by `build.yml`
- **iOS:** `apps/ios/project.yml` sets `TARGETED_DEVICE_FAMILY` to 1. iPad-only Info.plist keys are dropped.
- **Secrets:** pushing App Store metadata needs an API key with the App Manager role. The design says how to check whether the release key already has it.
- **Docs:** `PRIVACY.md`, `README.md` and `CONTRIBUTING.md` (how to update the listing).
- **Core and app code:** no changes.

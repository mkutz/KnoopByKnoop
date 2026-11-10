# Proposal

## Why

The maintainer pays for the Apple Developer Program and works on the apps in their spare time. They want a way for riders who like the apps to send a little money, "to pay for my Apple Developer Account and basically buy me a coffee/beer" (2026-11-10). The apps stay free, with nothing locked.

## What Changes

- The info sheet gets a small "Support Knoop by Knoop" section with two tips: a coffee (about €2.99) and a beer (about €4.99), each with its price as the store shows it in the rider's currency.
- A tip is an in-app purchase of the store, a consumable that can be bought again and unlocks nothing. After paying, the section says thank you.
- Where the store can't be reached, or tips can't be bought (e.g. purchases restricted), the section says so instead of showing buttons.
- Nothing else changes: no reminders, no hints during planning or navigating, no accounts, nothing sent to a server of the apps.

## Out of Scope

- **External tip links** (Ko-fi, PayPal, GitHub Sponsors): App Review has rejected external tip links under guideline 3.1.1, and Google removed an app from Play over a donation link; both stores want their billing for this.
- **Subscriptions, more sizes, a one-time hint** after rides: the maintainer chose two tips in the info sheet only.
- **Thanks in the app beyond the message,** e.g. a badge for supporters.

## Capabilities

### New Capabilities

- `tip-jar`: tips through the stores' in-app purchases, in the info sheet.

### Modified Capabilities

None.

## Impact

- **iOS:** StoreKit 2 in a new `TipJar` model and a section in `InfoSheet.swift`; the strings in `Localizable.xcstrings`.
- **Android:** the Google Play Billing Library in a new `TipJar` class and a section in `InfoSheet.kt`; the dependency in `apps/android/build.gradle.kts`; the strings in all four languages.
- **Core:** none.
- **Outside the code, for the maintainer:**
  - App Store Connect: accept the Paid Apps agreement, add bank and tax details, and create two consumable in-app purchases, `tip.coffee` and `tip.beer`.
  - Play Console: set up a payments profile (merchant account) and create two one-time products with the same IDs.
  - Both stores keep 15% under their small-business programmes.

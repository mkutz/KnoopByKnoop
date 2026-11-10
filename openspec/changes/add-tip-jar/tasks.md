# Tasks

## 1. Stores (the maintainer)

- [ ] 1.1 App Store Connect: accept the Paid Apps agreement, add bank and tax details, and create the consumables `tip.coffee` (about €2.99) and `tip.beer` (about €4.99) with names in all four languages and a review note that they unlock nothing.
- [ ] 1.2 Play Console: set up the payments profile and create the one-time products `tip.coffee` and `tip.beer` with the same prices and names.

## 2. iOS

- [ ] 2.1 `TipJar` with StoreKit 2 and a StoreKit configuration file for the simulator; the section in `InfoSheet.swift`; strings in all four languages. Simulator screenshots: "Tips with prices", "A coffee", "Cancelled", "Waiting for approval" (Ask to Buy in the StoreKit configuration), "Offline" and "Purchases not allowed" (StoreKit configuration failure), light and dark.

## 3. Android

- [ ] 3.1 `TipJar` with the Play Billing Library; the section in `InfoSheet.kt`; strings in all four languages. Emulator screenshots of "Tips with prices" (with the products from 1.2, else "Tips not available"), "Offline", light and dark. "A coffee" and "Cancelled" with a license tester on the Pixel once 1.2 is done.

## 4. Release

- [ ] 4.1 Changelog line; `./gradlew check` and `npx @fission-ai/openspec validate --all` pass; manual checks: one real tip on each phone after 1.1 and 1.2.

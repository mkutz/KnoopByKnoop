# Design

## Context

- The info sheet exists on both apps (`InfoSheet.swift`, `InfoSheet.kt`) with the networks, the path data, the appearance, the language and the attribution. It is opened while planning only.
- The apps have no backend; nothing about a rider is sent anywhere but to the stores during a purchase.
- The maintainer chose in-app purchases over external links, two tips (coffee, beer), and the info sheet only (2026-11-10).

## Decisions

### Consumables with the same IDs in both stores
- `tip.coffee` (about €2.99) and `tip.beer` (about €4.99), consumable in App Store Connect and one-time products in the Play Console. The prices come from the stores' price tables per country; the apps show the store's formatted price, never their own.
- **Why consumables:** a tip can be given again; a non-consumable could be bought once only and would show as "purchased".

### iOS: StoreKit 2
- `TipJar` loads `Product.products(for:)`, buys with `product.purchase()`, verifies the transaction and calls `finish()`. It also listens to `Transaction.updates` from the start, for purchases that complete later (Ask to Buy) or were left unfinished. `AppStore.canMakePayments` gives "not allowed".
- A StoreKit configuration file in the Xcode project lets the simulator buy the tips without App Store Connect, for the checks.

### Android: Play Billing Library
- `TipJar` connects a `BillingClient` with pending purchases enabled, queries both products with `queryProductDetailsAsync`, launches the flow with the one-time offer, and on `PURCHASED` consumes with `consumeAsync`, which also acknowledges it. `PENDING` gives the waiting message; purchases still unconsumed at the next start are consumed then.
- The current library version is checked against the documentation when implementing. The license testers of the Play Console buy without being charged.

### No state kept
- The apps keep no record of tips, so nothing to restore and nothing to sync. The thank-you lasts while the sheet is open.

## Risks / Trade-offs

- **[Review]** → Tips as consumable in-app purchases are what Apple's guideline 3.1.1 allows for tipping; the products need a review screenshot and a note that they unlock nothing.
- **[Payments agreements take time]** → Until the maintainer has signed them, the products aren't found and the section says tips aren't available, so the apps can ship before.
- **[15% to the stores]** → Accepted by the maintainer with the choice of in-app purchases.

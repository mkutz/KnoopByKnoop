## ADDED Requirements

### Requirement: Tips in the info sheet
The info sheet SHALL show a section "Support Knoop by Knoop" with two tips, a coffee and a beer, each with its name and its price as the store gives it in the rider's currency. The section SHALL say that the apps stay free and that a tip unlocks nothing. Tips SHALL NOT be offered anywhere else in the apps.

#### Scenario: Tips with prices
- **WHEN** the rider in Germany opens the info sheet
- **THEN** it shows "Support Knoop by Knoop" with "Coffee 2,99 €" and "Beer 4,99 €"

#### Scenario: No tips while riding
- **WHEN** the rider plans a route or navigates
- **THEN** no tip is offered or mentioned

### Requirement: Paying a tip
A tip SHALL be paid with the store's in-app purchase, as a consumable that can be bought again and changes nothing in the apps. After a successful purchase, the section SHALL say "Thank you!" A cancelled purchase SHALL leave the section as it was, and a failed one SHALL say that the tip didn't go through.

#### Scenario: A coffee
- **WHEN** the rider taps the coffee and confirms the purchase in the store's dialog
- **THEN** the section says "Thank you!", and the coffee can be tipped again later

#### Scenario: Cancelled
- **WHEN** the rider taps the beer and cancels the store's dialog
- **THEN** the section shows the tips as before, without a message

#### Scenario: Waiting for approval
- **WHEN** the purchase needs approval, e.g. by a parent, or the payment is still pending
- **THEN** the section says the tip will go through once approved, and says thank you when it does

### Requirement: Tips not available
Where the store can't be reached, the products aren't found, or the device doesn't allow purchases, the section SHALL say that tips aren't available right now instead of showing the tips. Nothing else in the apps SHALL depend on the store.

#### Scenario: Offline
- **WHEN** the rider opens the info sheet without internet access
- **THEN** the section says tips aren't available right now, and the rest of the info sheet works as before

#### Scenario: Purchases not allowed
- **WHEN** in-app purchases are turned off in the device's restrictions
- **THEN** the section says tips aren't available right now

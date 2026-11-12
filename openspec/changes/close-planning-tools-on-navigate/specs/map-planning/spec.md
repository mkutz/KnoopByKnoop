## ADDED Requirements

### Requirement: Planning tools close on Navigate
When the user chooses Navigate, every planning tool still open SHALL close at once, before the position is found, since planning tools are not needed while the route is ridden: the card of a tapped place, a tapped spot or place still being added, which SHALL then not be added, the search with the marker of a chosen hit, and a map move for "Show my position" or "Nearest knots" not yet made, which SHALL then not be made. They SHALL stay closed when the user then cancels the start question.

#### Scenario: Place card on Navigate
- **WHEN** the card of a tapped café with "Add to route" is shown and the user chooses Navigate
- **THEN** the card goes away at once and the route is unchanged

#### Scenario: Spot being added on Navigate
- **WHEN** the user tapped an empty spot, chose "Add to route", and chooses Navigate while it is still worked out where it fits
- **THEN** nothing is added and nothing shows that it is being added

#### Scenario: Search hit on Navigate
- **WHEN** the map shows the marker of the search hit "Utrecht Centraal" and the user chooses Navigate
- **THEN** the marker goes away and the search is closed

#### Scenario: Pending map move on Navigate
- **WHEN** the user chose "Nearest knots" or "Show my position" and chooses Navigate before the map moved
- **THEN** the map does not move for it once the position or the knots are found

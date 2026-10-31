# Spec Delta

## ADDED Requirements

### Requirement: Strip labels at large text sizes
The distances and arrivals below the progress strip's marks SHALL grow with the system text size and SHALL never be cut off. Where larger labels would overlap, the ranking of "Distances and arrivals on the strip" SHALL decide which are left out.

#### Scenario: Largest text size
- **WHEN** the system text size is at its largest and the rider navigates to the end 94 at 4.1 km, arriving at 17:25
- **THEN** the strip shows "4.1 km" and "17:25" below 94, complete and readable

#### Scenario: Labels that would overlap at a large text size
- **WHEN** the system text size is large and a stop's distance and arrival would overlap those of the end
- **THEN** the end's distance and arrival are shown, and the stop's are left out

#### Scenario: Default text size
- **WHEN** the system text size is the default
- **THEN** the strip shows its labels as before

### Requirement: Number to follow at large text sizes
At large system text sizes, the number to follow, the distance to the next knot and Stop SHALL be shown completely. Where the distance does not fit in the line with the number to follow, it SHALL be shown in a line below it.

#### Scenario: Number to follow at the largest text size
- **WHEN** the system text size is at its largest and the rider is at 94 with 41 to follow, 467 m ahead
- **THEN** the panel shows "At 94 follow 41" and Stop completely, and "467 m" completely below them

#### Scenario: Number to follow at the default text size
- **WHEN** the system text size is the default
- **THEN** the distance stays in the line with the number to follow, as before

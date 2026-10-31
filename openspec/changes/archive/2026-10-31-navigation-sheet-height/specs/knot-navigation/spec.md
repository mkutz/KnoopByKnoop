# Spec Delta

## ADDED Requirements

### Requirement: Panel height while navigating
In portrait, when navigation starts, the panel SHALL take the height of its navigation part: the number to follow and the progress strip with its distances and arrivals, whatever height it had while planning. While navigating, it SHALL keep that height and show no handle to drag it. When navigation ends, it SHALL go back to its smallest planning height.

#### Scenario: Starting with the smallest panel
- **WHEN** the panel is at its smallest height in portrait and the rider starts navigating a route with the stop 30 at 5.2 km and the end 31 at 12.4 km
- **THEN** the panel shows the number to follow and the whole strip, with "5.2 km" and the arrival below 30 and "12 km" and the arrival below 31, without being dragged

#### Scenario: Starting with a high panel
- **WHEN** the panel is at half the screen in portrait and the rider starts navigating
- **THEN** the panel moves down to the height of the number to follow and the strip

#### Scenario: Dragging the panel
- **WHEN** the rider drags the panel down or up while navigating in portrait
- **THEN** the panel keeps the height of the number to follow and the strip with its distances and arrivals

#### Scenario: Ending navigation
- **WHEN** navigation ends by Stop or by arriving
- **THEN** the panel goes back to its smallest planning height

#### Scenario: Navigating in landscape
- **WHEN** the rider navigates in landscape
- **THEN** the side panel shows the number to follow and the whole strip, as before

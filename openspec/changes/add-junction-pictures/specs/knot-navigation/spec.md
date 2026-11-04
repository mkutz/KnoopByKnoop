# Spec Delta

## ADDED Requirements

### Requirement: Doubtful junctions
On a guided way, a turn or a crossing SHALL count as doubtful when another similar exit is close by, the way crosses a main road, or the way leaves a road for a separate cycle path or path. Parts of a junction within 40 m of each other SHALL count as one junction.

#### Scenario: A similar exit close by
- **WHEN** the way takes an exit, and another rideable exit leaves the same junction less than 35° from it
- **THEN** the junction is doubtful

#### Scenario: Crossing a main road
- **WHEN** the way crosses a primary, secondary or tertiary road, also going straight on
- **THEN** the junction is doubtful

#### Scenario: Leaving the road for a cycle path
- **WHEN** the way leaves a street for a separate cycle path
- **THEN** the junction is doubtful

#### Scenario: A plain turn
- **WHEN** the way turns between quiet streets, with no similar exit close by
- **THEN** the junction isn't doubtful, and the short turn notice is given without a picture

### Requirement: Wording for doubtful junctions
At a doubtful junction, the turn notice SHALL describe what makes it doubtful in a short sentence, on screen, outside the app and in the voice, in the app's language.

#### Scenario: Crossing a main road and turning
- **WHEN** the way crosses a main road 100 m ahead and then turns left
- **THEN** the app says "In 100 m, cross the main road, then turn left"

#### Scenario: Cycle path after a road
- **WHEN** the way leaves the street to the left for a cycle path 100 m ahead
- **THEN** the app says "In 100 m, take the cycle path on the left"

#### Scenario: A similar exit
- **WHEN** the way takes the left of two similar exits 100 m ahead
- **THEN** the app says "In 100 m, keep left at the fork"

#### Scenario: Text on the Android lock screen
- **WHEN** an Android rider approaches a doubtful junction with the screen off
- **THEN** the notification shows the sentence, without a picture

### Requirement: Junction picture
A junction picture SHALL show the junction with the riding direction up: the way in a bold line with its direction, main roads as wide lines, other streets and cycle paths as short exits, and no footways, text, or map details. It SHALL be readable in light and dark appearance.

#### Scenario: Picture of a junction
- **WHEN** the way turns right over two main roads onto a cycle path
- **THEN** the picture shows the way bold from the bottom to its end, the two roads wide, and the cycle path in its own color

#### Scenario: Footways at a busy junction
- **WHEN** a junction has footways next to the way
- **THEN** the footways aren't drawn, unless the way uses them

#### Scenario: Roads that can't be ridden
- **WHEN** the way crosses over a trunk road
- **THEN** the trunk road is drawn as a main road

#### Scenario: Picture with a screen reader
- **WHEN** a screen reader reaches the picture
- **THEN** it announces the sentence of "Wording for doubtful junctions"

### Requirement: Picture in the app
While navigating with the app on screen, the app SHALL show the picture from about 150 m before a doubtful junction until it has been passed.

#### Scenario: Approaching a doubtful junction in the app
- **WHEN** the rider on a guided way comes within 150 m of a doubtful junction with the app on screen
- **THEN** the header shows the picture next to the sentence, on Android and iOS

#### Scenario: After the junction
- **WHEN** the rider has passed the junction
- **THEN** the picture is gone

#### Scenario: Leaving the way before the junction
- **WHEN** the rider leaves the guided way before the junction, and a new way is found
- **THEN** the picture is gone, unless the new way has a doubtful junction within 150 m

#### Scenario: No picture on an easy turn
- **WHEN** the rider approaches a junction that isn't doubtful
- **THEN** no picture is shown

### Requirement: Picture on the iPhone lock screen
On iOS, the Live Activity on the lock screen SHALL show the picture from about 150 m before a doubtful junction until it has been passed, drawn from data in the activity, without more location fixes than off route uses. The Dynamic Island SHALL stay text only.

#### Scenario: On the iPhone's lock screen
- **WHEN** the rider on iOS comes within 150 m of a doubtful junction with the phone locked
- **THEN** the lock screen shows the picture next to the sentence and the distance

#### Scenario: Dynamic Island at a doubtful junction
- **WHEN** the rider approaches a doubtful junction with another app open
- **THEN** the Dynamic Island shows text, without the picture

#### Scenario: Back to text
- **WHEN** the rider has passed the junction
- **THEN** the lock screen shows the off-route text again

#### Scenario: Screen comes on at a junction
- **WHEN** the rider wakes the phone 80 m before a doubtful junction
- **THEN** the lock screen already shows the picture

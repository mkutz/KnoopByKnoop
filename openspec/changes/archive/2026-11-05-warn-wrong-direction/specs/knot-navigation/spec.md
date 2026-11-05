# Spec Delta

## ADDED Requirements

### Requirement: Riding the wrong way
While navigating with the network's courses, a rider within 30 m of the course of the current leg who has ridden at least 150 m back along it from the furthest place they reached on that leg, and has moved at least 100 m in a straight line from that place, SHALL be told they are riding the wrong way. The app SHALL say it once and show it until they turn around, pass the next knot or leave the course. It SHALL NOT plan a new route.

#### Scenario: Riding back along the route
- **WHEN** the rider heading to 28 joined the route 2.0 km before 28 and has ridden 150 m back along it, away from 28
- **THEN** the app says "Wrong direction, turn around, next 28" once

#### Scenario: Wrong direction in the panel
- **WHEN** the rider is riding the wrong way, 2.2 km along the route from 28
- **THEN** the panel shows "Wrong direction, next 28" in red with 28 as a badge and "2.2 km", the rider's dot on the strip is red, and the map shows no dashed line and keeps following them

#### Scenario: Wrong direction outside the app
- **WHEN** the rider is riding the wrong way with the screen off or in another app
- **THEN** the notification or Live Activity shows "Wrong direction, next 28" with its warning look, and the Dynamic Island shows the warning sign and 28

#### Scenario: In the app's language
- **WHEN** the app is in German and the rider is riding the wrong way
- **THEN** the app says "Falsche Richtung, bitte wenden, nächster Knotenpunkt 28" and shows "Falsche Richtung, nächster 28"

#### Scenario: Turning around
- **WHEN** the rider who was told they ride the wrong way turns around and has ridden 50 m along the route towards 28
- **THEN** the app says "Back on route, next 28" and shows the route's usual header

#### Scenario: Turning back shortly
- **WHEN** the rider rides 100 m back along the route, e.g. to pick something up, and goes on
- **THEN** the app says nothing

#### Scenario: A place that jumps
- **WHEN** the course doubles back past itself, and the nearest place on it jumps 300 m back while the rider has moved 20 m
- **THEN** the app says nothing

#### Scenario: Leaving the course while riding the wrong way
- **WHEN** the rider riding the wrong way gets more than 100 m away from the course
- **THEN** the app says they are off route, as on any other way off the course

#### Scenario: Passing the next knot
- **WHEN** the rider riding the wrong way passes the next knot by another way
- **THEN** the app gives the usual notice for the knot after it, and the warning is gone

#### Scenario: Riding back again later
- **WHEN** the rider turned around, rode on, and later rides 150 m back along the route again
- **THEN** the app says it again

#### Scenario: On the way back of an out-and-back route
- **WHEN** the route goes 02 → 01 → 02, and the rider passed 01 and rides the same way back
- **THEN** the app does not say they ride the wrong way

#### Scenario: Following the branch to another signpost
- **WHEN** the rider follows the course to a signpost of the next knot that is not on the main course
- **THEN** the app does not say they ride the wrong way

#### Scenario: Before the first knot
- **WHEN** the first knot is still the next knot
- **THEN** the app does not say they ride the wrong way

#### Scenario: Without geometry
- **WHEN** the network's courses are not installed
- **THEN** the app does not say they ride the wrong way

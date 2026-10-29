# Spec Delta

## ADDED Requirements

### Requirement: Display on while navigating in the app
While navigating and the app is in the foreground, the display SHALL stay on, overriding the system's display timeout. When the app is left or navigation ends, the system's normal display timeout SHALL apply again.

#### Scenario: Navigating with the app open
- **WHEN** the rider navigates with the app open and does not touch the phone for longer than the system's display timeout
- **THEN** the display stays on

#### Scenario: Turning the display off
- **WHEN** the rider presses the power button while navigating
- **THEN** the display turns off and navigation continues with spoken notices

#### Scenario: Leaving the app
- **WHEN** the rider switches to another app while navigating
- **THEN** the display turns off after the system's timeout as usual

#### Scenario: Navigation ends
- **WHEN** navigation ends by Stop or by arriving and the app stays open
- **THEN** the display turns off after the system's timeout as usual

#### Scenario: Planning
- **WHEN** the app is open and not navigating
- **THEN** the display turns off after the system's timeout as usual

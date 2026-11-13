# Spec Delta

## Purpose
Takes the store screenshots of both apps on throwaway CI devices at a fixed moment, in every app language, so they look the same whenever they are taken and can be reviewed before they are committed.

## ADDED Requirements

### Requirement: Screenshots taken by a workflow
The maintainer SHALL start one workflow by hand that takes the store screenshots of both apps in English, German, French and Dutch from the demo route `fastlane/screenshots/demo-route.knoop`. These are five per platform and language, in this order:
1. the planned route with the panel
2. navigating between knots 03 and 21
3. the waypoint list
4. a tapped place with its details
5. the route in dark mode

They SHALL be taken on Android at 1080 × 1920 and on iPhone at 1320 × 2868, with the app set to the screenshot's language.

#### Scenario: Screenshots for review
- **WHEN** the maintainer starts the screenshot workflow
- **THEN** the run has one artifact with 40 screenshots in the folders of the store listing (`fastlane/metadata/android/<locale>/images/phoneScreenshots/` and `fastlane/screenshots/ios/<locale>/`) and a contact sheet showing them side by side

#### Scenario: Screenshot in the wrong language
- **WHEN** a screenshot is taken for German
- **THEN** the app's texts on it are German, also on the iPhone, whose system language may differ

### Requirement: A fixed moment
The workflow SHALL set the clock of both devices to the coming Saturday at 12:00 in Amsterdam (Europe/Amsterdam) before the apps start, so ferry states, arrival times and opening hours on the screenshots are the same whenever it runs. The moment SHALL lie at most seven days ahead of the real time, so the services' certificates stay valid. The status bars SHALL show 12:00, full battery and no notifications.

#### Scenario: Coming Saturday
- **WHEN** the workflow runs on Wednesday 2026-11-18 at 20:00 in Amsterdam
- **THEN** the devices' clocks show Saturday 2026-11-21 at 12:00 Amsterdam time

#### Scenario: Run on a Saturday afternoon
- **WHEN** the workflow runs on Saturday 2026-11-21 at 15:00 in Amsterdam
- **THEN** the devices' clocks show Saturday 2026-11-28 at 12:00 Amsterdam time

#### Scenario: Summer time
- **WHEN** the workflow runs on Wednesday 2027-06-09
- **THEN** the devices' clocks show Saturday 2027-06-12 at 12:00 Amsterdam time, which is 10:00 UTC

#### Scenario: Closed ferry
- **WHEN** a ferry of the demo route is closed at that moment, e.g. between Christmas and New Year
- **THEN** the run fails and names the ferry, instead of producing screenshots with a closed ferry

### Requirement: Screenshots are reviewed before they are committed
The workflow SHALL NOT write to the repository or use secrets. Its screenshots reach `main` only when the maintainer has looked at them and they are committed by hand.

#### Scenario: No write access
- **WHEN** the screenshot workflow runs
- **THEN** its token can only read the repository, and it uses no repository secrets

# Spec Delta

## ADDED Requirements

### Requirement: Ferry open now
The ferry list SHALL show for each ferry with known operating times whether it is open now according to OpenStreetMap and, where that can be told too, when that changes next, in the local time of the ferry's region. The operating times as tagged SHALL stay visible below the status, and the status SHALL stay current while the list is shown. When the operating times do not allow a sure answer, the ferry SHALL show no status.

#### Scenario: Open now
- **WHEN** a ferry runs "Apr-Oct: Mo-Su 07:00-19:00" and it is Wednesday, 16 June 2027, 14:00
- **THEN** the ferry shows "Open until 19:00" above "Apr-Oct: Mo-Su 07:00-19:00"

#### Scenario: Opens later today
- **WHEN** the same ferry is shown on Wednesday, 16 June 2027, 06:15
- **THEN** it shows "Closed, opens at 07:00"

#### Scenario: Opens tomorrow
- **WHEN** the same ferry is shown on Wednesday, 16 June 2027, 20:00
- **THEN** it shows "Closed, opens tomorrow at 07:00"

#### Scenario: Closed for the season
- **WHEN** the same ferry is shown on Wednesday, 2 December 2026
- **THEN** it shows "Closed, opens on 1 April at 07:00"

#### Scenario: Open past midnight
- **WHEN** a ferry runs "Mo-Th 06:00-24:00; Fr-Su 06:00-01:00" and it is Saturday, 19 June 2027, 00:30
- **THEN** it shows "Open until 01:00"

#### Scenario: Open around the clock
- **WHEN** a ferry runs `24/7; "Not in ice conditions"`
- **THEN** it shows "Open" above the tagged times, which still show the comment

#### Scenario: Public holiday
- **WHEN** a ferry runs "Mo-Sa 06:00-19:00; PH,Su 09:00-19:00" and it is Christmas Day, Friday, 25 December 2026, 08:00
- **THEN** it shows "Closed, opens at 09:00"

#### Scenario: Holiday in only some countries
- **WHEN** the same ferry is shown on Good Friday, 26 March 2027, 08:00, a public holiday in Nordrhein-Westfalen but not in the Netherlands or Belgium
- **THEN** it shows no status, only the tagged times

#### Scenario: School holidays
- **WHEN** a ferry runs "Sa,Su,PH 09:30-19:40; Mar-Sep SH 09:30-19:40" and it is Wednesday, 16 June 2027, 10:00
- **THEN** it shows no status, only the tagged times

#### Scenario: Unreadable times
- **WHEN** a ferry's operating times are tagged as "April-October"
- **THEN** it shows no status, only the tagged times

#### Scenario: Phone in another time zone
- **WHEN** a ferry runs "Apr-Oct: Mo-Su 07:00-19:00" and the phone is set to London time, where it is 18:30 on Wednesday, 16 June 2027 (19:30 in the Netherlands)
- **THEN** it shows "Closed, opens tomorrow at 07:00"

#### Scenario: Closing while shown
- **WHEN** the ferry list is shown at 18:59 for a ferry that closes at 19:00
- **THEN** its status changes to closed within a minute after 19:00, without any action

#### Scenario: Offline status
- **WHEN** the device has no internet access
- **THEN** the status, including public holidays, is shown as online

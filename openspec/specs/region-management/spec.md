# region-management Specification

## Purpose
Gets the knooppunt networks of a region onto the device and keeps them current, so the apps can plan offline.

## Requirements

### Requirement: List available regions
The app SHALL list all regions of the published index with their download size, the date of their data and whether they are installed.

#### Scenario: First start
- **WHEN** the user opens the app for the first time with network access
- **THEN** the app lists the Netherlands, Belgium and Nordrhein-Westfalen with their sizes and data dates, none of them installed

#### Scenario: No network access
- **WHEN** the user opens the region list without network access
- **THEN** the app lists the installed regions and says that other regions need network access

### Requirement: Download a region in the background
Installing a region SHALL download all of its networks, each with its graph and its geometry. The download SHALL continue when the user leaves the app and SHALL show its progress while the app is open.

#### Scenario: Leaving the app during a download
- **WHEN** the user starts downloading the Netherlands and switches to another app
- **THEN** the download continues, and the region is installed when the user returns after it finished

#### Scenario: Interrupted download
- **WHEN** the network connection drops during a download
- **THEN** the download resumes when the connection is back, without starting over

### Requirement: Install only verified data
A downloaded file SHALL only be installed if its size and checksum match the index. Installed data SHALL stay usable until verified replacement data is installed.

#### Scenario: Corrupted download
- **WHEN** a downloaded file does not match the checksum in the index
- **THEN** the file is discarded, the region keeps its previous state, and the user sees that the download failed and can retry

#### Scenario: Unsupported file version
- **WHEN** a downloaded file has a format version the app does not support
- **THEN** it is not installed, and the user is asked to update the app

### Requirement: Update regions
The app SHALL offer an update for an installed region when the index has newer data for it.

#### Scenario: Newer data available
- **WHEN** the index lists a newer data date for an installed region
- **THEN** the region list marks it as updatable, and planning on the installed data keeps working until the update is installed

### Requirement: Remove a region
The user SHALL be able to remove an installed region to free storage.

#### Scenario: Remove the planned region
- **WHEN** the user removes the region of the route being planned
- **THEN** the region's files are deleted and the current route is discarded

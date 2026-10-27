# Spec Delta

## REMOVED Requirements

### Requirement: List available regions
**Reason**: Users no longer choose regions; the published data is one network per type.
**Migration**: The data dates and the state of downloads are shown in the info sheet, see `network-sync` "Info sheet".

### Requirement: Download a region in the background
**Reason**: Downloads start by themselves and cover whole networks instead of regions.
**Migration**: See `network-sync` "Download the networks automatically", which keeps the behavior for leaving the app and for interrupted downloads.

### Requirement: Install only verified data
**Reason**: Moved to the capability that now owns downloads.
**Migration**: See `network-sync` "Install only verified data"; a corrupted download is retried later instead of waiting for the user.

### Requirement: Update regions
**Reason**: Updates are installed in the background without the user doing anything.
**Migration**: See `network-sync` "Keep the networks current".

### Requirement: Remove a region
**Reason**: With about 6 MB for all networks there is no storage worth freeing, and removing data would only break planning.
**Migration**: None. Files of the old per-region data are deleted by the app when the new networks are installed, see `network-sync`.

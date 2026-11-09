# Design

## Context

See `proposal.md` for why. `release.yml` has a `check` job (green main, release notes) and calls `release-android.yml` and `release-ios.yml` with `workflow_call` and `secrets: inherit`. The iOS workflow uses cloud-managed signing with the App Store Connect API key and `fastlane pilot`, see the design of `add-release-pipelines`. Without the Apple secrets it fails while archiving.

## Goals / Non-Goals

**Goals:**
- Release runs are green while iOS is not set up, so a red run means something.
- Once the Apple membership exists, adding the secrets is all it takes to release iOS.

**Non-Goals:**
- Choosing platforms per release with an input: releases stay one trigger for everything that can be released.

## Decisions

### The check job tells which platforms can be released
A job-level `if:` cannot read `secrets`, so the `check` job maps each platform's secrets to `env` and sets the outputs `android` and `ios` to `true` when all of them are non-empty. The calling jobs get `if: needs.check.outputs.ios == 'true'`; a skipped platform gets a `::notice::` naming the missing secrets, without their values. The secrets check in `build.yml` already allows secrets in `release.yml`.
*Alternative:* a repository variable like `IOS_RELEASE_ENABLED`. It can disagree with the secrets and is one more thing to set.

### Android is skipped the same way
The same rule applies to Android, so the requirement holds for both platforms and a fork of the repository can release whichever store it has.

## Risks / Trade-offs

- [A secret is set but wrong] → The platform runs and fails visibly, as now.
- [Both platforms skipped] → The release fails: a run that delivers nothing is not a successful release.

# Manual checks

| Scenario | Check | Result |
|---|---|---|
| Successful release | Releases 0.1.0 to 0.1.4 (Release runs 1 to 5): the Android job uploaded a signed App Bundle to the internal testing track each time; the maintainer got them on the Pixel from Play | ✅ |
| Build numbers increase | Version codes 1, 2, 3, 4, 5 from the run numbers; Play accepted each upload | ✅ |
| Release notes | Each upload carried the changelog section as `whatsnew-en-US`, accepted by Play | ✅ |
| Missing changelog section, Failing checks, Pull requests from forks | Covered by `release-notes-test.sh`, the green-main step and `check-workflow-secrets-test.sh` in the Build workflow | ✅ |

The iOS job failed in every run, as expected without an Apple Developer Program membership; see `add-ios-release`.

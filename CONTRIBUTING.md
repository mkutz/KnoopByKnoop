# Contributing

Thanks for your interest in Knoop by Knoop!


## Spec first

This project is spec-driven with [OpenSpec](https://github.com/Fission-AI/OpenSpec).
Every change in behavior starts as a change proposal in `openspec/changes/<change-id>/`:

1. **Propose:** `proposal.md` (why, what, out of scope), spec deltas in `specs/<capability>/spec.md`, and `tasks.md`.
   With Claude Code: `/opsx:propose "your idea"`.
2. **Discuss** the proposal in a pull request or issue before implementing it.
3. **Apply:** implement the tasks; each spec scenario gets a test. With Claude Code: `/opsx:apply`.
4. **Archive:** once merged, move the deltas into `openspec/specs/`. With Claude Code: `/opsx:archive`.

Validate specs with:

```bash
npx @fission-ai/openspec validate --all
```

Pure maintenance (dependency updates, refactorings, build changes) needs no proposal.


## Code

- `./gradlew check` must pass, including formatting. Fix formatting with `./gradlew spotlessApply`.
- Kotlin is formatted with ktfmt (Google style).
- Logic that both apps need belongs in `modules/core` and must not use platform APIs.
- Do not abbreviate names.


## Releasing

Releases are started by hand with the workflow **Release** in GitHub Actions, with the version as input.
`CHANGELOG.md` needs a section `## <version>`; its entries become the release notes.
The release only runs when the latest Build on `main` succeeded; later commits may only change Markdown files.
It uploads the Android App Bundle to Google Play's internal testing track, with the run number of the Release workflow as build number.
A platform whose secrets are not all set is skipped with a notice in the run, e.g. iOS until the App Store Connect secrets exist; the release fails only when neither platform can be released.

The release workflows need these repository secrets:

| Secret | Content |
|---|---|
| `ANDROID_UPLOAD_KEYSTORE_BASE64` | The upload keystore, base64-encoded |
| `ANDROID_UPLOAD_KEYSTORE_PASSWORD` | Password of the upload keystore |
| `ANDROID_UPLOAD_KEY_ALIAS` | Alias of the upload key in the keystore |
| `ANDROID_UPLOAD_KEY_PASSWORD` | Password of the upload key |
| `PLAY_SERVICE_ACCOUNT_JSON` | JSON key of the Google Play service account allowed to release |

Only the release workflows may use secrets; a check in the Build workflow fails otherwise.
Without the keystore variables, local release builds are signed with the debug key.

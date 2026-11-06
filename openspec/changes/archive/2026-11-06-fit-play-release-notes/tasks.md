# Tasks

- [x] 1.1 `.github/scripts/play-release-notes.sh`: reads notes on stdin and prints them fitted into a limit (500 by default), with its test `play-release-notes-test.sh` run in `build.yml`. Tests: "Release notes" (short notes unchanged), "Long release notes" (whole entries, the pointer, at most 500 characters), a single entry too long on its own.
- [x] 1.2 `release-android.yml` writes the fitted notes for Play.
- [x] 1.3 The real 0.10.0 section fits: run the script on it and check the length.
- [x] 1.4 Release 0.10.0 again; the Play upload succeeds.

Released as 0.10.0 on 2026-11-06 (run 42213377326): Play got the first entry and the pointer to the changelog.

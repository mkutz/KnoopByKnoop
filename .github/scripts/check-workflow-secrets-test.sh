#!/bin/sh
# Tests for check-workflow-secrets.sh. Run from the repository root.
set -eu

script=.github/scripts/check-workflow-secrets.sh
workflows=$(mktemp -d)
trap 'rm -rf "$workflows"' EXIT

fail() {
  echo "FAIL: $1" >&2
  exit 1
}

reset() {
  rm -f "$workflows"/*.yml
  printf 'on:\n  pull_request:\njobs: {}\n# ${{ secrets.GITHUB_TOKEN }}\n' > "$workflows/build.yml"
  printf 'on:\n  workflow_dispatch:\njobs: {}\n# ${{ secrets.PLAY_SERVICE_ACCOUNT }}\n' > "$workflows/release.yml"
  printf 'on:\n  workflow_call:\njobs: {}\n# ${{ secrets.APP_STORE_CONNECT_KEY }}\n' > "$workflows/release-ios.yml"
}

reset
sh "$script" "$workflows" > /dev/null 2>&1 || fail "secrets in the release workflows pass"

sh "$script" > /dev/null 2>&1 || fail "the repository's workflows pass"

reset
printf 'on:\n  pull_request_target:\n' > "$workflows/labels.yml"
if sh "$script" "$workflows" > /dev/null 2>&1; then fail "pull_request_target fails"; fi

reset
printf 'on:\n  pull_request:\n# ${{ secrets.PLAY_SERVICE_ACCOUNT }}\n' > "$workflows/build.yml"
if sh "$script" "$workflows" > /dev/null 2>&1; then fail "a store secret outside the release workflows fails"; fi

reset
printf 'on:\n  workflow_call:\n  pull_request:\n' > "$workflows/release-android.yml"
if sh "$script" "$workflows" > /dev/null 2>&1; then fail "a release workflow started by pull requests fails"; fi

echo "check-workflow-secrets.sh: all tests passed"

#!/bin/sh
# Checks that the workflow Store screenshots can only read the repository and uses no secrets, see
# `store-screenshots` "Screenshots are reviewed before they are committed". Run from the
# repository root. Usage: screenshots-workflow-test.sh [workflow]
set -eu

workflow=${1:-.github/workflows/screenshots.yml}

fail() {
  echo "FAIL: $1" >&2
  exit 1
}

# Scenario: No write access
if grep -nE '^ *[a-z-]+: *write *$|write-all' "$workflow"; then
  fail "$workflow asks for write access"
fi
grep -qE '^permissions:' "$workflow" || fail "$workflow does not limit its permissions"
grep -qE '^  contents: read$' "$workflow" || fail "$workflow does not read the contents only"
if grep -nE '\$\{\{[^}]*secrets\.' "$workflow"; then
  fail "$workflow uses secrets"
fi

echo "screenshots-workflow-test.sh: $workflow can only read"

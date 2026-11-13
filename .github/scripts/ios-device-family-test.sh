#!/bin/sh
# Tests for ios-device-family.sh. Run from the repository root.
set -eu

script=.github/scripts/ios-device-family.sh
fixture=$(mktemp)
trap 'rm -f "$fixture"' EXIT

fail() {
  echo "FAIL: $1" >&2
  exit 1
}

spec() {
  printf 'name: App\ntargets:\n  App:\n    type: application\n    settings:\n      base:\n        TARGETED_DEVICE_FAMILY: "%s"\n  Widgets:\n    type: app-extension\n    settings:\n      base:\n        TARGETED_DEVICE_FAMILY: "%s"\nschemes:\n  App:\n    build: {}\n' "$1" "$2" > "$fixture"
}

# Scenario: iPhone only
sh "$script" > /dev/null 2>&1 || fail "the repository's iOS targets are for iPhone only"

spec 1 1
sh "$script" "$fixture" > /dev/null 2>&1 || fail "targets for iPhone only pass"

spec 1 "1,2"
if sh "$script" "$fixture" > /dev/null 2>&1; then fail "a target for iPhone and iPad fails"; fi

spec 1 1
sed -i.bak '/Widgets/,/TARGETED/{/TARGETED/d;}' "$fixture" && rm -f "$fixture.bak"
if sh "$script" "$fixture" > /dev/null 2>&1; then fail "a target without a device family fails"; fi

echo "ios-device-family.sh: all tests passed"

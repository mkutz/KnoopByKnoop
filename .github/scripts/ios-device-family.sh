#!/bin/sh
# Fails unless every target in the XcodeGen spec is for iPhone only (TARGETED_DEVICE_FAMILY "1"),
# see `store-listing` "The iOS app is for iPhone". Run from the repository root.
# Usage: ios-device-family.sh [project.yml]
set -eu

spec=${1:-apps/ios/project.yml}

awk '
  /^targets:/ { in_targets = 1; next }
  /^[^ #]/ { in_targets = 0 }
  in_targets && /^  [A-Za-z0-9_-]+:[ ]*$/ {
    name = $1; sub(/:$/, "", name); targets[name] = ""; current = name; next
  }
  in_targets && /TARGETED_DEVICE_FAMILY:/ {
    value = $0; sub(/.*TARGETED_DEVICE_FAMILY:[ ]*/, "", value); gsub(/"/, "", value)
    targets[current] = value
  }
  END {
    failed = 0
    for (name in targets) {
      if (targets[name] != "1") {
        printf "%s: target %s is not for iPhone only (TARGETED_DEVICE_FAMILY: %s)\n", FILENAME, name, (targets[name] == "" ? "not set" : targets[name]) > "/dev/stderr"
        failed = 1
      }
    }
    exit failed
  }
' "$spec"

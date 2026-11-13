#!/bin/sh
# Tests for store-listing-check.sh. Run from the repository root.
set -eu

script=.github/scripts/store-listing-check.sh
listing=$(mktemp -d)
trap 'rm -rf "$listing"' EXIT

fail() {
  echo "FAIL: $1" >&2
  exit 1
}

# A complete listing within the limits, in every language.
reset() {
  rm -rf "${listing:?}"/*
  for locale in en-US de-DE fr-FR nl-NL; do
    ios="$listing/metadata/ios/$locale"
    android="$listing/metadata/android/$locale"
    mkdir -p "$ios" "$android"
    echo "Knoop by Knoop" > "$ios/name.txt"
    echo "Ride knot by knot" > "$ios/subtitle.txt"
    echo "knooppunt,cycling" > "$ios/keywords.txt"
    echo "Plan and ride" > "$ios/promotional_text.txt"
    echo "A navigator for knot networks." > "$ios/description.txt"
    echo "https://example.com/issues" > "$ios/support_url.txt"
    echo "https://example.com/privacy" > "$ios/privacy_url.txt"
    echo "Knoop by Knoop" > "$android/title.txt"
    echo "Plan and ride the knot networks" > "$android/short_description.txt"
    ln -s "../../ios/$locale/description.txt" "$android/full_description.txt"
  done
}

# Prints n times the character $1.
repeat() { python3 -c 'import sys; print(sys.argv[1] * int(sys.argv[2]))' "$1" "$2"; }

reset
sh "$script" "$listing" > /dev/null 2>&1 || fail "a complete listing passes"

# Scenario: Missing text
reset
rm "$listing/metadata/ios/nl-NL/subtitle.txt"
if output=$(sh "$script" "$listing" 2>&1); then fail "a missing subtitle fails"; fi
case "$output" in *"ios/nl-NL/subtitle.txt: missing"*) ;; *) fail "names the Dutch subtitle, got: $output" ;; esac

# Scenario: Text over the limit
reset
repeat a 81 > "$listing/metadata/android/de-DE/short_description.txt"
if output=$(sh "$script" "$listing" 2>&1); then fail "81 characters in a short description fail"; fi
case "$output" in *"android/de-DE/short_description.txt: 81 characters, at most 80"*) ;; *) fail "names the file and its length, got: $output" ;; esac

reset
repeat a 80 > "$listing/metadata/android/de-DE/short_description.txt"
sh "$script" "$listing" > /dev/null 2>&1 || fail "80 characters in a short description pass"

# Scenario: Limits count characters, not bytes
reset
repeat é 30 > "$listing/metadata/ios/fr-FR/subtitle.txt"
sh "$script" "$listing" > /dev/null 2>&1 || fail "30 accented characters in a subtitle pass"

# Scenario: Different descriptions
reset
rm "$listing/metadata/android/en-US/full_description.txt"
echo "Another text." > "$listing/metadata/android/en-US/full_description.txt"
if output=$(sh "$script" "$listing" 2>&1); then fail "different descriptions fail"; fi
case "$output" in *"android/en-US/full_description.txt: differs from"*"ios/en-US/description.txt"*) ;; *) fail "names both files, got: $output" ;; esac

echo "store-listing-check.sh: all tests passed"

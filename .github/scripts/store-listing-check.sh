#!/bin/sh
# Checks the store listing in fastlane/, see `store-listing`: every app language has every text,
# each within its store's limit in characters, and the same description on both stores.
# Names each file at fault and fails if there is one. Run from the repository root.
# Usage: store-listing-check.sh [fastlane directory]
set -eu

root=${1:-fastlane}
locales="en-US de-DE fr-FR nl-NL"
failed=0

problem() {
  echo "$1" >&2
  failed=1
}

# Characters, not bytes, without the trailing newlines the stores drop.
chars() { python3 -c 'import sys; print(len(open(sys.argv[1], encoding="utf-8").read().rstrip("\n")))' "$1"; }

text() {
  file=$1
  limit=$2
  if [ ! -s "$file" ]; then
    problem "$file: missing"
    return
  fi
  length=$(chars "$file")
  if [ "$length" -gt "$limit" ]; then
    problem "$file: $length characters, at most $limit"
  fi
}

for locale in $locales; do
  ios="$root/metadata/ios/$locale"
  android="$root/metadata/android/$locale"

  text "$ios/name.txt" 30
  text "$ios/subtitle.txt" 30
  text "$ios/keywords.txt" 100
  text "$ios/promotional_text.txt" 170
  text "$ios/description.txt" 4000
  text "$ios/support_url.txt" 255
  text "$ios/privacy_url.txt" 255

  text "$android/title.txt" 30
  text "$android/short_description.txt" 80
  text "$android/full_description.txt" 4000

  if [ -s "$ios/description.txt" ] && [ -s "$android/full_description.txt" ] &&
    ! cmp -s "$ios/description.txt" "$android/full_description.txt"; then
    problem "$android/full_description.txt: differs from $ios/description.txt"
  fi
done

exit "$failed"

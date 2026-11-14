#!/bin/sh
# Checks the store listing in fastlane/, see `store-listing`: every app language has every text,
# each within its store's limit in characters, the same description on both stores, and five
# screenshots per platform in sizes the stores take.
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

# Width and height of a PNG, from its header.
size() { od -An -tu1 -j16 -N8 "$1" | awk '{ print $1*16777216+$2*65536+$3*256+$4, $5*16777216+$6*65536+$7*256+$8 }'; }

screenshots() {
  directory=$1
  label=$2
  count=$(find "$directory" -maxdepth 1 -name '*.png' 2> /dev/null | wc -l | tr -d ' ')
  if [ "$count" -ne 5 ]; then
    problem "$directory: $count $label screenshots, needs 5"
  fi
}

for locale in $locales; do
  ios="$root/screenshots/ios/$locale"
  android="$root/metadata/android/$locale/images/phoneScreenshots"

  screenshots "$ios" iPhone
  for file in "$ios"/*.png; do
    [ -f "$file" ] || continue
    set -- $(size "$file")
    if [ "$1" -ne 1320 ] || [ "$2" -ne 2868 ]; then
      problem "$file: $1 × $2, needs 1320 × 2868"
    fi
  done

  screenshots "$android" Android
  for file in "$android"/*.png; do
    [ -f "$file" ] || continue
    set -- $(size "$file")
    if [ "$1" -lt 320 ] || [ "$2" -gt 3840 ] || [ $(($1 * 16)) -ne $(($2 * 9)) ]; then
      problem "$file: $1 × $2, needs 9:16 portrait with sides between 320 and 3840"
    fi
  done
done

exit "$failed"

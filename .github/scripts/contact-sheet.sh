#!/bin/sh
# Lays out the store screenshots in a fastlane directory side by side for review, see
# `store-screenshots`: one row per platform and language, five columns, labelled.
# Needs ImageMagick. Usage: contact-sheet.sh <fastlane dir> <out.png>
set -eu

fastlane=$1
out=$2
rows=$(mktemp -d)
trap 'rm -rf "$rows"' EXIT

for locale in en-US de-DE fr-FR nl-NL; do
  for platform in android ios; do
    if [ "$platform" = android ]; then
      directory="$fastlane/metadata/android/$locale/images/phoneScreenshots"
    else
      directory="$fastlane/screenshots/ios/$locale"
    fi
    montage "$directory"/*.png -resize x640 -geometry +6+6 -tile 5x1 \
      -title "$platform $locale" -pointsize 28 "$rows/$locale-$platform.png"
  done
done
montage "$rows"/*.png -geometry +0+12 -tile 1x "$out"

#!/bin/sh
# Prints the entries of the CHANGELOG.md section for a version, see `release-delivery`.
# Fails when there is no such section or it is empty.
#
# Usage: release-notes.sh <version> [changelog]
set -eu

version=$1
changelog=${2:-CHANGELOG.md}

notes=$(awk -v heading="## $version" '
  $0 == heading { inside = 1; next }
  inside && /^## / { exit }
  inside { print }
' "$changelog" | sed '/./,$!d' | sed -e :a -e '/^\n*$/{$d;N;ba' -e '}')

if [ -z "$notes" ]; then
  echo "$changelog has no entries for $version" >&2
  exit 1
fi

printf '%s\n' "$notes"

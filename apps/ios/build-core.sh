#!/bin/sh
# Builds the Kotlin core framework for Xcode (pre-build script in project.yml).
#
# Xcode started from the Dock does not see the shell's environment, so a JDK installed with e.g.
# SDKMAN is not found. Use JAVA_HOME if set, otherwise look in the usual places.
set -e

if [ -z "$JAVA_HOME" ] || [ ! -x "$JAVA_HOME/bin/java" ]; then
  for candidate in \
    "$(/usr/libexec/java_home 2>/dev/null)" \
    "$HOME/.sdkman/candidates/java/current" \
    "/Applications/Android Studio.app/Contents/jbr/Contents/Home"; do
    if [ -n "$candidate" ] && [ -x "$candidate/bin/java" ]; then
      export JAVA_HOME="$candidate"
      break
    fi
  done
fi

if [ -z "$JAVA_HOME" ] || [ ! -x "$JAVA_HOME/bin/java" ]; then
  echo "error: No JDK found. Install one or set JAVA_HOME, see README.md." >&2
  exit 1
fi

cd "$(dirname "$0")/../.."
./gradlew :modules:core:embedAndSignAppleFrameworkForXcode

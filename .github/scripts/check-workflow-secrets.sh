#!/bin/sh
# Checks that pull requests from forks never get signing keys or store credentials, see
# `release-delivery`: GitHub gives them no secrets unless a workflow uses `pull_request_target`,
# and only the release workflows, which pull requests cannot start, use secrets.
#
# Usage: check-workflow-secrets.sh [workflow directory]
set -eu

directory=${1:-.github/workflows}
failed=0

for workflow in "$directory"/*.yml; do
  name=$(basename "$workflow")
  if grep -n 'pull_request_target' "$workflow"; then
    echo "$name: uses pull_request_target, which gives fork pull requests the secrets" >&2
    failed=1
  fi
  case $name in
    release.yml | release-*.yml)
      if grep -nE '^ *(pull_request|push|schedule|workflow_run):' "$workflow"; then
        echo "$name: a release workflow may only be started by hand or by release.yml" >&2
        failed=1
      fi
      ;;
    *)
      if grep -nE '\$\{\{[^}]*secrets\.' "$workflow" | grep -v 'secrets\.GITHUB_TOKEN'; then
        echo "$name: only the release workflows may use secrets other than GITHUB_TOKEN" >&2
        failed=1
      fi
      ;;
  esac
done

exit $failed

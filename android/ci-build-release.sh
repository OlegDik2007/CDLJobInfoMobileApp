#!/usr/bin/env bash
# Build AAB without disabling Android release lint. Retry transient remote Maven
# resolution failures; deterministic compilation or signing errors still fail.
set -euo pipefail

for attempt in 1 2 3; do
  options=()
  if (( attempt > 1 )); then
    options=(--refresh-dependencies)
  fi

  if gradle --no-daemon "${options[@]}" :app:bundleRelease; then
    exit 0
  fi

  if (( attempt == 3 )); then
    echo "::error::Android release AAB build failed after three attempts"
    exit 1
  fi

  echo "::warning::Gradle AAB build failed on attempt ${attempt}; retrying dependency download without disabling lint"
  sleep "$((attempt * 5))"
done

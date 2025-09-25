#!/bin/bash
set -euo pipefail

FUNCTIONALITY_INPUT="${1:-}"
HEAD_COMMIT_MSG="${2:-}"

if [ -z "$FUNCTIONALITY_INPUT" ]; then
  COMMIT_MSG="$HEAD_COMMIT_MSG"
else
  COMMIT_MSG="test($FUNCTIONALITY_INPUT): Manual triggered for CI feedback"
fi

# Defaults
FUNCTIONALITY="**"
SERVICE="ALL"

# Check patterns
if [[ "$COMMIT_MSG" =~ feat\(([a-zA-Z0-9_-]+)(/([a-zA-Z0-9_/-]+))?\) ]]; then
  SERVICE="${BASH_REMATCH[1]}"
  FUNCTIONALITY="${BASH_REMATCH[3]:-**}"
elif [[ "$COMMIT_MSG" =~ test\(([a-zA-Z0-9_-]+)(/([a-zA-Z0-9_/-]+))?\) ]]; then
  SERVICE="${BASH_REMATCH[1]}"
  FUNCTIONALITY="${BASH_REMATCH[3]:-**}"
elif [[ "$COMMIT_MSG" =~ refactor\(([a-zA-Z0-9_-]+)(/([a-zA-Z0-9_/-]+))?\) ]]; then
  SERVICE="${BASH_REMATCH[1]}"
  FUNCTIONALITY="${BASH_REMATCH[3]:-**}"
elif [[ "$COMMIT_MSG" =~ perf\(([a-zA-Z0-9_-]+)(/([a-zA-Z0-9_/-]+))?\) ]]; then
  SERVICE="${BASH_REMATCH[1]}"
  FUNCTIONALITY="${BASH_REMATCH[3]:-**}"
fi

# Ensure non-empty values
SERVICE="${SERVICE:-ALL}"
FUNCTIONALITY="${FUNCTIONALITY:-**}"

# Print both values for caller to capture
echo "$SERVICE $FUNCTIONALITY"

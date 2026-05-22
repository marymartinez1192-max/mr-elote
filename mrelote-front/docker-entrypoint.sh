#!/bin/sh
set -e

# Rewrite hardcoded BASE_URL inside api.js using API_BASE_URL env var.
# Default in source: /api/v1 (served by nginx reverse proxy to backend service).
TARGET="/usr/share/nginx/html/js/api.js"
DEFAULT_URL="/api/v1"
NEW_URL="${API_BASE_URL:-/api/v1}"

if [ -f "$TARGET" ] && [ "$NEW_URL" != "$DEFAULT_URL" ]; then
  ESCAPED=$(printf '%s\n' "$NEW_URL" | sed -e 's/[\/&]/\\&/g')
  sed -i "s|const BASE_URL = '${DEFAULT_URL}';|const BASE_URL = '${ESCAPED}';|g" "$TARGET"
  echo "[entrypoint] BASE_URL set to: $NEW_URL"
fi

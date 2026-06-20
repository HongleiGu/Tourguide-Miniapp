#!/usr/bin/env bash
# Regenerate the shared TypeScript API types from the backend OpenAPI spec.
#
# Usage:
#   tools/gen-api-types.sh                 # fetch from a running dev backend (default URL)
#   tools/gen-api-types.sh <url|file>      # or a custom spec URL / local openapi.json
#
# Outputs (committed, so frontends build without running the backend):
#   frontend/shared/openapi.json   - snapshot of the spec
#   frontend/shared/api.d.ts       - generated TS types (openapi-typescript)
set -euo pipefail

SPEC="${1:-http://localhost:8080/v3/api-docs}"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
OUT_DIR="$ROOT/frontend/shared"
SNAPSHOT="$OUT_DIR/openapi.json"
OUT="$OUT_DIR/api.d.ts"

mkdir -p "$OUT_DIR"

echo "Reading spec from: $SPEC"
if [[ "$SPEC" == http* ]]; then
  curl -fsSL "$SPEC" -o "$SNAPSHOT"
elif [[ "$(realpath "$SPEC")" != "$(realpath "$SNAPSHOT")" ]]; then
  cp "$SPEC" "$SNAPSHOT"
fi

# Pinned major version for reproducible output.
npx --yes openapi-typescript@7 "$SNAPSHOT" -o "$OUT"
echo "Generated: $OUT"

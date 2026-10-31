#!/usr/bin/env bash
# Regenerate Android TV launcher banner + icons (Play TV-LB / TV-BN).
# Usage: ./scripts/gen-tv-assets.sh [--name Arthur] [--skip-playstore]
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
exec python3 "$ROOT/scripts/playstore/gen_tv_launcher_assets.py" "$@"

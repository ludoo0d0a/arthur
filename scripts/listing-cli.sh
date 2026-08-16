#!/usr/bin/env bash
set -euo pipefail
TOOLS="${GK_TOOLS:-/Users/ludovic/dev/android/geoking-tools}"
exec python3 "$TOOLS/playstore-listing/listing_cli.py" "$@"

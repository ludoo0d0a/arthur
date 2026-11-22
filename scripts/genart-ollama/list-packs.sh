#!/usr/bin/env bash
set -euo pipefail
# shellcheck source=common.sh
source "$(cd "$(dirname "$0")" && pwd)/common.sh"

ensure_workdir
python3 "$SCRIPT_DIR/lib/resolve_packs.py" list --work "$WORK_ROOT"

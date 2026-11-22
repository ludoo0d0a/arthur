#!/usr/bin/env bash
set -euo pipefail
# shellcheck source=common.sh
source "$(cd "$(dirname "$0")" && pwd)/common.sh"

ensure_workdir
N="${1:-5}"
APPLY_FLAG=()
if [[ "${2:-}" == "--apply" ]]; then
  APPLY_FLAG=(--apply)
fi

mapfile -t NEXT < <(comm -23 \
  <(sort -u "$WORK_ROOT/queue.txt") \
  <(cat "$WORK_ROOT/done.txt" "$WORK_ROOT/skip.txt" 2>/dev/null | sort -u) \
  | head -n "$N")

if [[ ${#NEXT[@]} -eq 0 ]]; then
  echo "Queue empty / all done. Rebuild with: ./scripts/genart-ollama/build-queue.sh --pack weather"
  exit 0
fi

for eng in "${NEXT[@]}"; do
  echo "======== BATCH $eng ========"
  "$SCRIPT_DIR/run-one.sh" "${APPLY_FLAG[@]}" "$eng" || {
    echo "warn: run-one failed for $eng — continuing" >&2
  }
done

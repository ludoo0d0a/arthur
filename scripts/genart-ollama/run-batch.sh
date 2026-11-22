#!/usr/bin/env bash
set -euo pipefail
# shellcheck source=common.sh
source "$(cd "$(dirname "$0")" && pwd)/common.sh"

ensure_workdir

APPLY_FLAG=()
MODE=continuous   # continuous | once | pack-once | count
COUNT=0
PACK_FORCE=""

usage() {
  cat <<'EOF'
Usage: run-batch.sh [options]

Default: run continuously — resume the last incomplete pack, process engines,
         when the pack is finished advance to the next pack, until all done.

Options:
  --list           list packs progress and exit
  --once           process one engine then stop
  --pack-once      finish the current pack then stop
  --pack NAME      start/force this pack (then continuous unless --once/--pack-once)
  -n N / N         process at most N engines then stop
  --apply          pass --apply to run-one.sh
  -h / --help      this help

Examples:
  ./scripts/genart-ollama/run-batch.sh
  ./scripts/genart-ollama/run-batch.sh --list
  ./scripts/genart-ollama/run-batch.sh --once
  ./scripts/genart-ollama/run-batch.sh --pack-once --apply
  ./scripts/genart-ollama/run-batch.sh --pack nature -n 3
EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --list) resolve_packs list; exit 0 ;;
    --once) MODE=once; shift ;;
    --pack-once) MODE=pack-once; shift ;;
    --pack) PACK_FORCE="${2:-}"; shift 2 ;;
    --apply) APPLY_FLAG=(--apply); shift ;;
    -n) COUNT="${2:-}"; MODE=count; shift 2 ;;
    -h|--help) usage; exit 0 ;;
    [0-9]*) COUNT="$1"; MODE=count; shift ;;
    *) echo "Unknown arg: $1"; usage; exit 1 ;;
  esac
done

processed=0
start_pack=""

read_next_into_queue() {
  # Parses resolve_packs next → CURRENT_PACK + QUEUE_ENGINES
  local line first=1
  local pack=""
  QUEUE_ENGINES=()
  while IFS= read -r line || [[ -n "$line" ]]; do
    if [[ "$first" -eq 1 ]]; then
      pack="$line"
      first=0
      continue
    fi
    case "$line" in
      engine:*) QUEUE_ENGINES+=("${line#engine:}") ;;
    esac
  done < <(resolve_packs next)
  CURRENT_PACK="$pack"
  if [[ -n "$CURRENT_PACK" ]]; then
    printf '%s\n' "$CURRENT_PACK" > "$WORK_ROOT/current_pack.txt"
    if [[ ${#QUEUE_ENGINES[@]} -gt 0 ]]; then
      printf '%s\n' "${QUEUE_ENGINES[@]}" > "$WORK_ROOT/queue.txt"
    else
      : > "$WORK_ROOT/queue.txt"
    fi
  fi
}

activate_pack() {
  if [[ -n "$PACK_FORCE" ]]; then
    "$SCRIPT_DIR/build-queue.sh" --pack "$PACK_FORCE"
    PACK_FORCE=""
    CURRENT_PACK="$(cat "$WORK_ROOT/current_pack.txt")"
    QUEUE_ENGINES=()
    while IFS= read -r line || [[ -n "$line" ]]; do
      [[ -z "$line" ]] && continue
      QUEUE_ENGINES+=("$line")
    done < "$WORK_ROOT/queue.txt"
  else
    if ! load_next_pack; then
      echo "All packs optimized. Done."
      resolve_packs list
      return 1
    fi
  fi
  start_pack="$CURRENT_PACK"
  echo "======== PACK $CURRENT_PACK (${#QUEUE_ENGINES[@]} remaining) ========"
  return 0
}

if ! activate_pack; then
  exit 0
fi

prev_pack="$CURRENT_PACK"

while true; do
  read_next_into_queue
  if [[ -z "${CURRENT_PACK:-}" ]]; then
    mark_pack_done_if_complete "$prev_pack"
    echo "All packs optimized. Done."
    resolve_packs list
    exit 0
  fi

  if [[ "$CURRENT_PACK" != "$prev_pack" ]]; then
    mark_pack_done_if_complete "$prev_pack"
    if [[ "$MODE" == "pack-once" && -n "$start_pack" && "$prev_pack" == "$start_pack" ]]; then
      echo "Pack $start_pack finished — stopping (--pack-once)."
      resolve_packs list
      exit 0
    fi
    echo "======== PACK $CURRENT_PACK (${#QUEUE_ENGINES[@]} remaining) ========"
    prev_pack="$CURRENT_PACK"
  fi

  if [[ ${#QUEUE_ENGINES[@]} -eq 0 ]]; then
    mark_pack_done_if_complete "$CURRENT_PACK"
    if ! load_next_pack; then
      echo "All packs optimized. Done."
      resolve_packs list
      exit 0
    fi
    prev_pack="$CURRENT_PACK"
    echo "======== PACK $CURRENT_PACK (${#QUEUE_ENGINES[@]} remaining) ========"
    continue
  fi

  eng="${QUEUE_ENGINES[0]}"
  echo "======== [$CURRENT_PACK] $eng ========"
  "$SCRIPT_DIR/run-one.sh" "${APPLY_FLAG[@]}" "$eng" || {
    echo "warn: run-one failed for $eng — continuing" >&2
  }
  processed=$((processed + 1))
  mark_pack_done_if_complete "$CURRENT_PACK"

  case "$MODE" in
    once)
      echo "Stopped after 1 engine (--once). processed=$processed"
      exit 0
      ;;
    count)
      if [[ "$processed" -ge "$COUNT" ]]; then
        echo "Stopped after $processed engines (-n $COUNT)."
        exit 0
      fi
      ;;
  esac
done

#!/usr/bin/env bash
set -euo pipefail
# shellcheck source=common.sh
source "$(cd "$(dirname "$0")" && pwd)/common.sh"

ensure_workdir

# Default: APPLY — local LLM patches Engine/Still into the repo for real quality upgrades.
APPLY=1
MODE=continuous   # continuous | once | pack-once | count
COUNT=0
PACK_FORCE=""

usage() {
  cat <<'EOF'
Usage: run-batch.sh [options]

Optimizes genart Engine+Still quality via local Ollama models, for every pack.

Default: --apply continuous — resume incomplete pack, LLM-fix each engine into the
         repo, bake after/compare, advance to next pack until all done.

Options:
  --list           list packs progress and exit
  --once           process one engine then stop
  --pack-once      finish the current pack then stop
  --pack NAME      start/force this pack
  -n N / N         process at most N engines then stop
  --apply          (default) write LLM Kotlin into the repo
  --no-apply       audit/fix only — leave suggestions in work/<Engine>/
  -h / --help      this help

Examples:
  ./scripts/genart-ollama/run-batch.sh
  ./scripts/genart-ollama/run-batch.sh --list
  ./scripts/genart-ollama/run-batch.sh --once
  ./scripts/genart-ollama/run-batch.sh --pack-once
  ./scripts/genart-ollama/run-batch.sh --pack nature -n 3
  ./scripts/genart-ollama/run-batch.sh --no-apply --once
EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --list) resolve_packs list; exit 0 ;;
    --once) MODE=once; shift ;;
    --pack-once) MODE=pack-once; shift ;;
    --pack) PACK_FORCE="${2:-}"; shift 2 ;;
    --apply) APPLY=1; shift ;;
    --no-apply) APPLY=0; shift ;;
    -n) COUNT="${2:-}"; MODE=count; shift 2 ;;
    -h|--help) usage; exit 0 ;;
    [0-9]*) COUNT="$1"; MODE=count; shift ;;
    *) echo "Unknown arg: $1"; usage; exit 1 ;;
  esac
done

processed=0
start_pack=""

read_next_into_queue() {
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
  echo "======== PACK $CURRENT_PACK (${#QUEUE_ENGINES[@]} remaining) apply=$APPLY ========"
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
    echo "======== PACK $CURRENT_PACK (${#QUEUE_ENGINES[@]} remaining) apply=$APPLY ========"
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
    echo "======== PACK $CURRENT_PACK (${#QUEUE_ENGINES[@]} remaining) apply=$APPLY ========"
    continue
  fi

  eng="${QUEUE_ENGINES[0]}"
  echo "======== [$CURRENT_PACK] optimize $eng (LLM local) ========"
  if [[ "$APPLY" -eq 1 ]]; then
    "$SCRIPT_DIR/run-one.sh" --apply "$eng" || {
      echo "warn: run-one failed for $eng — continuing" >&2
    }
  else
    "$SCRIPT_DIR/run-one.sh" "$eng" || {
      echo "warn: run-one failed for $eng — continuing" >&2
    }
  fi
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

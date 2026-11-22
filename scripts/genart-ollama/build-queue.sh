#!/usr/bin/env bash
set -euo pipefail
# shellcheck source=common.sh
source "$(cd "$(dirname "$0")" && pwd)/common.sh"

ensure_workdir

PACK=""
RESUME=0
while [[ $# -gt 0 ]]; do
  case "$1" in
    --pack) PACK="${2:-}"; shift 2 ;;
    --resume) RESUME=1; shift ;;
    --list)
      resolve_packs list
      exit 0
      ;;
    -h|--help)
      cat <<'EOF'
Usage: build-queue.sh [--resume | --pack NAME | --list]

  --resume   (default if no --pack) queue = remaining engines of next incomplete pack
  --pack X   queue = all engines in pack X (from GenartPackTopics)
  --list     show pack progress and exit

Pack order and membership come from GenartPackTopics.categoryTopicSuffixes.
EOF
      exit 0
      ;;
    *) echo "Unknown arg: $1 (try --help)"; exit 1 ;;
  esac
done

if [[ -z "$PACK" ]]; then
  RESUME=1
fi

if [[ "$RESUME" -eq 1 ]]; then
  if ! load_next_pack; then
    echo "All packs complete (nothing to queue)."
    resolve_packs list
    exit 0
  fi
  echo "Resume pack=$CURRENT_PACK (${#QUEUE_ENGINES[@]} remaining) → $WORK_ROOT/queue.txt"
  printf '  %s\n' "${QUEUE_ENGINES[@]}"
  exit 0
fi

# Explicit --pack
QUEUE_ENGINES=()
while IFS= read -r line || [[ -n "$line" ]]; do
  [[ -z "$line" ]] && continue
  QUEUE_ENGINES+=("$line")
done < <(resolve_packs engines --pack "$PACK")

if [[ ${#QUEUE_ENGINES[@]} -eq 0 ]]; then
  echo "No engines for pack=$PACK (unknown or empty)" >&2
  exit 1
fi
CURRENT_PACK="$PACK"
printf '%s\n' "$CURRENT_PACK" > "$WORK_ROOT/current_pack.txt"
printf '%s\n' "${QUEUE_ENGINES[@]}" > "$WORK_ROOT/queue.txt"
echo "Wrote ${#QUEUE_ENGINES[@]} engines → $WORK_ROOT/queue.txt (pack=$PACK)"
printf '  %s\n' "${QUEUE_ENGINES[@]}"

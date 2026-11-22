#!/usr/bin/env bash
set -euo pipefail
# shellcheck source=common.sh
source "$(cd "$(dirname "$0")" && pwd)/common.sh"

ensure_workdir

# Default: apply LLM patches into the repo (quality optimization path).
APPLY=1
REDESIGN=0
SKIP_BAKE=0
ENGINE=""
while [[ $# -gt 0 ]]; do
  case "$1" in
    --apply) APPLY=1; shift ;;
    --no-apply) APPLY=0; shift ;;
    --redesign) REDESIGN=1; shift ;;
    --skip-bake) SKIP_BAKE=1; shift ;;
    -*) echo "Unknown flag $1"; exit 1 ;;
    *) ENGINE="$1"; shift ;;
  esac
done
[[ -n "$ENGINE" ]] || {
  echo "Usage: run-one.sh [--no-apply] [--redesign] [--skip-bake] <Engine>"
  exit 1
}

ENG_PATH="$ARTHUR_ROOT/genart/src/main/kotlin/fr/geoking/arthur/genart/engines/${ENGINE}Engine.kt"
STILL_PATH="$ARTHUR_ROOT/genart/src/main/kotlin/fr/geoking/arthur/genart/stills/${ENGINE}Still.kt"
[[ -f "$ENG_PATH" && -f "$STILL_PATH" ]] || {
  echo "Missing Engine/Still for $ENGINE"
  exit 1
}

DIR="$WORK_ROOT/work/$ENGINE"
mkdir -p "$DIR/out"
cp "$ENG_PATH" "$STILL_PATH" "$DIR/"

slug="$(engine_to_slug "$ENGINE")"
STILL_PNG="$PREVIEWS/before/stills/${slug}.png"
VIDEO_MP4="$PREVIEWS/before/videos/${slug}.mp4"

if [[ "$SKIP_BAKE" -eq 0 ]]; then
  "$SCRIPT_DIR/bake-previews.sh" before "$ENGINE" || true
fi

fill_prompt() {
  local template="$1"
  local audit_file="${2:-}"
  python3 "$SCRIPT_DIR/lib/fill_prompt.py" \
    "$template" \
    "$ENGINE" \
    "$STILL_PNG" \
    "$VIDEO_MP4" \
    "$ENG_PATH" \
    "$STILL_PATH" \
    "$audit_file"
}

require_cmd ollama

echo "=== AUDIT $ENGINE ==="
PROMPT_AUDIT="$(fill_prompt "$SCRIPT_DIR/prompts/01-audit.md")"
ollama run arthur-audit "$PROMPT_AUDIT" | tee "$DIR/audit.md"

DECISION="$(grep -Eo 'KEEP_LIGHT_FIX|REDESIGN|SKIP' "$DIR/audit.md" | tail -n1 || true)"
if [[ "$DECISION" == "SKIP" && "$REDESIGN" -eq 0 ]]; then
  echo "$ENGINE" >> "$WORK_ROOT/skip.txt"
  echo "SKIP - recorded (already good enough)"
  exit 0
fi

if [[ "$REDESIGN" -eq 1 || "$DECISION" == "REDESIGN" ]]; then
  echo "=== REDESIGN $ENGINE (arthur-heavy) ==="
  PROMPT="$(fill_prompt "$SCRIPT_DIR/prompts/04-redesign.md" "$DIR/audit.md")"
  ollama run arthur-heavy "$PROMPT" | tee "$DIR/redesign.md"
else
  echo "=== FIX $ENGINE (arthur-fix) - quality upgrade ==="
  PROMPT="$(fill_prompt "$SCRIPT_DIR/prompts/02-fix.md" "$DIR/audit.md")"
  ollama run arthur-fix "$PROMPT" | tee "$DIR/fix-v1.md"

  echo "=== MICRO $ENGINE (arthur-fast) ==="
  PROMPT="$(fill_prompt "$SCRIPT_DIR/prompts/03-micro.md" "")"
  ollama run arthur-fast "$PROMPT" | tee "$DIR/fix-v2.md"
fi

# Extract kotlin fences into out/
EXTRACT_OK=0
python3 "$SCRIPT_DIR/lib/extract_kotlin.py" "$DIR" "$ENGINE" || EXTRACT_OK=$?

if [[ "$APPLY" -eq 1 ]]; then
  if [[ "$EXTRACT_OK" -eq 0 && -f "$DIR/out/${ENGINE}Engine.kt" && -f "$DIR/out/${ENGINE}Still.kt" ]]; then
    cp "$DIR/out/${ENGINE}Engine.kt" "$ENG_PATH"
    cp "$DIR/out/${ENGINE}Still.kt" "$STILL_PATH"
    echo "Applied LLM patches -> repo ($ENGINE)"
    if [[ "$SKIP_BAKE" -eq 0 ]]; then
      SKIP_GENART_STILLS=1 "$SCRIPT_DIR/bake-previews.sh" after "$ENGINE" || true
      "$SCRIPT_DIR/compare-previews.sh" "$ENGINE" || true
    fi
  else
    echo "warn: apply requested but Kotlin extract incomplete - NOT marking done (will retry)" >&2
    echo "$ENGINE" >> "$WORK_ROOT/failed.txt"
    exit 1
  fi
fi

echo "$ENGINE" >> "$WORK_ROOT/done.txt"
echo "OK - optimized $ENGINE (apply=$APPLY). Review $DIR"

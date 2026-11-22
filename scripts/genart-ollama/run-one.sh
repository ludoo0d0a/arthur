#!/usr/bin/env bash
set -euo pipefail
# shellcheck source=common.sh
source "$(cd "$(dirname "$0")" && pwd)/common.sh"

ensure_workdir

APPLY=0
REDESIGN=0
SKIP_BAKE=0
ENGINE=""
while [[ $# -gt 0 ]]; do
  case "$1" in
    --apply) APPLY=1; shift ;;
    --redesign) REDESIGN=1; shift ;;
    --skip-bake) SKIP_BAKE=1; shift ;;
    -*) echo "Unknown flag $1"; exit 1 ;;
    *) ENGINE="$1"; shift ;;
  esac
done
[[ -n "$ENGINE" ]] || { echo "Usage: run-one.sh [--apply] [--redesign] [--skip-bake] <Engine>"; exit 1; }

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
  local audit_body="${2:-}"
  sed \
    -e "s|{{ENGINE}}|$ENGINE|g" \
    -e "s|{{STILL_PNG}}|${STILL_PNG}|g" \
    -e "s|{{VIDEO_MP4}}|${VIDEO_MP4}|g" \
    "$template" \
    | awk -v eng="$(cat "$ENG_PATH")" -v still="$(cat "$STILL_PATH")" -v audit="$audit_body" '
      {
        line=$0
        if (index(line, "{{ENGINE_SRC}}")) {
          split(line, a, "{{ENGINE_SRC}}")
          printf "%s%s%s", a[1], eng, a[2]
          next
        }
        if (index(line, "{{STILL_SRC}}")) {
          split(line, a, "{{STILL_SRC}}")
          printf "%s%s%s", a[1], still, a[2]
          next
        }
        if (index(line, "{{AUDIT}}")) {
          split(line, a, "{{AUDIT}}")
          printf "%s%s%s", a[1], audit, a[2]
          next
        }
        print line
      }'
}

require_cmd ollama

echo "=== AUDIT $ENGINE ==="
PROMPT_AUDIT="$(fill_prompt "$SCRIPT_DIR/prompts/01-audit.md")"
ollama run arthur-audit "$PROMPT_AUDIT" | tee "$DIR/audit.md"

DECISION="$(grep -Eo 'KEEP_LIGHT_FIX|REDESIGN|SKIP' "$DIR/audit.md" | tail -n1 || true)"
if [[ "$DECISION" == "SKIP" && "$REDESIGN" -eq 0 ]]; then
  echo "$ENGINE" >> "$WORK_ROOT/skip.txt"
  echo "SKIP — recorded"
  exit 0
fi

if [[ "$REDESIGN" -eq 1 || "$DECISION" == "REDESIGN" ]]; then
  echo "=== REDESIGN $ENGINE ==="
  PROMPT="$(fill_prompt "$SCRIPT_DIR/prompts/04-redesign.md" "$(cat "$DIR/audit.md")")"
  ollama run arthur-heavy "$PROMPT" | tee "$DIR/redesign.md"
  SRC_MD="$DIR/redesign.md"
else
  echo "=== FIX $ENGINE ==="
  PROMPT="$(fill_prompt "$SCRIPT_DIR/prompts/02-fix.md" "$(cat "$DIR/audit.md")")"
  ollama run arthur-fix "$PROMPT" | tee "$DIR/fix-v1.md"

  echo "=== MICRO $ENGINE ==="
  # Prefer fix-v1 bodies as context by temporarily pointing paths — micro uses repo sources;
  # enough for seam/quality pass on originals; human merges fix-v1.
  PROMPT="$(fill_prompt "$SCRIPT_DIR/prompts/03-micro.md")"
  ollama run arthur-fast "$PROMPT" | tee "$DIR/fix-v2.md"
  SRC_MD="$DIR/fix-v1.md"
fi

# Extract kotlin fences into out/ (best-effort)
python3 - "$SRC_MD" "$DIR/out" "$ENGINE" <<'PY'
import re, sys
from pathlib import Path
md = Path(sys.argv[1]).read_text(encoding="utf-8", errors="replace")
out = Path(sys.argv[2])
engine = sys.argv[3]
blocks = re.findall(r"```(?:kotlin|kt)?\n(.*?)```", md, flags=re.S | re.I)
eng_file = still_file = None
for b in blocks:
    if f"{engine}Engine" in b or "fun " + engine in b or f"internal fun {engine}Engine" in b:
        eng_file = b.strip() + "\n"
    if f"{engine}Still" in b or f"object {engine}Still" in b:
        still_file = b.strip() + "\n"
# Heuristic: first large block engine, second still
if eng_file is None and blocks:
    eng_file = blocks[0].strip() + "\n"
if still_file is None and len(blocks) > 1:
    still_file = blocks[1].strip() + "\n"
if eng_file:
    (out / f"{engine}Engine.kt").write_text(eng_file, encoding="utf-8")
if still_file:
    (out / f"{engine}Still.kt").write_text(still_file, encoding="utf-8")
print(f"extracted engine={bool(eng_file)} still={bool(still_file)} → {out}")
PY

if [[ "$APPLY" -eq 1 ]]; then
  if [[ -f "$DIR/out/${ENGINE}Engine.kt" && -f "$DIR/out/${ENGINE}Still.kt" ]]; then
    cp "$DIR/out/${ENGINE}Engine.kt" "$ENG_PATH"
    cp "$DIR/out/${ENGINE}Still.kt" "$STILL_PATH"
    echo "Applied out/ → repo"
    if [[ "$SKIP_BAKE" -eq 0 ]]; then
      "$SCRIPT_DIR/bake-previews.sh" after "$ENGINE" || true
      "$SCRIPT_DIR/compare-previews.sh" "$ENGINE" || true
    fi
  else
    echo "warn: --apply but out/ incomplete; skip apply" >&2
  fi
fi

echo "$ENGINE" >> "$WORK_ROOT/done.txt"
echo "OK — review $DIR (and previews under $PREVIEWS)"

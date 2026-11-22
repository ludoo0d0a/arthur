#!/usr/bin/env bash
# Shared paths for Arthur genart-ollama kit.
set -euo pipefail

_GENART_OLLAMA_SELF="${BASH_SOURCE[0]:-$0}"
SCRIPT_DIR="$(cd "$(dirname "$_GENART_OLLAMA_SELF")" && pwd)"
unset _GENART_OLLAMA_SELF
ARTHUR_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
export ARTHUR_ROOT
export SCRIPT_DIR

WORK_ROOT="${ARTHUR_GENART_OLLAMA_HOME:-$HOME/arthur-genart-ollama}"
export WORK_ROOT

PREVIEWS="$WORK_ROOT/previews"
FRAMES_GRADLE_OUT="$ARTHUR_ROOT/screenshots/genart-frames"
STILLS_GRADLE_OUT="$ARTHUR_ROOT/screenshots"

engine_to_slug() {
  local name="$1"
  # Aurora -> aurora ; genart.aurora -> aurora
  echo "$name" | sed -E 's/^genart\.//; s/Engine$//; s/Still$//' | tr '[:upper:]' '[:lower:]'
}

ensure_workdir() {
  mkdir -p \
    "$WORK_ROOT/work" \
    "$PREVIEWS/before/stills" \
    "$PREVIEWS/before/videos" \
    "$PREVIEWS/before/frames" \
    "$PREVIEWS/after/stills" \
    "$PREVIEWS/after/videos" \
    "$PREVIEWS/after/frames" \
    "$PREVIEWS/compare"
  touch "$WORK_ROOT/queue.txt" "$WORK_ROOT/done.txt" "$WORK_ROOT/skip.txt"
}

require_cmd() {
  command -v "$1" >/dev/null 2>&1 || {
    echo "Missing required command: $1" >&2
    exit 1
  }
}

# Map PascalCase engine name to catalog still filename pattern via registry titles is hard in bash;
# stills are #N-Name.png — we copy by matching slug inside Name (case-insensitive) when possible.
copy_still_for_engine() {
  local stage="$1"
  local engine="$2"
  local slug
  slug="$(engine_to_slug "$engine")"
  local dest_dir="$PREVIEWS/$stage/stills"
  mkdir -p "$dest_dir"
  local match=""
  local slugflat
  slugflat="$(echo "$slug" | tr -d ' -_')"
  if [[ -f "$STILLS_GRADLE_OUT/${slug}.png" ]]; then
    match="$STILLS_GRADLE_OUT/${slug}.png"
  else
    # Match #N-Title.png when flattened title starts or ends with slug
    # (AuroraRibbons, SoftRain, FallingSnow). Avoids rain ⊂ terrain.
    local f titleflat
    for f in "$STILLS_GRADLE_OUT"/#*.png; do
      [[ -e "$f" ]] || continue
      titleflat="$(basename "$f" .png | sed -E 's/^#[0-9]+-//' | tr '[:upper:]' '[:lower:]' | tr -d ' -_')"
      if [[ "$titleflat" == "$slugflat"* || "${titleflat%"$slugflat"}" != "$titleflat" ]]; then
        match="$f"
        break
      fi
    done
  fi
  if [[ -n "$match" ]]; then
    cp "$match" "$dest_dir/${slug}.png"
    echo "still: $match → $dest_dir/${slug}.png"
  else
    echo "warn: no still PNG found for $engine (slug=$slug); run generateGenartScreenshots" >&2
  fi
}

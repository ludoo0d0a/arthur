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
  touch "$WORK_ROOT/queue.txt" "$WORK_ROOT/done.txt" "$WORK_ROOT/skip.txt" \
    "$WORK_ROOT/packs_done.txt" "$WORK_ROOT/current_pack.txt" "$WORK_ROOT/failed.txt"
}

resolve_packs() {
  python3 "$SCRIPT_DIR/lib/resolve_packs.py" "$@" --work "$WORK_ROOT"
}

# Load next pack + remaining engines into CURRENT_PACK / QUEUE_ENGINES.
load_next_pack() {
  local pack="" line first=1
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

  if [[ -z "$pack" ]]; then
    CURRENT_PACK=""
    return 1
  fi
  CURRENT_PACK="$pack"
  printf '%s\n' "$CURRENT_PACK" > "$WORK_ROOT/current_pack.txt"
  if [[ ${#QUEUE_ENGINES[@]} -gt 0 ]]; then
    printf '%s\n' "${QUEUE_ENGINES[@]}" > "$WORK_ROOT/queue.txt"
  else
    : > "$WORK_ROOT/queue.txt"
  fi
  return 0
}

mark_pack_done_if_complete() {
  local pack="${1:-}"
  [[ -n "$pack" ]] || return 0
  local e pending=0 total=0
  while IFS= read -r e || [[ -n "$e" ]]; do
    [[ -z "$e" ]] && continue
    total=$((total + 1))
    if ! grep -qxF "$e" "$WORK_ROOT/done.txt" 2>/dev/null \
      && ! grep -qxF "$e" "$WORK_ROOT/skip.txt" 2>/dev/null; then
      pending=$((pending + 1))
    fi
  done < <(resolve_packs engines --pack "$pack")
  if [[ "$total" -gt 0 && "$pending" -eq 0 ]]; then
    if ! grep -qxF "$pack" "$WORK_ROOT/packs_done.txt" 2>/dev/null; then
      echo "$pack" >> "$WORK_ROOT/packs_done.txt"
      echo "pack done: $pack"
    fi
  fi
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

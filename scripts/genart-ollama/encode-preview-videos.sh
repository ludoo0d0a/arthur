#!/usr/bin/env bash
set -euo pipefail
# shellcheck source=common.sh
source "$(cd "$(dirname "$0")" && pwd)/common.sh"

require_cmd ffmpeg
ensure_workdir

STAGE="${1:?Usage: encode-preview-videos.sh <before|after> [Engine...]}"
shift || true
FPS="${GENART_VIDEO_FPS:-12}"

encode_slug() {
  local slug="$1"
  local stage="$2"
  local src="$PREVIEWS/$stage/frames/$slug"
  if [[ ! -d "$src" ]]; then
    # fall back to gradle output
    src="$FRAMES_GRADLE_OUT/frames/$slug"
  fi
  if [[ ! -d "$src" ]] || ! ls "$src"/frame_*.png >/dev/null 2>&1; then
    echo "warn: no frames for $slug under $src" >&2
    return 0
  fi
  local dest_dir="$PREVIEWS/$stage/videos"
  mkdir -p "$dest_dir"
  local dest="$dest_dir/${slug}.mp4"
  ffmpeg -y -loglevel error -framerate "$FPS" -i "$src/frame_%03d.png" \
    -c:v libx264 -pix_fmt yuv420p -movflags +faststart "$dest"
  echo "video: $dest"
}

if [[ $# -eq 0 ]]; then
  # encode all frame dirs present for stage (copied) or gradle out
  shopt -s nullglob
  for d in "$PREVIEWS/$STAGE/frames"/*/ "$FRAMES_GRADLE_OUT/frames"/*/; do
    [[ -d "$d" ]] || continue
    slug="$(basename "$d")"
    encode_slug "$slug" "$STAGE"
  done
else
  for eng in "$@"; do
    encode_slug "$(engine_to_slug "$eng")" "$STAGE"
  done
fi

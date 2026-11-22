#!/usr/bin/env bash
set -euo pipefail
# shellcheck source=common.sh
source "$(cd "$(dirname "$0")" && pwd)/common.sh"

require_cmd ffmpeg
ensure_workdir

ENGINES=("$@")
if [[ ${#ENGINES[@]} -eq 0 ]]; then
  ENGINES=()
  for f in "$PREVIEWS/before/videos"/*.mp4; do
    [[ -e "$f" ]] || continue
    ENGINES+=("$(basename "$f" .mp4)")
  done
fi

for eng in "${ENGINES[@]}"; do
  slug="$(engine_to_slug "$eng")"
  a="$PREVIEWS/before/videos/${slug}.mp4"
  b="$PREVIEWS/after/videos/${slug}.mp4"
  if [[ ! -f "$a" || ! -f "$b" ]]; then
    echo "warn: missing before/after video for $slug" >&2
    continue
  fi
  dest="$PREVIEWS/compare/${slug}.mp4"
  ffmpeg -y -loglevel error -i "$a" -i "$b" \
    -filter_complex "[0:v][1:v]hstack=inputs=2" \
    -c:v libx264 -pix_fmt yuv420p -movflags +faststart "$dest"
  echo "compare: $dest"
done

#!/usr/bin/env bash
set -euo pipefail
# shellcheck source=common.sh
source "$(cd "$(dirname "$0")" && pwd)/common.sh"

ensure_workdir
require_cmd ffmpeg

STAGE="${1:?Usage: bake-previews.sh <before|after> [Engine...]}"
shift || true
case "$STAGE" in before|after) ;; *) echo "stage must be before|after"; exit 1 ;; esac

ENGINES=("$@")
if [[ ${#ENGINES[@]} -eq 0 ]]; then
  # default: next unfinished from queue (up to 8) or all weather if empty done
  mapfile -t ENGINES < <(comm -23 <(sort -u "$WORK_ROOT/queue.txt") <(sort -u "$WORK_ROOT/done.txt" "$WORK_ROOT/skip.txt" 2>/dev/null | sort -u) | head -n 8)
fi
if [[ ${#ENGINES[@]} -eq 0 ]]; then
  echo "No engines specified / queued." >&2
  exit 1
fi

SLUGS=()
for e in "${ENGINES[@]}"; do
  SLUGS+=("$(engine_to_slug "$e")")
done
ENGINE_CSV="$(IFS=,; echo "${SLUGS[*]}")"

SIZE="${GENART_VIDEO_SIZE:-720}"
FPS="${GENART_VIDEO_FPS:-12}"
SECS="${GENART_VIDEO_SECONDS:-4}"

echo "Baking stills (catalog)..."
if [[ "${SKIP_GENART_STILLS:-0}" != "1" ]]; then
  (
    cd "$ARTHUR_ROOT"
    ./gradlew :androidApp:generateGenartScreenshots \
      -PgenartScreenshotSize="$SIZE" \
      --quiet
  )
else
  echo "(skipped stills: SKIP_GENART_STILLS=1)"
fi

echo "Baking frames for: ${ENGINE_CSV} ..."
(
  cd "$ARTHUR_ROOT"
  ./gradlew :androidApp:generateGenartPreviewFrames \
    -PgenartVideoSize="$SIZE" \
    -PgenartVideoFps="$FPS" \
    -PgenartVideoSeconds="$SECS" \
    -PgenartVideoEngines="${ENGINE_CSV}" \
    --quiet
)

for slug in "${SLUGS[@]}"; do
  mkdir -p "$PREVIEWS/$STAGE/frames/$slug"
  if [[ -d "$FRAMES_GRADLE_OUT/frames/$slug" ]]; then
    rm -rf "$PREVIEWS/$STAGE/frames/$slug"
    cp -R "$FRAMES_GRADLE_OUT/frames/$slug" "$PREVIEWS/$STAGE/frames/$slug"
  fi
done

for e in "${ENGINES[@]}"; do
  copy_still_for_engine "$STAGE" "$e"
done

"$SCRIPT_DIR/encode-preview-videos.sh" "$STAGE" "${ENGINES[@]}"

echo "Done bake → $PREVIEWS/$STAGE"

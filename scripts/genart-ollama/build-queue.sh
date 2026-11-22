#!/usr/bin/env bash
set -euo pipefail
# shellcheck source=common.sh
source "$(cd "$(dirname "$0")" && pwd)/common.sh"

ensure_workdir

PACK=""
while [[ $# -gt 0 ]]; do
  case "$1" in
    --pack) PACK="${2:-}"; shift 2 ;;
    *) echo "Usage: build-queue.sh [--pack weather|nature|water|all]"; exit 1 ;;
  esac
done

STILLS_DIR="$ARTHUR_ROOT/genart/src/main/kotlin/fr/geoking/arthur/genart/stills"
QUEUE="$WORK_ROOT/queue.txt"

# Pack → still filename stem (PascalCase) substrings / exact names derived from GenartPackTopics.
weather_match() {
  case "$1" in
    Snow|Aurora|Clouds|Rain|Fog|Sunbeams|Storm|LightDrizzle|Rainbow|Smog|Smoke|HeatHaze|Sunshine|SteamCurl|RainOnGlass|PaneReflections|WaterfallMist|SoftWindStreaks|DayNightWash|FrostCrystals|EclipseCorona|Fire|AuroraWash|Fireworks) return 0 ;;
    *) return 1 ;;
  esac
}

nature_match() {
  case "$1" in
    Grass|PondRipples|FallingLeaves|Bubbles|Waves|Lake|Fields|Reeds|Rivers|FlowRibbons|MultiFlames|AirBubbles|CherryBlossoms|Fireflies|FishSchool|DriftingPollen|BirdFlock|Mountains|Dunes|Tree|Reeds) return 0 ;;
    *) return 1 ;;
  esac
}

water_match() {
  case "$1" in
    PondRipples|Waves|Lake|Rivers|Bubbles|AirBubbles|MoonlightRipples|OceanSwell|InkInWater|SoapFilm|MarbleCaustics|SoftCaustics|RainOnGlass|WaterfallMist) return 0 ;;
    *) return 1 ;;
  esac
}

: > "$QUEUE"
count=0
for f in "$STILLS_DIR"/*Still.kt; do
  [[ -e "$f" ]] || continue
  base="$(basename "$f" Still.kt)"
  include=1
  if [[ -n "$PACK" && "$PACK" != "all" ]]; then
    include=0
    case "$PACK" in
      weather) weather_match "$base" && include=1 ;;
      nature) nature_match "$base" && include=1 ;;
      water) water_match "$base" && include=1 ;;
      *) echo "Unknown pack: $PACK" >&2; exit 1 ;;
    esac
  fi
  if [[ "$include" -eq 1 ]]; then
    echo "$base" >> "$QUEUE"
    count=$((count + 1))
  fi
done

sort -u -o "$QUEUE" "$QUEUE"
echo "Wrote $count engines → $QUEUE (pack=${PACK:-all})"

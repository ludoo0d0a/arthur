#!/usr/bin/env bash
set -euo pipefail
# genart-upgrade skill — full upgrade pipeline for one engine.
# Usage: upgrade-one.sh <EngineName>
#
# Steps:
#   1. Bake "before" artifacts (still + video)
#   2. (Agent improves Engine + Still Kotlin files)
#   3. Bake "after" artifacts
#   4. Generate side-by-side comparison video
#   5. Regenerate preview icon (1080px screenshot)
#
# Requires: ffmpeg, Android build tools

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
SKILL_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
ARTHUR_ROOT="$(cd "$SKILL_ROOT/../../.." && pwd)"

ENGINE="${1:?Usage: upgrade-one.sh <EngineName>}"
GENART_OLLAMA_DIR="$ARTHUR_ROOT/scripts/genart-ollama"

# Check prereqs
command -v ffmpeg >/dev/null 2>&1 || { echo "Missing: ffmpeg"; exit 1; }
[[ -f "$GENART_OLLAMA_DIR/common.sh" ]] || { echo "Missing: scripts/genart-ollama/"; exit 1; }

echo "════════════════════════════════════════"
echo "  GENART UPGRADE: $ENGINE"
echo "════════════════════════════════════════"

# Step 1: Bake BEFORE
echo ""
echo "▸ Step 1: Baking BEFORE artifacts..."
"$GENART_OLLAMA_DIR/bake-previews.sh" before "$ENGINE" || {
    echo "warn: before bake had errors (may be first run)" >&2
}

echo ""
echo "▸ Step 2: IMPROVE ENGINE + STILL"
echo "  → Edit the following files:"
echo "    Engine: genart/src/main/kotlin/fr/geoking/arthur/genart/engines/${ENGINE}Engine.kt"
echo "    Still:  genart/src/main/kotlin/fr/geoking/arthur/genart/stills/${ENGINE}Still.kt"
echo ""
echo "  Apply improvements from the genart-upgrade skill checklist."
echo "  When done, run this script with --after to continue."
echo ""

if [[ "${2:-}" == "--after" ]]; then
    # Step 3: Bake AFTER
    echo "▸ Step 3: Baking AFTER artifacts..."
    SKIP_GENART_STILLS=1 "$GENART_OLLAMA_DIR/bake-previews.sh" after "$ENGINE" || true

    # Step 4: Side-by-side comparison
    echo ""
    echo "▸ Step 4: Generating comparison video..."
    "$GENART_OLLAMA_DIR/compare-previews.sh" "$ENGINE" || true

    # Step 5: Regenerate preview icon
    echo ""
    echo "▸ Step 5: Regenerating preview icon (1080px)..."
    (cd "$ARTHUR_ROOT" && ./gradlew :androidApp:generateGenartScreenshots \
        -PgenartScreenshotSize=1080 --quiet) || true

    echo ""
    echo "════════════════════════════════════════"
    echo "  UPGRADE COMPLETE: $ENGINE"
    echo "════════════════════════════════════════"
    echo ""
    echo "Artifacts:"

    source "$GENART_OLLAMA_DIR/common.sh"
    ensure_workdir
    slug="$(engine_to_slug "$ENGINE")"
    echo "  Before still:  $PREVIEWS/before/stills/${slug}.png"
    echo "  After still:   $PREVIEWS/after/stills/${slug}.png"
    echo "  Before video:  $PREVIEWS/before/videos/${slug}.mp4"
    echo "  After video:   $PREVIEWS/after/videos/${slug}.mp4"
    echo "  Comparison:    $PREVIEWS/compare/${slug}.mp4"
fi

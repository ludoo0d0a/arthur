#!/usr/bin/env bash
set -euo pipefail
# genart-upgrade skill — validate that an upgrade didn't break anything.
# Usage: validate-upgrade.sh [<EngineName>]
#
# Checks:
#   1. Unit tests pass (catalog coverage, stable ids, stable titles)
#   2. Engine + Still files compile
#   3. Before/after artifacts exist
#   4. No signature changes in Engine/Still

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
ARTHUR_ROOT="$(cd "$SCRIPT_DIR/../../../.." && pwd)"
GENART_SRC="$ARTHUR_ROOT/genart/src/main/kotlin/fr/geoking/arthur/genart"
ENGINE="${1:-}"

ERRORS=0

echo "════════════════════════════════════════"
echo "  GENART UPGRADE VALIDATION"
echo "════════════════════════════════════════"

# Check 1: Unit tests
echo ""
echo "▸ Check 1: Unit tests..."
if (cd "$ARTHUR_ROOT" && ./gradlew :genart:testDebugUnitTest --quiet 2>&1); then
    echo "  ✓ Unit tests pass"
else
    echo "  ✗ Unit tests FAILED"
    ERRORS=$((ERRORS + 1))
fi

# Check 2: Compile
echo ""
echo "▸ Check 2: Compilation..."
if (cd "$ARTHUR_ROOT" && ./gradlew :genart:compileDebugKotlin --quiet 2>&1); then
    echo "  ✓ Compilation succeeds"
else
    echo "  ✗ Compilation FAILED"
    ERRORS=$((ERRORS + 1))
fi

# Check 3: File existence (if engine specified)
if [[ -n "$ENGINE" ]]; then
    echo ""
    echo "▸ Check 3: File existence for $ENGINE..."
    eng_file="$GENART_SRC/engines/${ENGINE}Engine.kt"
    still_file="$GENART_SRC/stills/${ENGINE}Still.kt"
    for f in "$eng_file" "$still_file"; do
        if [[ -f "$f" ]]; then
            echo "  ✓ $(basename "$f")"
        else
            echo "  ✗ $(basename "$f") MISSING"
            ERRORS=$((ERRORS + 1))
        fi
    done

    # Check 4: Signature preserved
    echo ""
    echo "▸ Check 4: Signature check for $ENGINE..."

    # Engine must have the standard @Composable function signature
    if grep -q "fun ${ENGINE}Engine(" "$eng_file" 2>/dev/null; then
        echo "  ✓ Engine function signature present"
    else
        echo "  ✗ Engine function signature MISSING"
        ERRORS=$((ERRORS + 1))
    fi

    # Still must have the standard draw function
    if grep -q "fun draw(" "$still_file" 2>/dev/null; then
        echo "  ✓ Still draw function present"
    else
        echo "  ✗ Still draw function MISSING"
        ERRORS=$((ERRORS + 1))
    fi

    # Check required parameters
    for param in "isActive" "paletteColors" "quality" "brightness" "speed"; do
        if grep -q "$param" "$eng_file" 2>/dev/null; then
            echo "  ✓ Engine uses $param"
        else
            echo "  ⚠ Engine may not use $param"
        fi
    done

    # Check before/after artifacts
    echo ""
    echo "▸ Check 5: Artifacts..."
    source "$ARTHUR_ROOT/scripts/genart-ollama/common.sh" 2>/dev/null || true
    slug="$(echo "$ENGINE" | sed -E 's/^genart\.//; s/Engine$//; s/Still$//' | tr '[:upper:]' '[:lower:]')"
    WORK_ROOT="${ARTHUR_GENART_OLLAMA_HOME:-$HOME/arthur-genart-ollama}"
    PREVIEWS="$WORK_ROOT/previews"
    for stage in before after; do
        for type in stills videos; do
            ext="png"
            [[ "$type" == "videos" ]] && ext="mp4"
            f="$PREVIEWS/$stage/$type/${slug}.${ext}"
            if [[ -f "$f" ]]; then
                echo "  ✓ $stage/$type/${slug}.${ext}"
            else
                echo "  ○ $stage/$type/${slug}.${ext} (not yet baked)"
            fi
        done
    done
fi

echo ""
echo "════════════════════════════════════════"
if [[ "$ERRORS" -eq 0 ]]; then
    echo "  ✓ ALL CHECKS PASSED"
else
    echo "  ✗ $ERRORS CHECK(S) FAILED"
fi
echo "════════════════════════════════════════"
exit "$ERRORS"

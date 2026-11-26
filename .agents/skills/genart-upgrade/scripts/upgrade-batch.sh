#!/usr/bin/env bash
set -euo pipefail
# genart-upgrade skill — batch upgrade pipeline.
# Usage: upgrade-batch.sh [--category <animals|nature|abstract|scene|all>] [--dry-run]
#
# Lists engines by category for systematic upgrade.

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
ARTHUR_ROOT="$(cd "$SCRIPT_DIR/../../../.." && pwd)"
GENART_SRC="$ARTHUR_ROOT/genart/src/main/kotlin/fr/geoking/arthur/genart"

CATEGORY="${1:-all}"
DRY_RUN=0

while [[ $# -gt 0 ]]; do
    case "$1" in
        --category) CATEGORY="$2"; shift 2 ;;
        --dry-run) DRY_RUN=1; shift ;;
        *) shift ;;
    esac
done

# Categorize engines
animals=(CatWindow CatSleep CatTail DogNap DogDistantWalk DogStretch PandaSit PandaBamboo PandaRoll BirdFlock FishSchool Aquarium SleepingPet AntTrails)
nature=(Aurora AuroraWash Clouds Rain Fog Snow Nebula Dunes Storm Mountains GerstnerOcean CherryBlossoms FallingLeaves Grass PondRipples Sunbeams WaterfallMist CanyonDunes CoastCliffHaze TideFoam ObsidianShore HarborMist SoftTideRipples)
abstract=(VoronoiWash SilkFolds GradientMesh ArcMosaic ChromaticBlobs MorphingBlobs SoftNoiseField Constellation StarField Matrix SpiralMandala FlowRibbons CliffordWash PlasmaNova EnergyTendrils InterferenceWash)
scene=(AutoDashSoft AutoRainGlass AutoDashboardGlow AutoTunnelLight AutoHorizonDrive AutoDistantHeadlights MotoHeadlight MotoWindStreaks MotoRoadBlur MotoNightLean MotoTailGlow MotoDawnRide AvionHorizonClouds AvionContrail AvionWingGlow AvionCabinWindow AvionSunriseBand AvionNightCity TrainWindowBlur TrainCatenary TrainPlatformGlow TrainCountryside TrainNightCarriage TrainTunnelPulse)

case "$CATEGORY" in
    animals) engines=("${animals[@]}") ;;
    nature)  engines=("${nature[@]}") ;;
    abstract) engines=("${abstract[@]}") ;;
    scene)   engines=("${scene[@]}") ;;
    all)     engines=("${animals[@]}" "${nature[@]}" "${abstract[@]}" "${scene[@]}") ;;
    *) echo "Unknown category: $CATEGORY"; exit 1 ;;
esac

echo "Genart Upgrade Batch — Category: $CATEGORY (${#engines[@]} engines)"
echo "════════════════════════════════════════════════════════════════"

for eng in "${engines[@]}"; do
    eng_file="$GENART_SRC/engines/${eng}Engine.kt"
    still_file="$GENART_SRC/stills/${eng}Still.kt"
    if [[ -f "$eng_file" && -f "$still_file" ]]; then
        echo "  ✓ $eng"
        if [[ "$DRY_RUN" -eq 0 ]]; then
            echo "    Engine: $eng_file"
            echo "    Still:  $still_file"
        fi
    else
        echo "  ✗ $eng (missing files)"
    fi
done

echo ""
echo "Total: ${#engines[@]} engines to upgrade"
echo ""
echo "Run upgrade for each:"
echo "  .agents/skills/genart-upgrade/scripts/upgrade-one.sh <EngineName>"

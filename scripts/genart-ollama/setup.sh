#!/usr/bin/env bash
set -euo pipefail
# shellcheck source=common.sh
source "$(cd "$(dirname "$0")" && pwd)/common.sh"

require_cmd ollama
require_cmd ffmpeg

ensure_workdir

echo "Pulling Ollama models (M5 24GB set)..."
ollama pull gpt-oss:20b
ollama pull qwen2.5-coder:14b
ollama pull deepseek-coder-v2:16b
ollama pull qwen3-coder:30b

MF="$SCRIPT_DIR/modelfiles"
echo "Creating arthur-* models..."
ollama create arthur-audit -f "$MF/Modelfile.audit"
ollama create arthur-fix -f "$MF/Modelfile.fix"
ollama create arthur-fast -f "$MF/Modelfile.fast"
ollama create arthur-heavy -f "$MF/Modelfile.heavy"

"$SCRIPT_DIR/build-queue.sh" --resume || true

echo "Ready. Workdir: $WORK_ROOT"
echo "Docs: $ARTHUR_ROOT/docs/genart-ollama.md"
echo "Packs:"
"$SCRIPT_DIR/list-packs.sh" || true
ollama list | grep -E 'arthur-|NAME' || true

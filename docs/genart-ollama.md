# Genart Ollama kit (Mac M5 24 Go)

Local loop to **audit and improve** Arthur Ambient genarts (Compose Canvas + still bakers), with **PNG + MP4** before/after previews.

## Goals

- Richer, softer motion and look on **phone** and **TV** (car-safe: no flash).
- Still bakers stay coherent for **Auto album art** / Control Plane grid.
- Stay on Tier A/B ([roadmap-genart.md](roadmap-genart.md)); no WebGL; AGSL only with Canvas fallback.

## Prerequisites

```bash
brew install ollama ffmpeg
ollama serve   # leave running
```

From repo root:

```bash
./scripts/genart-ollama/setup.sh
```

Creates Ollama models `arthur-audit`, `arthur-fix`, `arthur-fast`, `arthur-heavy` and `~/arthur-genart-ollama/` workdirs.

## Models (24 Go unified memory)

| Model | Ollama tag | Role |
|-------|------------|------|
| arthur-audit | gpt-oss:20b | Visual / Ambient checklist |
| arthur-fix | qwen2.5-coder:14b | Kotlin Engine+Still patches |
| arthur-fast | deepseek-coder-v2:16b | Micro (seam, qualityCount, Paint) |
| arthur-heavy | qwen3-coder:30b | Rare redesign (close other apps) |

Do **not** pull `qwen3.6:35b-a3b` on 24 Go (too tight).

## Preview bake (PNG + MP4)

Stills (existing):

```bash
./gradlew :androidApp:generateGenartScreenshots
```

Animated frames (phase sweep → PNG sequence under output dir):

```bash
./gradlew :androidApp:generateGenartPreviewFrames \
  -PgenartVideoSize=720 \
  -PgenartVideoFps=12 \
  -PgenartVideoSeconds=4 \
  -PgenartVideoEngines=aurora,clouds,fog
```

Encode + stage before/after:

```bash
./scripts/genart-ollama/bake-previews.sh before Aurora Clouds Fog
./scripts/genart-ollama/bake-previews.sh after Aurora Clouds Fog
# Skip full still catalog bake when PNGs already exist:
# SKIP_GENART_STILLS=1 ./scripts/genart-ollama/bake-previews.sh after Aurora
./scripts/genart-ollama/compare-previews.sh Aurora Clouds Fog
```

Outputs live under `~/arthur-genart-ollama/previews/{before,after}/{stills,videos}/` and `…/compare/`.

Video defaults: **720px**, **12 fps**, **4 s** (48 frames). Override via Gradle props or env in scripts.

## Packs: list, resume, continuous

Pack membership and order come from `GenartPackTopics.categoryTopicSuffixes` (auto-resolved).

```bash
# Progress table (done / total / remaining)
./scripts/genart-ollama/list-packs.sh
# same as:
./scripts/genart-ollama/run-batch.sh --list
```

State under `~/arthur-genart-ollama/`: `current_pack.txt`, `packs_done.txt`, `queue.txt`, `done.txt`, `skip.txt`.

```bash
# Rebuild queue = remaining engines of the next incomplete pack (default)
./scripts/genart-ollama/build-queue.sh --resume

# Or force one pack
./scripts/genart-ollama/build-queue.sh --pack weather
```

### Continuous optimization (default = apply LLM patches)

Local Ollama **writes improved Engine+Still Kotlin into the repo** for every pack:

```bash
# Prerequisites once
./scripts/genart-ollama/setup.sh
ollama serve   # keep running

# Optimize all packs (resume → apply → next pack → …)
./scripts/genart-ollama/run-batch.sh

# One engine
./scripts/genart-ollama/run-batch.sh --once

# Finish current pack only
./scripts/genart-ollama/run-batch.sh --pack-once

# At most N engines
./scripts/genart-ollama/run-batch.sh -n 5

# Force pack
./scripts/genart-ollama/run-batch.sh --pack weather

# Audit only (do not touch repo Kotlin)
./scripts/genart-ollama/run-batch.sh --no-apply --once
```

## One-engine loop

```bash
# Default: apply LLM patches into genart/engines + stills
./scripts/genart-ollama/run-one.sh Storm

# Suggestions only
./scripts/genart-ollama/run-one.sh --no-apply Storm
```

Flow: bake before → Ollama audit → fix/micro (or heavy) → **apply into repo** → bake after → compare MP4.

## Cadence

- ~8–12 engines/day on M5 24 Go (audit + fix).
- 2–3 redesigns/week with `arthur-heavy` only.
- Cloud Claude Opus remains best for flagship “wow” creation; local models iterate patches.

## Validation after patches

```bash
./gradlew :genart:test --tests fr.geoking.arthur.genart.GenartCatalogTest
./gradlew :genart:compileDebugKotlin
```

Manual: phone preview Low/Med, TV Dream if available, still bake readability.

## Workdir layout

```
~/arthur-genart-ollama/
  current_pack.txt packs_done.txt
  queue.txt done.txt skip.txt failed.txt
  work/<Engine>/{audit,fix-v1,fix-v2,redesign}.md
  work/<Engine>/out/{XxxEngine.kt,XxxStill.kt}
  previews/before|after/{stills,videos,frames}/
  previews/compare/
```

If extract/apply fails, the engine goes to `failed.txt` and is **not** marked done (batch can retry).

---
name: arthur-commit-build
description: >-
  Verify Arthur builds successfully before every git commit. Use when committing,
  preparing a commit, fixing CI compile failures, or when the user asks to build
  properly on each commit.
---

# Arthur: build before every commit

**Hard rule:** do not create a git commit in this repo until a local Gradle build matching CI has succeeded for the current working tree.

## Required gate (before `git commit`)

1. Use **JDK 21** (`jdk21` skill / `mise` / `JAVA_HOME`). Prefer `./gradlew`, never system `gradle`.
2. Run:

```bash
./gradlew :androidApp:assembleFullDebug --quiet
```

3. Only if that succeeds, proceed with the commit.
4. If it fails, fix compile/test errors, re-run the gate, then commit. Never commit known-broken Kotlin/Android code.

## Faster loop while fixing

When iterating on `genart` / Kotlin compile errors only:

```bash
./gradlew :genart:compileDebugKotlin --quiet
```

Re-run the full gate (`:androidApp:assembleFullDebug`) once before the commit.

## CI alignment

Primary PR/CI assemble task: `:androidApp:assembleFullDebug` (see `.github/workflows/android-ci.yml`).

Related unit-test modules on release workflows include `:genart:testDebugUnitTest` and `:fractal:testDebugUnitTest` — run those when the change touches those modules or when CI reports test failures.

## Common genart pitfalls

- `Brush.verticalGradient` / `horizontalGradient`: pass `colors = listOf(...)` (or color-stop pairs `0f to color`), not bare `Color` varargs with `startY`/`endY`.
- Compose `Stroke`: use `width = ...`, not `strokeWidth = ...`.
- Android `LinearGradient(..., intArrayOf(...), …)` needs the `FloatArray?` positions argument (or `null`) before `TileMode`.
- Package-private `data class` / `const val` names in `genart/.../engines/` must be unique across the package (rename per-file helpers; do not clash with `HeavySimAgsl.kt`).

## Do not skip

- Do not commit with `--no-verify` to bypass this gate unless the user explicitly requests it.
- Do not assume “only docs/skills changed” skips the gate when Kotlin/Android sources in the same commit are also dirty — if any app/library source is staged or will be included, run the gate.

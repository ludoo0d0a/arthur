#!/usr/bin/env python3
"""Extract Engine/Still Kotlin fences from LLM markdown into out/."""
from __future__ import annotations

import re
import sys
from pathlib import Path


def extract(md_text: str, engine: str) -> tuple[str | None, str | None]:
    blocks = re.findall(r"```(?:kotlin|kt)?\n(.*?)```", md_text, flags=re.S | re.I)
    eng_file = still_file = None
    for b in blocks:
        if (
            f"{engine}Engine" in b
            or f"internal fun {engine}Engine" in b
            or f"fun {engine}Engine" in b
        ):
            eng_file = b.strip() + "\n"
        if f"object {engine}Still" in b or f"{engine}Still" in b:
            still_file = b.strip() + "\n"
    if eng_file is None and blocks:
        scored = sorted(blocks, key=len, reverse=True)
        eng_file = scored[0].strip() + "\n"
        if still_file is None and len(scored) > 1:
            still_file = scored[1].strip() + "\n"
    return eng_file, still_file


def main() -> int:
    if len(sys.argv) != 3:
        print("Usage: extract_kotlin.py <work_dir> <Engine>", file=sys.stderr)
        return 2
    work = Path(sys.argv[1])
    engine = sys.argv[2]
    out = work / "out"
    out.mkdir(parents=True, exist_ok=True)

    eng_file = still_file = None
    for name in ("redesign.md", "fix-v1.md", "fix-v2.md"):
        p = work / name
        if not p.is_file():
            continue
        e, s = extract(p.read_text(encoding="utf-8", errors="replace"), engine)
        if eng_file is None and e:
            eng_file = e
        if still_file is None and s:
            still_file = s

    if eng_file:
        (out / f"{engine}Engine.kt").write_text(eng_file, encoding="utf-8")
    if still_file:
        (out / f"{engine}Still.kt").write_text(still_file, encoding="utf-8")
    print(f"extracted engine={bool(eng_file)} still={bool(still_file)} -> {out}")
    return 0 if eng_file and still_file else 1


if __name__ == "__main__":
    raise SystemExit(main())

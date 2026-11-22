#!/usr/bin/env python3
"""Fill genart-ollama prompt templates from files (no huge env vars)."""
from __future__ import annotations

import sys
from pathlib import Path


def main() -> int:
    if len(sys.argv) != 8:
        print(
            "Usage: fill_prompt.py template engine still_png video_mp4 "
            "engine_kt still_kt audit_md_or_empty",
            file=sys.stderr,
        )
        return 2
    template, engine, still_png, video_mp4, eng_path, still_path, audit_path = sys.argv[1:]
    t = Path(template).read_text(encoding="utf-8")
    audit = ""
    if audit_path:
        p = Path(audit_path)
        if p.is_file():
            audit = p.read_text(encoding="utf-8", errors="replace")
    repl = {
        "{{ENGINE}}": engine,
        "{{STILL_PNG}}": still_png,
        "{{VIDEO_MP4}}": video_mp4,
        "{{ENGINE_SRC}}": Path(eng_path).read_text(encoding="utf-8"),
        "{{STILL_SRC}}": Path(still_path).read_text(encoding="utf-8"),
        "{{AUDIT}}": audit,
    }
    for k, v in repl.items():
        t = t.replace(k, v)
    sys.stdout.write(t)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

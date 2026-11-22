#!/usr/bin/env python3
"""Resolve Genart pack → Engine PascalCase names from Kotlin sources."""
from __future__ import annotations

import argparse
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]
TOPICS = ROOT / "shared/src/commonMain/kotlin/fr/geoking/arthur/shared/marketplace/GenartPackTopics.kt"
SOURCE = ROOT / "shared/src/commonMain/kotlin/fr/geoking/arthur/shared/source/GenartSource.kt"
REGISTRY = ROOT / "genart/src/main/kotlin/fr/geoking/arthur/genart/GenartRegistry.kt"
STILLS = ROOT / "genart/src/main/kotlin/fr/geoking/arthur/genart/stills"


def parse_pack_order(text: str) -> list[str]:
    m = re.search(
        r"val categoryTopicSuffixes:\s*List<String>\s*=\s*listOf\((.*?)\)",
        text,
        re.S,
    )
    if not m:
        raise SystemExit("categoryTopicSuffixes not found")
    # Tokens are CONST names like NATURE, WEATHER — resolve to string values
    const_vals = dict(re.findall(r'const val (\w+)\s*=\s*"([^"]+)"', text))
    order: list[str] = []
    for tok in re.findall(r"\b([A-Z][A-Z0-9_]*)\b", m.group(1)):
        if tok in const_vals:
            order.append(const_vals[tok])
    return order


def parse_pack_source_consts(text: str) -> dict[str, list[str]]:
    """pack_slug → [GenartSource.CONST, ...]"""
    const_vals = dict(re.findall(r'const val (\w+)\s*=\s*"([^"]+)"', text))
    packs: dict[str, list[str]] = {}
    for m in re.finditer(
        r"val ([A-Z0-9_]+)_IDS:\s*Set<String>\s*=\s*setOf\((.*?)\)",
        text,
        re.S,
    ):
        pack_const = m.group(1)  # WEATHER
        pack_slug = const_vals.get(pack_const)
        if not pack_slug:
            # US_VINTAGE_IDS → us_vintage via const US_VINTAGE
            pack_slug = const_vals.get(pack_const) or pack_const.lower()
        refs = re.findall(r"GenartSource\.([A-Z0-9_]+)", m.group(2))
        packs[pack_slug] = refs
    return packs


def parse_source_ids(text: str) -> dict[str, str]:
    """CONST → genart.xxx"""
    return dict(re.findall(r'const val ([A-Z0-9_]+)\s*=\s*"(genart\.[^"]+)"', text))


def parse_registry(text: str) -> dict[str, str]:
    """stableId genart.xxx → Engine PascalCase (GenartEngineId.Foo → Foo)"""
    out: dict[str, str] = {}
    for m in re.finditer(
        r"id\s*=\s*GenartEngineId\.(\w+)\s*,\s*\n\s*stableId\s*=\s*\"(genart\.[^\"]+)\"",
        text,
    ):
        out[m.group(2)] = m.group(1)
    return out


def existing_engines() -> set[str]:
    names: set[str] = set()
    for p in STILLS.glob("*Still.kt"):
        names.add(p.name[: -len("Still.kt")])
    return names


def pack_engines(
    pack: str,
    pack_consts: dict[str, list[str]],
    source_ids: dict[str, str],
    registry: dict[str, str],
    existing: set[str],
) -> tuple[list[str], list[str]]:
    """Returns (engines_present, missing_stable_ids)."""
    consts = pack_consts.get(pack, [])
    present: list[str] = []
    missing: list[str] = []
    seen: set[str] = set()
    for c in consts:
        sid = source_ids.get(c)
        if not sid:
            missing.append(f"GenartSource.{c}")
            continue
        eng = registry.get(sid)
        if not eng:
            missing.append(sid)
            continue
        if eng not in existing:
            missing.append(f"{sid}→{eng}(no Still)")
            continue
        if eng not in seen:
            present.append(eng)
            seen.add(eng)
    return present, missing


def load() -> tuple[list[str], dict[str, list[str]], dict[str, str], dict[str, str], set[str]]:
    topics = TOPICS.read_text(encoding="utf-8")
    order = parse_pack_order(topics)
    pack_consts = parse_pack_source_consts(topics)
    # Fix pack_slug for *_IDS when const name differs — keys from parse may be wrong
    # Re-key using const_vals: WEATHER_IDS → weather via WEATHER const
    const_vals = dict(re.findall(r'const val (\w+)\s*=\s*"([^"]+)"', topics))
    fixed: dict[str, list[str]] = {}
    for m in re.finditer(
        r"val ([A-Z0-9_]+)_IDS:\s*Set<String>\s*=\s*setOf\((.*?)\)",
        topics,
        re.S,
    ):
        key = m.group(1)
        slug = const_vals.get(key, key.lower())
        refs = re.findall(r"GenartSource\.([A-Z0-9_]+)", m.group(2))
        fixed[slug] = refs
    source_ids = parse_source_ids(SOURCE.read_text(encoding="utf-8"))
    registry = parse_registry(REGISTRY.read_text(encoding="utf-8"))
    existing = existing_engines()
    return order, fixed, source_ids, registry, existing


def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument("cmd", choices=["order", "engines", "list", "next", "status"])
    ap.add_argument("--pack")
    ap.add_argument("--work", type=Path, default=Path.home() / "arthur-genart-ollama")
    args = ap.parse_args()

    order, packs, source_ids, registry, existing = load()
    work: Path = args.work
    done_engines = set()
    skip_engines = set()
    packs_done = set()
    current = ""
    if work.is_dir():
        done_f = work / "done.txt"
        skip_f = work / "skip.txt"
        packs_done_f = work / "packs_done.txt"
        current_f = work / "current_pack.txt"
        if done_f.exists():
            done_engines = {l.strip() for l in done_f.read_text().splitlines() if l.strip()}
        if skip_f.exists():
            skip_engines = {l.strip() for l in skip_f.read_text().splitlines() if l.strip()}
        if packs_done_f.exists():
            packs_done = {l.strip() for l in packs_done_f.read_text().splitlines() if l.strip()}
        if current_f.exists():
            current = current_f.read_text().strip()

    finished = done_engines | skip_engines

    if args.cmd == "order":
        for p in order:
            print(p)
        return

    if args.cmd == "engines":
        pack = args.pack
        if not pack:
            raise SystemExit("--pack required")
        present, _missing = pack_engines(pack, packs, source_ids, registry, existing)
        for e in present:
            print(e)
        return

    def pack_stats(p: str) -> tuple[int, int, str]:
        present, _ = pack_engines(p, packs, source_ids, registry, existing)
        total = len(present)
        done_n = sum(1 for e in present if e in finished)
        if p in packs_done or (total > 0 and done_n >= total):
            status = "done"
        elif p == current or (done_n > 0 and done_n < total):
            status = "in_progress"
        else:
            status = "pending"
        return done_n, total, status

    if args.cmd == "list" or args.cmd == "status":
        print(f"{'PACK':<16} {'STATUS':<12} {'DONE':>4}/{'TOTAL':<4}  REMAINING")
        for p in order:
            done_n, total, status = pack_stats(p)
            rem = total - done_n
            mark = " ←" if p == current else ""
            print(f"{p:<16} {status:<12} {done_n:4d}/{total:<4d}  {rem}{mark}")
        if current:
            print(f"current_pack={current}")
        return

    if args.cmd == "next":
        # 1) current_pack with remaining  2) any in_progress  3) first pending in order
        def remaining_for(p: str) -> list[str]:
            present, _ = pack_engines(p, packs, source_ids, registry, existing)
            return [e for e in present if e not in finished]

        picked = ""
        rem: list[str] = []
        if current and current not in packs_done:
            rem = remaining_for(current)
            if rem:
                picked = current
        if not picked:
            for p in order:
                if p in packs_done:
                    continue
                present, _ = pack_engines(p, packs, source_ids, registry, existing)
                done_n = sum(1 for e in present if e in finished)
                if present and 0 < done_n < len(present):
                    rem = remaining_for(p)
                    if rem:
                        picked = p
                        break
        if not picked:
            for p in order:
                if p in packs_done:
                    continue
                rem = remaining_for(p)
                if rem:
                    picked = p
                    break
        if not picked:
            print("")
            return
        print(picked)
        for e in rem:
            print(f"engine:{e}")
        return


if __name__ == "__main__":
    main()

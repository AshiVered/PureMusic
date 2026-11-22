#!/usr/bin/env python3
"""Build release notes for an automated PureMusic release.

Usage: changelog.py <old-upstream-ref> <new-upstream-ref> <version> [--pure-from <ref>]

Groups Metrolist's commits (conventional commit subjects) into sections and adds PureMusic's own
commits since the previous release, if any.
"""
import argparse
import re
import subprocess

UPSTREAM_REPO = "https://github.com/MetrolistGroup/Metrolist"

SECTIONS = [
    ("New features", ("feat",)),
    ("Fixes", ("fix",)),
    ("Performance", ("perf",)),
    ("Translations", ("i18n", "translation", "l10n")),
    ("Dependencies", ("deps",)),
]


def git(*args: str) -> str:
    return subprocess.run(["git", *args], check=True, capture_output=True, text=True, encoding="utf-8").stdout


def commits(*revs: str) -> list[tuple[str, str]]:
    out = git("log", "--no-merges", "--format=%h%x09%s", *revs, "--")
    return [tuple(line.split("\t", 1)) for line in out.splitlines() if "\t" in line]


def section_of(subject: str) -> str:
    m = re.match(r"^(\w+)(?:\(([^)]*)\))?!?:", subject)
    kind, scope = (m.group(1).lower(), (m.group(2) or "").lower()) if m else ("", "")
    if "weblate" in subject.lower() or "translat" in subject.lower():
        return "Translations"
    for title, keys in SECTIONS:
        if kind in keys or scope in keys:
            return title
    return "Other changes"


def clean(subject: str) -> str:
    subject = re.sub(r"^\w+(\([^)]*\))?!?:\s*", "", subject)
    subject = re.sub(r"\s*\(#(\d+)\)$", rf" ([#\1]({UPSTREAM_REPO}/pull/\1))", subject)
    return subject[:1].upper() + subject[1:]


def main() -> None:
    p = argparse.ArgumentParser()
    p.add_argument("old")
    p.add_argument("new")
    p.add_argument("version")
    p.add_argument("--pure-from", help="previous PureMusic release ref, to list PureMusic's own commits")
    a = p.parse_args()

    # PureMusic is Android only: skip commits scoped to the desktop or iOS apps.
    upstream = [
        (sha, s) for sha, s in commits(f"{a.old}..{a.new}") if not re.match(r"^\w+\((desktop|ios|macos|windows|linux)\)", s, re.I)
    ]
    grouped: dict[str, list[str]] = {}
    for sha, subject in upstream:
        grouped.setdefault(section_of(subject), []).append(f"- {clean(subject)} (`{sha}`)")

    lines = [f"PureMusic {a.version}: synced with Metrolist `{git('rev-parse', '--short', a.new).strip()}`.", ""]

    if a.pure_from:
        own = [
            (sha, s)
            for sha, s in commits(f"{a.pure_from}..HEAD", f"^{a.new}")
            if not s.startswith("Merge Metrolist") and not s.startswith("Release PureMusic")
        ]
        if own:
            lines += ["## PureMusic changes", *[f"- {s} (`{sha}`)" for sha, s in own], ""]

    if upstream:
        lines += [f"## From Metrolist ({len(upstream)} commits)", ""]
        for title in [t for t, _ in SECTIONS] + ["Other changes"]:
            if title in grouped:
                lines += [f"### {title}", *grouped[title], ""]
        lines.append(f"**Full Metrolist changes:** {UPSTREAM_REPO}/compare/{git('rev-parse', a.old).strip()}...{git('rev-parse', a.new).strip()}")
    else:
        lines.append("No Metrolist changes in this release.")

    lines += [
        "",
        "> Built automatically. All PureMusic restrictions were verified to still be hooked in "
        "(`scripts/puremusic/check_hooks.py`) before building. Test before publishing this draft.",
    ]
    print("\n".join(lines))


if __name__ == "__main__":
    main()

#!/usr/bin/env python3
"""Extract bundle/ALL_CODE.md and prove the archive round-trips.

Writes every archived file into a temporary directory, compares those bytes
with the repository (the parent of bundle/), then runs ./mvnw -B verify there.

Exit 0: byte compare matched and Maven verify succeeded.
Exit 1: the archive could not be parsed or bytes differed.
Exit 2: Maven verify failed.
"""

from __future__ import annotations

import re
import stat
import subprocess
import sys
import tempfile
from pathlib import Path

START = re.compile(br"(?m)^===== FILE: ([^\r\n]+) =====\n")
END_MARK = b"===== END FILE ====="


def extract(archive: bytes) -> list[tuple[str, bytes]]:
    matches = list(START.finditer(archive))
    if not matches:
        raise SystemExit("no FILE sections in bundle/ALL_CODE.md")
    files: list[tuple[str, bytes]] = []
    seen: set[str] = set()
    for match in matches:
        rel = match.group(1).decode("utf-8")
        if rel in seen:
            raise SystemExit(f"duplicate path in archive: {rel}")
        if rel.startswith("/") or rel.startswith("\\") or ".." in Path(rel).parts:
            raise SystemExit(f"refusing path: {rel}")
        start = match.end()
        end = archive.find(END_MARK, start)
        if end < 0:
            raise SystemExit(f"missing END FILE for {rel}")
        content = archive[start:end]
        if not content.endswith(b"\n"):
            raise SystemExit(f"{rel} is not terminated by a newline before END FILE")
        files.append((rel, content))
        seen.add(rel)
    return files


def surefire_totals(build_dir: Path) -> tuple[int, int, int, int]:
    run = failed = errors = skipped = 0
    reports = build_dir / "target" / "surefire-reports"
    if not reports.is_dir():
        return (0, 0, 0, 0)
    pattern = re.compile(
        r"Tests run: (\d+), Failures: (\d+), Errors: (\d+), Skipped: (\d+)"
    )
    for report in sorted(reports.glob("*.txt")):
        text = report.read_text(encoding="utf-8", errors="replace")
        found = pattern.findall(text)
        if not found:
            continue
        # Surefire writes the class summary once at the end of each report.
        tests, fails, errs, skips = (int(n) for n in found[-1])
        run += tests
        failed += fails
        errors += errs
        skipped += skips
    return (run, failed, errors, skipped)


def main() -> int:
    bundle = Path(__file__).resolve().parent
    repo = bundle.parent
    archive_path = bundle / "ALL_CODE.md"
    archive = archive_path.read_bytes()
    files = extract(archive)
    print(f"extracted_sections={len(files)}")

    dest = Path(tempfile.mkdtemp(prefix="mdp-bundle-"))
    print(f"temp_dir={dest}")
    mismatches: list[str] = []
    for rel, content in files:
        target = dest / rel
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_bytes(content)
        original = repo / rel
        if not original.is_file():
            mismatches.append(f"missing in repo: {rel}")
            continue
        if original.read_bytes() != content:
            mismatches.append(f"bytes differ: {rel}")
    if mismatches:
        print("BYTE_COMPARE_FAILED")
        for item in mismatches:
            print(item)
        return 1
    print(f"byte_compare=ok files={len(files)}")

    mvnw = dest / "mvnw"
    mvnw.chmod(mvnw.stat().st_mode | stat.S_IXUSR | stat.S_IXGRP | stat.S_IXOTH)
    print("running ./mvnw -B verify")
    completed = subprocess.run(
        ["./mvnw", "-B", "verify"],
        cwd=dest,
        check=False,
    )
    run, failed, errors, skipped = surefire_totals(dest)
    print(
        "surefire "
        f"tests_run={run} failures={failed} errors={errors} skipped={skipped}"
    )
    if completed.returncode != 0:
        print(f"MVN_VERIFY_FAILED exit={completed.returncode}")
        return 2
    print(
        "ROUND_TRIP_OK "
        f"files={len(files)} byte_compare=ok mvn_exit=0 "
        f"tests_run={run} failures={failed} errors={errors} skipped={skipped}"
    )
    return 0


if __name__ == "__main__":
    sys.exit(main())

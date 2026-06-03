"""Lightweight repository secret scan for submission checks.

The scanner intentionally avoids printing matched secret values. It reports only
file paths and line numbers so logs are safe to share.
"""
from __future__ import annotations

import re
import sys
import subprocess
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
SKIP_DIRS = {
    ".git", ".venv", "venv", "build", "dist", ".gradle", "__pycache__",
    ".idea", ".vs", ".claude", ".kotlin", ".pytest_cache", ".mypy_cache",
    "node_modules", "hf_home", "qdrant_storage", "uploads",
}
SKIP_NAMES = {".env"}
SKIP_SUFFIXES = {
    ".png", ".jpg", ".jpeg", ".webp", ".pdf", ".docx", ".pptx", ".xlsx",
    ".jar", ".keystore", ".apk", ".aab",
}
PATTERNS = [
    re.compile(r"ark-[A-Za-z0-9-]{20,}"),
    re.compile(r"sk-[A-Za-z0-9_-]{20,}"),
    re.compile(r"(?i)(api[_-]?key|secret|token)\s*=\s*['\"]?(?!<|your|xxx|xxxxxx)[A-Za-z0-9._/-]{16,}"),
]
PLACEHOLDER_MARKERS = ("your-key", "your_", "your-", "xxxx", "xxxxx", "<redacted>", "example")


def should_skip(path: Path) -> bool:
    rel_parts = set(path.relative_to(ROOT).parts)
    return path.name in SKIP_NAMES or bool(rel_parts & SKIP_DIRS) or path.suffix.lower() in SKIP_SUFFIXES


def scan_text_file(path: Path) -> list[str]:
    findings = []
    try:
        text = path.read_text(encoding="utf-8", errors="ignore")
    except OSError:
        return findings

    for line_no, line in enumerate(text.splitlines(), 1):
        if any(marker in line.lower() for marker in PLACEHOLDER_MARKERS):
            continue
        if any(pattern.search(line) for pattern in PATTERNS):
            findings.append(f"{path.relative_to(ROOT)}:{line_no}")
    return findings


def main() -> int:
    findings: list[str] = []
    try:
        result = subprocess.run(
            ["git", "ls-files", "-z", "--cached", "--others", "--exclude-standard"],
            cwd=ROOT,
            check=True,
            capture_output=True,
        )
    except (OSError, subprocess.CalledProcessError) as exc:
        print(f"Unable to list repository files: {exc}", file=sys.stderr)
        return 2

    for raw_path in result.stdout.split(b"\0"):
        if not raw_path:
            continue
        path = ROOT / raw_path.decode("utf-8", errors="ignore")
        if path.is_file() and not path.is_symlink() and not should_skip(path):
            findings.extend(scan_text_file(path))

    if findings:
        print("Potential secrets found:")
        for item in findings:
            print(f"  {item}")
        return 1

    print("No obvious secrets found in text files.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
